package com.bernaferrari.wordkeep.domain.usecase

import com.bernaferrari.wordkeep.domain.model.isNoteFileName
import com.bernaferrari.wordkeep.domain.repository.IFileRepository
import com.bernaferrari.wordkeep.domain.repository.INoteMetadataRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Single
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.seconds

@Single
class IndexNoteMetadataUseCase(
    private val fileRepository: IFileRepository,
    private val metadataRepository: INoteMetadataRepository,
) {
    private val mutex = Mutex()
    private var lastScan: Pair<Path, TimeSource.Monotonic.ValueTimeMark>? = null

    suspend operator fun invoke(root: Path) = mutex.withLock {
        val previous = lastScan
        if (previous?.first == root && previous.second.elapsedNow() < 10.seconds) return@withLock
        val existing = metadataRepository.observeNotes().first().associateBy { it.path }
        val files = fileRepository.listFilesRecursively(root, includePreview = false)
            .filter { isNoteFileName(it.name) }
        val indexed = files.mapTo(hashSetOf()) { it.path.toString() }

        for (file in files) {
            val path = file.path.toString()
            if (existing[path]?.updatedAt == file.lastModified) continue
            val content = fileRepository.readText(file.path) ?: continue
            if (fileRepository.isFile(file.path)) {
                metadataRepository.upsertFromContent(path, content, file.lastModified)
            }
        }

        for (path in existing.keys - indexed) {
            val file = path.toPath()
            val relative = runCatching { file.relativeTo(root) }.getOrNull() ?: continue
            if (relative.segments.isEmpty() || relative.segments.any { it == ".." || it.startsWith(".") }) continue
            if (!fileRepository.isFile(file)) metadataRepository.deleteByPath(path)
        }
        lastScan = root to TimeSource.Monotonic.markNow()
    }
}
