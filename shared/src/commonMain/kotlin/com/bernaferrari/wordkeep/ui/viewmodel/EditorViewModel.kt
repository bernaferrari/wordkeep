package com.bernaferrari.wordkeep.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.wordkeep.domain.model.Document
import com.bernaferrari.wordkeep.domain.repository.IDocumentRepository
import com.bernaferrari.wordkeep.domain.repository.IFavoritesRepository
import com.bernaferrari.wordkeep.domain.repository.INoteMetadataRepository
import com.bernaferrari.wordkeep.ui.components.UserMessageManager
import com.bernaferrari.wordkeep.ui.screens.editor.EditorUndoRedoState
import com.bernaferrari.wordkeep.ui.screens.editor.commitTitleRenameIfNeeded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import markor.shared.generated.resources.Res
import markor.shared.generated.resources.failed_to_rename
import markor.shared.generated.resources.failed_to_save
import markor.shared.generated.resources.saved
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EditorViewModel(
    private val documentRepository: IDocumentRepository,
    private val metadataRepository: INoteMetadataRepository,
    private val favoritesRepository: IFavoritesRepository,
    private val messageManager: UserMessageManager,
) : ViewModel() {
    var document by mutableStateOf<Document?>(null)
        private set
    var content by mutableStateOf(TextFieldValue())
        private set
    var titleInput by mutableStateOf("")
    var isLoading by mutableStateOf(true)
        private set
    var noteColor by mutableStateOf<Int?>(null)
        private set
    var isArchived by mutableStateOf(false)
        private set

    internal val undoRedo = EditorUndoRedoState(viewModelScope)
    private val writes = Mutex()
    private var autosave: Job? = null

    fun load(path: String) {
        if (document != null) return
        viewModelScope.launch {
            writes.withLock {
                if (document != null) return@withLock
                try {
                    documentRepository.loadDocument(path.toPath())?.let { loaded ->
                        content = TextFieldValue(loaded.content)
                        titleInput = loaded.title.ifBlank { loaded.name.substringBeforeLast(".") }
                        val metadata = metadataRepository.getNoteByPath(path)
                        noteColor = metadata?.color
                        isArchived = metadata?.isArchived == true
                        document = loaded
                        favoritesRepository.recordFileAccess(path)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    document = null
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun updateContent(value: TextFieldValue, trackTyping: Boolean = false) {
        val textChanged = content.text != value.text
        if (trackTyping) undoRedo.trackContentChange(content, value)
        content = value
        if (!textChanged) return
        autosave?.cancel()
        autosave = viewModelScope.launch {
            delay(2_000)
            autosave = null
            save()
        }
    }

    fun flushOnStop() {
        viewModelScope.launch { save() }
    }

    suspend fun save(commitTitle: Boolean = false, notifySuccess: Boolean = false): Boolean =
        writes.withLock {
            val previous = document ?: return@withLock true
            try {
                val text = content.text
                if (previous.content != text) {
                    if (!documentRepository.saveDocument(previous, text)) {
                        messageManager.error(getString(Res.string.failed_to_save))
                        return@withLock false
                    }
                    document = previous.copy(content = text)
                }
                if (commitTitle) {
                    val title = titleInput
                    val result = commitTitleRenameIfNeeded(title, document) { doc, name ->
                        documentRepository.renameDocument(doc, name)?.also { path ->
                            favoritesRepository.updatePath(doc.path.toString(), path.toString())
                        }
                    }
                    document = result.document
                    if (titleInput == title) titleInput = result.updatedTitleInput
                    if (title.isNotBlank() && result.updatedPath == null &&
                        result.document?.title != title.trim()
                    ) {
                        messageManager.error(getString(Res.string.failed_to_rename))
                        return@withLock false
                    }
                }
                if (notifySuccess) messageManager.success(getString(Res.string.saved))
                true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                messageManager.error(getString(Res.string.failed_to_save))
                false
            }
        }

    fun setColor(color: Int?) {
        viewModelScope.launch {
            writes.withLock {
                val path = document?.path?.toString() ?: return@withLock
                metadataRepository.setColor(path, color)
                noteColor = color
            }
        }
    }

    suspend fun setArchived(archived: Boolean) {
        writes.withLock {
            val path = document?.path?.toString() ?: return@withLock
            metadataRepository.setArchived(path, archived)
            isArchived = archived
        }
    }
}
