package com.bernaferrari.wordkeep.domain.repository

enum class FileSortOrder(val preferenceValue: String) {
    NEWEST("date"),
    OLDEST("oldest"),
    NAME("name"),
    SIZE("size");

    fun comparator(): Comparator<FileInfo> = when (this) {
        NEWEST -> compareByDescending { it.lastModified }
        OLDEST -> compareBy { it.lastModified }
        NAME -> compareBy { it.name.lowercase() }
        SIZE -> compareByDescending { it.size }
    }

    companion object {
        fun fromPreference(value: String?): FileSortOrder =
            entries.firstOrNull { it.preferenceValue == value } ?: NEWEST
    }
}
