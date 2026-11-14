package com.bernaferrari.wordkeep.ui.screens.editor

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class EditorKeyboardShortcutHandlers(
    val showSearchDialog: Boolean,
    val onDismissSearch: () -> Unit,
    val onShowSearch: () -> Unit,
    val content: TextFieldValue,
    val onContentChange: (TextFieldValue) -> Unit,
    val onSave: () -> Unit,
    val undoRedo: EditorUndoRedoState,
    val scope: CoroutineScope,
)

internal fun Modifier.editorKeyboardShortcuts(handlers: EditorKeyboardShortcutHandlers): Modifier =
    onPreviewKeyEvent { event ->
        if (event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
            return@onPreviewKeyEvent when {
                handlers.showSearchDialog -> {
                    handlers.onDismissSearch()
                    true
                }
                else -> false
            }
        }

        if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
            return@onPreviewKeyEvent when (event.key) {
                Key.S -> {
                    handlers.scope.launch { handlers.onSave() }
                    true
                }
                Key.Z -> {
                    val updated = if (event.isShiftPressed) {
                        handlers.undoRedo.redo(handlers.content)
                    } else {
                        handlers.undoRedo.undo(handlers.content)
                    }
                    if (updated != null) {
                        handlers.onContentChange(updated)
                    }
                    true
                }
                Key.Y -> {
                    handlers.undoRedo.redo(handlers.content)?.let { updated ->
                        handlers.onContentChange(updated)
                    }
                    true
                }
                Key.B -> {
                    handlers.undoRedo.pushImmediate(handlers.content)
                    handlers.onContentChange(wrapSelection(handlers.content, "**", "**"))
                    true
                }
                Key.I -> {
                    handlers.undoRedo.pushImmediate(handlers.content)
                    handlers.onContentChange(wrapSelection(handlers.content, "_", "_"))
                    true
                }
                Key.U -> {
                    handlers.undoRedo.pushImmediate(handlers.content)
                    handlers.onContentChange(wrapSelection(handlers.content, "`", "`"))
                    true
                }
                Key.K -> {
                    handlers.undoRedo.pushImmediate(handlers.content)
                    handlers.onContentChange(wrapSelection(handlers.content, "[", "](url)"))
                    true
                }
                Key.F, Key.H -> {
                    handlers.onShowSearch()
                    true
                }
                Key.A, Key.C, Key.X, Key.V, Key.Enter -> false
                else -> false
            }
        }

        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
            handlers.undoRedo.pushImmediate(handlers.content)
            handlers.onContentChange(
                insertAtCursor(handlers.content, "\t", ""),
            )
            true
        } else {
            false
        }
    }