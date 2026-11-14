package com.bernaferrari.wordkeep.ui.screens.editor

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal class EditorUndoRedoState(
    private val scope: CoroutineScope,
) {
    val undoStack: SnapshotStateList<TextFieldValue> = mutableStateListOf()
    val redoStack: SnapshotStateList<TextFieldValue> = mutableStateListOf()
    private var debounceJob: Job? = null

    fun pushImmediate(value: TextFieldValue) {
        debounceJob?.cancel()
        debounceJob = null
        push(value)
    }

    private fun push(value: TextFieldValue) {
        redoStack.clear()
        if (undoStack.isEmpty() || undoStack.last().text != value.text) {
            undoStack.add(value)
            if (undoStack.size > MAX_STACK_SIZE) undoStack.removeAt(0)
        }
    }

    fun trackContentChange(previous: TextFieldValue, newContent: TextFieldValue) {
        if (previous.text == newContent.text) return
        val isSignificantChange = abs(newContent.text.length - previous.text.length) > 10
        if (isSignificantChange) {
            pushImmediate(previous)
        } else {
            if (debounceJob == null) push(previous)
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(DEBOUNCE_MS)
                debounceJob = null
            }
        }
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        debounceJob?.cancel()
        debounceJob = null
        if (undoStack.isEmpty()) return null
        redoStack.add(current)
        return undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        debounceJob?.cancel()
        debounceJob = null
        if (redoStack.isEmpty()) return null
        undoStack.add(current)
        return redoStack.removeAt(redoStack.lastIndex)
    }

    private companion object {
        const val MAX_STACK_SIZE = 50
        const val DEBOUNCE_MS = 2_000L
    }
}
