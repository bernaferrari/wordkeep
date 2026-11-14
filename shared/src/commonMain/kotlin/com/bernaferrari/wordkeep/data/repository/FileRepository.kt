package com.bernaferrari.wordkeep.data.repository

import com.bernaferrari.wordkeep.domain.model.isNoteFileName
import com.bernaferrari.wordkeep.domain.repository.FileInfo
import org.koin.core.annotation.Named
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.bernaferrari.wordkeep.domain.repository.IFileRepository
import com.bernaferrari.wordkeep.domain.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import com.bernaferrari.wordkeep.util.platformFileSystem
import org.koin.core.annotation.Single

@Single(binds = [IFileRepository::class])
class FileRepository(
    private val settingsRepository: ISettingsRepository,
    @Named("default_notebook_path") private val defaultNotebookPath: String,
) : IFileRepository {

    private val fileSystem = platformFileSystem

    private val trashDirectoryName = ".trash"

    override suspend fun getTrashPath(): Path =
        settingsRepository.getNotebookDirectory.first().ifEmpty { defaultNotebookPath }.toPath() / trashDirectoryName

    override suspend fun listFiles(directory: Path): List<FileInfo> =
        withContext(Dispatchers.Default) {
            try {
                if (!fileSystem.exists(directory)) {
                    return@withContext emptyList()
                }
                val metadata = fileSystem.metadata(directory)
                if (!metadata.isDirectory) {
                    return@withContext emptyList()
                }

                val showHidden = settingsRepository.isFileBrowserShowHiddenFiles.first()
                val sortOrder = settingsRepository.getFileBrowserSortOrder.first()
                val foldersFirst = settingsRepository.isFileBrowserFolderFirst.first()

                val files = fileSystem.list(directory)

                files
                    .filter { path ->
                        val name = path.name
                        showHidden || !name.startsWith(".")
                    }
                    .mapNotNull { path ->
                        getFileInfo(path)
                    }
                    .sortedWith(compareBy<FileInfo> { foldersFirst && !it.isDirectory }.then(sortOrder.comparator()))
            } catch (e: IOException) {
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun listFilesRecursively(directory: Path, includePreview: Boolean): List<FileInfo> =
        withContext(Dispatchers.Default) {
            fileSystem.listRecursively(directory)
                .filter { path ->
                    !path.relativeTo(directory).segments.any { it.startsWith(".") } &&
                        fileSystem.metadata(path).isRegularFile
                }
                .mapNotNull { getFileInfo(it, includePreview) }
                .toList()
        }

    override suspend fun searchFiles(
        directory: Path,
        query: String,
        recursive: Boolean
    ): List<FileInfo> = withContext(Dispatchers.Default) {
        try {
            val fileList = if (recursive) {
                fileSystem.listRecursively(directory).toList()
            } else {
                fileSystem.list(directory)
            }

            fileList.filter { path ->
                val metadata = fileSystem.metadata(path)
                metadata.isRegularFile && path.name.contains(query, ignoreCase = true)
            }.mapNotNull { getFileInfo(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFileInfo(path: Path): FileInfo? =
        withContext(Dispatchers.Default) { getFileInfo(path, includePreview = true) }

    private fun getFileInfo(path: Path, includePreview: Boolean): FileInfo? {
        return try {
            if (!fileSystem.exists(path)) return null
            val metadata = fileSystem.metadata(path)

            val name = path.name
            val extension = if (name.contains(".")) name.substringAfterLast(".") else ""

            val preview = if (includePreview && !metadata.isDirectory && isNoteFileName(name)) {
                try {
                    fileSystem.read(path) {
                        val limit = 1500L
                        val available = if (request(limit)) limit else buffer.size
                        readUtf8(available)
                    }
                } catch (e: Exception) {
                    null
                }
            } else null

            FileInfo(
                path = path,
                name = name,
                isDirectory = metadata.isDirectory,
                size = metadata.size ?: 0L,
                lastModified = metadata.lastModifiedAtMillis ?: 0L,
                extension = extension,
                preview = preview
            )
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun createFile(parent: Path, name: String): Path? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    if (!fileSystem.exists(parent)) {
                        fileSystem.createDirectories(parent)
                    }
                    val filePath = fileSystem.resolveUniquePath(parent, name)
                    fileSystem.write(filePath) {
                    }
                    filePath
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun createFileWithContent(parent: Path, name: String, content: String): Path? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    if (!fileSystem.exists(parent)) {
                        fileSystem.createDirectories(parent)
                    }
                    val filePath = fileSystem.resolveUniquePath(parent, name)
                    fileSystem.write(filePath) {
                        writeUtf8(content)
                    }
                    filePath
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun createDirectory(parent: Path, name: String): Path? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    val dirPath = fileSystem.resolveUniquePath(parent, name)
                    fileSystem.createDirectories(dirPath)
                    dirPath
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun deleteFile(path: Path): Boolean = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            try {
                val entry = trashEntry(getTrashPath(), path)
                if (entry != null) {
                    fileSystem.deleteRecursively(entry)
                    return@withLock true
                }
                val metadata = fileSystem.metadata(path)
                if (metadata.isDirectory) {
                    fileSystem.deleteRecursively(path)
                } else {
                    fileSystem.delete(path)
                }
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun moveToTrash(path: Path): Path? = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            val trash = getTrashPath()
            val entry = trash / Uuid.random().toString()
            val destination = entry / "item" / path.name
            try {
                if (path == trash || path.segments.any { it == trashDirectoryName }) return@withLock null
                fileSystem.createDirectories(destination.parent!!)
                fileSystem.write(entry / "original-path") {
                    writeUtf8(path.normalized().toString())
                }
                fileSystem.atomicMove(path, destination)
                destination
            } catch (_: IOException) {
                runCatching { fileSystem.deleteRecursively(entry) }
                null
            }
        }
    }

    override suspend fun restoreFromTrash(path: Path): Path? = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            val trash = getTrashPath()
            val original = originalTrashPath(trash, path) ?: return@withLock null
            val parent = original.parent ?: return@withLock null
            try {
                fileSystem.createDirectories(parent)
                val destination = fileSystem.resolveUniquePath(parent, original.name)
                fileSystem.atomicMove(path, destination)
                trashEntry(trash, path)?.let { runCatching { fileSystem.deleteRecursively(it) } }
                destination
            } catch (_: IOException) {
                null
            }
        }
    }

    private fun trashEntry(trash: Path, item: Path): Path? =
        item.parent?.takeIf { it.name == "item" }?.parent?.takeIf { it.parent == trash }

    private fun originalTrashPath(trash: Path, path: Path): Path? {
        val entry = trashEntry(trash, path)
        if (entry != null) {
            return try {
                fileSystem.read(entry / "original-path") { readUtf8().toPath(normalize = true) }
                    .takeIf { original ->
                        original.name == path.name &&
                            trashDirectoryName !in original.segments && ".." !in original.segments
                    }
            } catch (_: IOException) {
                null
            }
        }

        // Older trash entries store only the timestamp and filename.
        if (path.parent != trash) return null
        val name = legacyTrashName.matchEntire(path.name)?.groupValues?.get(1) ?: return null
        return trash.parent?.resolve(name)
    }

    private val legacyTrashName = Regex("[0-9]{8}_[0-9]{6}_(.+)")

    override suspend fun emptyTrash(): Boolean = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            try {
                fileSystem.deleteRecursively(getTrashPath())
                true
            } catch (_: IOException) {
                false
            }
        }
    }

    override suspend fun listTrash(): List<FileInfo> = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            val trash = getTrashPath()
            fileSystem.listOrNull(trash).orEmpty()
                .flatMap { entry ->
                    if (legacyTrashName.matches(entry.name)) listOf(entry)
                    else fileSystem.listOrNull(entry / "item").orEmpty()
                }
                .mapNotNull { path ->
                    getFileInfo(path, includePreview = true)?.let { file ->
                        val original = originalTrashPath(trash, path)
                        file.copy(name = original?.name ?: file.name)
                    }
                }
                .sortedByDescending { it.lastModified }
        }
    }

    override suspend fun readText(path: Path): String? = withContext(Dispatchers.Default) {
        try {
            fileSystem.read(path) { readUtf8() }
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun renameFile(path: Path, newName: String): Path? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    val parent = path.parent ?: ".".toPath()
                    val dest = fileSystem.resolveUniquePath(parent, newName, excludePath = path)
                    if (dest == path) return@withLock path
                    fileSystem.atomicMove(path, dest)
                    dest
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun copyFile(source: Path, destination: Path): Boolean =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    fileSystem.copy(source, destination)
                    true
                } catch (e: IOException) {
                    false
                }
            }
        }

    override suspend fun moveFile(source: Path, destination: Path): Boolean =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    fileSystem.atomicMove(source, destination)
                    true
                } catch (e: IOException) {
                    false
                }
            }
        }

    override suspend fun isDirectory(path: Path): Boolean = withContext(Dispatchers.Default) {
        try {
            fileSystem.metadata(path).isDirectory
        } catch (e: IOException) {
            false
        }
    }

    override suspend fun isFile(path: Path): Boolean = withContext(Dispatchers.Default) {
        try {
            fileSystem.metadata(path).isRegularFile
        } catch (e: IOException) {
            false
        }
    }

}
