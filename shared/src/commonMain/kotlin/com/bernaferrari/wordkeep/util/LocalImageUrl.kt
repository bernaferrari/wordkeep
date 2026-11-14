package com.bernaferrari.wordkeep.util

import okio.Path

fun resolveImageUrl(
    imageUrl: String,
    markdownFilePath: Path
): String? {
    // Already absolute path or URL
    if (imageUrl.startsWith("http://") ||
        imageUrl.startsWith("https://") ||
        imageUrl.startsWith("/")
    ) {
        return imageUrl
    }

    // Resolve relative to markdown file's directory
    val parentDir = markdownFilePath.parent ?: return null

    return try {
        val resolved = parentDir.resolve(imageUrl)
        resolved.toString()
    } catch (_: Exception) {
        null
    }
}
