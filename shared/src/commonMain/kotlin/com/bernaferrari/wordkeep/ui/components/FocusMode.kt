package com.bernaferrari.wordkeep.ui.components

/**
 * Apply focus mode styling to paragraphs.
 * Returns alpha value for each paragraph based on whether it's the active one.
 *
 * @param paragraphIndex The index of the paragraph being styled
 * @param currentParagraphIndex The index of the currently focused paragraph
 * @param isFocusMode Whether focus mode is enabled
 * @return Alpha value (0.0 to 1.0) for the paragraph
 */
fun calculateParagraphAlpha(
    paragraphIndex: Int,
    currentParagraphIndex: Int,
    isFocusMode: Boolean
): Float {
    if (!isFocusMode) return 1f

    val distance = kotlin.math.abs(paragraphIndex - currentParagraphIndex)
    return when (distance) {
        0 -> 1f // Current paragraph
        1 -> 0.6f // Adjacent paragraphs
        2 -> 0.35f // Two away
        else -> 0.2f // Further paragraphs
    }
}

/**
 * Determines the current paragraph index based on cursor position.
 */
fun getCurrentParagraphIndex(text: String, cursorPosition: Int): Int {
    if (text.isEmpty() || cursorPosition == 0) return 0

    val beforeCursor = text.substring(0, minOf(cursorPosition, text.length))
    return beforeCursor.count { it == '\n' }
}
