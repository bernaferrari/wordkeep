package com.bernaferrari.wordkeep.data.repository

import kotlinx.coroutines.sync.Mutex
import okio.FileSystem
import okio.Path

internal val noteFileMutations = Mutex()

internal fun FileSystem.resolveUniquePath(
    parent: Path,
    requestedName: String,
    excludePath: Path? = null
): Path {
    val normalizedName = requestedName.trim().ifBlank { "Untitled" }
    val (baseName, extension) = splitBaseAndExtension(normalizedName)
    var index = 0

    while (true) {
        val candidateName = if (index == 0) {
            normalizedName
        } else {
            "$baseName ($index)$extension"
        }
        val candidatePath = parent / candidateName
        if (excludePath != null && candidatePath == excludePath) return candidatePath
        if (!exists(candidatePath)) return candidatePath
        index++
    }
}

private fun splitBaseAndExtension(fileName: String): Pair<String, String> {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex <= 0 || dotIndex == fileName.length - 1) {
        return fileName to ""
    }
    return fileName.substring(0, dotIndex) to fileName.substring(dotIndex)
}
