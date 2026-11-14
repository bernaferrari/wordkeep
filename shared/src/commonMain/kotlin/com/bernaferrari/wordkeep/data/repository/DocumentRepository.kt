package com.bernaferrari.wordkeep.data.repository

import com.bernaferrari.wordkeep.domain.model.Document
import com.bernaferrari.wordkeep.domain.model.DocumentFormat
import com.bernaferrari.wordkeep.domain.repository.IDocumentRepository
import com.bernaferrari.wordkeep.domain.repository.INoteMetadataRepository
import com.bernaferrari.wordkeep.domain.repository.ISettingsRepository
import com.bernaferrari.wordkeep.util.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Named
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.withContext
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import com.bernaferrari.wordkeep.util.platformFileSystem
import org.koin.core.annotation.Single

@Single(binds = [IDocumentRepository::class])
class DocumentRepository(
    private val settingsRepository: ISettingsRepository,
    private val noteMetadataRepository: INoteMetadataRepository,
    @Named("default_notebook_path") private val defaultNotebookPath: String,
) : IDocumentRepository {

    private val fileSystem = platformFileSystem

    @OptIn(ExperimentalUuidApi::class)
    private fun writeAtomically(target: Path, content: String) {
        val temporary = (target.parent ?: ".".toPath()) / ".wordkeep-${Uuid.random()}.tmp"
        try {
            fileSystem.write(temporary, mustCreate = true) { writeUtf8(content) }
            fileSystem.atomicMove(temporary, target)
        } finally {
            if (fileSystem.exists(temporary)) fileSystem.delete(temporary)
        }
    }

    override suspend fun loadDocument(path: Path): Document? = withContext(Dispatchers.Default) {
        try {
            if (!fileSystem.exists(path)) return@withContext null
            val metadata = fileSystem.metadata(path)
            if (metadata.isDirectory) return@withContext null

            // Read content
            val content = fileSystem.read(path) {
                readUtf8()
            }

            val lastModified = metadata.lastModifiedAtMillis ?: 0L

            val todo = settingsRepository.getTodoFilePath.first()
                .ifEmpty { (defaultNotebookPath.toPath() / "todo.txt").toString() }
            val document = Document.fromPath(path).copy(content = content, lastModified = lastModified)
            if (todo.toPath() == path) document.copy(format = DocumentFormat.TODOTXT)
            else document
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun saveDocument(document: Document, content: String): Boolean =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    if (!fileSystem.exists(document.path)) return@withLock false
                    writeAtomically(document.path, content)
                    noteMetadataRepository.upsertFromContent(
                        path = document.path.toString(),
                        content = content,
                        nowMillis = fileSystem.metadata(document.path).lastModifiedAtMillis ?: nowMillis()
                    )
                    true
                } catch (e: IOException) {
                    false
                }
            }
        }

    override suspend fun createDocument(path: Path, content: String): Document? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    val parent = path.parent ?: ".".toPath()
                    fileSystem.createDirectories(parent)
                    val actualPath = fileSystem.resolveUniquePath(parent, path.name)
                    writeAtomically(actualPath, content)

                    val metadata = fileSystem.metadata(actualPath)
                    val document = Document.fromPath(actualPath)
                        .copy(content = content, lastModified = metadata.lastModifiedAtMillis ?: 0L)
                    noteMetadataRepository.upsertFromContent(
                        path = actualPath.toString(),
                        content = content,
                        nowMillis = document.lastModified
                    )
                    document
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun deleteDocument(path: Path): Boolean = withContext(Dispatchers.Default) {
        noteFileMutations.withLock {
            try {
                fileSystem.delete(path)
                noteMetadataRepository.deleteByPath(path.toString())
                true
            } catch (e: IOException) {
                false
            }
        }
    }

    override suspend fun renameDocument(document: Document, newName: String): Path? =
        withContext(Dispatchers.Default) {
            noteFileMutations.withLock {
                try {
                    val oldPath = document.path
                    val parent = oldPath.parent ?: ".".toPath()
                    val newPath = fileSystem.resolveUniquePath(parent, newName, excludePath = oldPath)
                    if (newPath == oldPath) return@withLock oldPath
                    val notebook = settingsRepository.getNotebookDirectory.first()
                        .ifBlank { defaultNotebookPath }.toPath()
                    val quickNote = settingsRepository.getQuickNoteFilePath.first()
                        .ifEmpty { (notebook / "quicknote.md").toString() }
                    val todo = settingsRepository.getTodoFilePath.first()
                        .ifEmpty { (notebook / "todo.txt").toString() }
                    fileSystem.atomicMove(oldPath, newPath)
                    if (quickNote.toPath() == oldPath) settingsRepository.setQuickNoteFilePath(newPath.toString())
                    if (todo.toPath() == oldPath) settingsRepository.setTodoFilePath(newPath.toString())
                    noteMetadataRepository.updatePath(
                        oldPath = oldPath.toString(),
                        newPath = newPath.toString(),
                        nowMillis = fileSystem.metadata(newPath).lastModifiedAtMillis ?: nowMillis()
                    )
                    newPath
                } catch (e: IOException) {
                    null
                }
            }
        }

    override suspend fun documentExists(path: Path): Boolean = withContext(Dispatchers.Default) {
        fileSystem.exists(path)
    }

    override suspend fun readContent(path: Path): String? = withContext(Dispatchers.Default) {
        try {
            fileSystem.read(path) {
                readUtf8()
            }
        } catch (e: IOException) {
            null
        }
    }

}
