package com.bernaferrari.wordkeep.domain.model

enum class DocumentFormat(val extension: String, val displayName: String) {
    MARKDOWN(".md", "Markdown"),
    TODOTXT(".txt", "Todo.txt"),
    CSV(".csv", "CSV"),
    WIKITEXT(".wiki", "Wikitext"),
    ORGMODE(".org", "Org-mode"),
    ASCIIDOC(".adoc", "AsciiDoc"),
    PLAINTEXT(".txt", "Plaintext"),
    KEYVALUE(".keyvalue", "Key-Value"),
    EPUB(".epub", "EPUB");

    companion object {
        fun fromExtension(ext: String): DocumentFormat {
            val normalizedExt = if (ext.startsWith(".")) ext.lowercase() else ".${ext.lowercase()}"
            return when (normalizedExt) {
                ".markdown" -> MARKDOWN
                ".asciidoc" -> ASCIIDOC
                ".todo" -> TODOTXT
                ".txt" -> PLAINTEXT
                else -> entries.find { it.extension == normalizedExt } ?: PLAINTEXT
            }
        }
    }
}
