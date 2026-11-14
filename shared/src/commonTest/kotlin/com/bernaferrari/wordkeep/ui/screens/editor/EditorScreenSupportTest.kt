package com.bernaferrari.wordkeep.ui.screens.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorScreenSupportTest {
    @Test
    fun buildDocumentInfoStats_countsWordsAndLines() {
        val stats = buildDocumentInfoStats("# Title\n\nHello world", headings = 1)
        assertEquals(1, stats.headings)
        assertEquals(3, stats.lines)
        assertEquals(4, stats.words)
        assertEquals(20, stats.characters)
        assertTrue(stats.bytes > 0)
    }

    @Test
    fun buildTargetName_preservesExtension() {
        assertEquals("My Note.md", buildTargetName("My Note", "old.md"))
        assertEquals("note.md", buildTargetName("note.md", "old.md"))
    }

    @Test
    fun wrapSelection_wrapsSelectedText() {
        val value = TextFieldValue("hello world", TextRange(0, 5))
        val wrapped = wrapSelection(value, "**", "**")
        assertEquals("**hello** world", wrapped.text)
        assertEquals(7, wrapped.selection.start)
    }

    @Test
    fun insertAtStartOfLine_addsPrefix() {
        val value = TextFieldValue("item\nsecond", TextRange(5, 5))
        val updated = insertAtStartOfLine(value, "- ")
        assertEquals("item\n- second", updated.text)
    }

    @Test
    fun handleSmartEnter_continuesBulletList() {
        var result: TextFieldValue? = null
        val value = TextFieldValue("- item one", TextRange(10, 10))
        val handled = handleSmartEnter(value) { result = it }
        assertTrue(handled)
        assertEquals("- item one\n- ", result?.text)
    }
    @Test
    fun wrapSelection_acceptsBackwardSelection() {
        val value = TextFieldValue("hello world", TextRange(5, 0))
        val wrapped = wrapSelection(value, "**", "**")
        assertEquals("**hello** world", wrapped.text)
        assertEquals(TextRange(7), wrapped.selection)
    }

    @Test
    fun handleSmartEnter_exitsEmptyListInPlace() {
        var result: TextFieldValue? = null
        val value = TextFieldValue("before\n- [ ] \nafter", TextRange(13))
        assertTrue(handleSmartEnter(value) { result = it })
        assertEquals("before\n\n\nafter", result?.text)
        assertEquals(TextRange(8), result?.selection)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun undo_restoresTextBeforeTypingEvenAfterPause() = runTest {
        val history = EditorUndoRedoState(this)
        val original = TextFieldValue("original")
        val first = TextFieldValue("original!")
        val second = TextFieldValue("original!!")
        history.trackContentChange(original, first)
        history.trackContentChange(first, second)
        advanceTimeBy(2_001)

        assertEquals(original, history.undo(second))
        assertEquals(second, history.redo(original))
    }
}
