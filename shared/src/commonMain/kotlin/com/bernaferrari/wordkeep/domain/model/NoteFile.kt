package com.bernaferrari.wordkeep.domain.model

fun isNoteFileName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in noteExtensions

private val noteExtensions = setOf(
    "md", "markdown", "txt", "todo", "org", "adoc", "asciidoc", "rst",
    "csv", "wiki", "keyvalue",
)
