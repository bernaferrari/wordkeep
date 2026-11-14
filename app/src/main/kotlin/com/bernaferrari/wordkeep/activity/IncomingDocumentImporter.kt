package com.bernaferrari.wordkeep.activity

import android.content.ContentResolver
import com.bernaferrari.wordkeep.domain.model.isNoteFileName
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.bernaferrari.wordkeep.domain.repository.IDocumentRepository
import com.bernaferrari.wordkeep.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import java.io.File

/** Converts Android share/open intents into documents the multiplatform editor can open. */
internal class IncomingDocumentImporter(
    private val contentResolver: ContentResolver,
    private val settingsRepository: ISettingsRepository,
    private val documentRepository: IDocumentRepository,
    private val defaultNotebookPath: String,
) {
    suspend fun import(intent: Intent): String? {
        if (intent.action !in SUPPORTED_ACTIONS) return null

        val streamUri = intent.getParcelableExtraCompat(Intent.EXTRA_STREAM)
            ?: intent.clipData?.getItemAt(0)?.uri
            ?: intent.data

        if (streamUri != null) {
            val name = displayName(streamUri, intent)
            val mimeType = contentResolver.getType(streamUri) ?: intent.type
            if (mimeType?.startsWith("text/") != true &&
                !(mimeType in setOf(null, "application/octet-stream", "*/*") && isNoteFileName(name))
            ) return null

            val content = readText(streamUri) ?: return null
            streamUri.directFilePath(intent.action)?.let { return it }
            return createImportedDocument(
                fileName = ensureTextExtension(name),
                content = content,
            )
        }

        val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            ?.toString()
            ?.takeIf { it.isNotEmpty() && it.length <= MAX_IMPORT_BYTES }
            ?.takeIf { it.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES }
            ?: return null

        return createImportedDocument(
            fileName = ensureTextExtension(intent.getStringExtra(Intent.EXTRA_TITLE)),
            content = sharedText,
        )
    }

    private suspend fun createImportedDocument(fileName: String, content: String): String? {
        val notebookPath = settingsRepository.getNotebookDirectory.first()
            .ifBlank { defaultNotebookPath }
        val path = notebookPath.toPath() / sanitizeFileName(fileName)
        return documentRepository.createDocument(path, content)?.path?.toString()
    }

    private fun readText(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count == -1) break
                    if (count > MAX_IMPORT_BYTES - bytes.size()) return null
                    bytes.write(buffer, 0, count)
                }
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray()))
                    .toString()
                    .takeUnless { '\u0000' in it }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun displayName(uri: Uri, intent: Intent): String {
        val queriedName = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

        return queriedName
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: ""
    }

    private fun Uri.directFilePath(action: String?): String? {
        if (scheme != ContentResolver.SCHEME_FILE) return null
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_EDIT) return null

        return path
            ?.let(::File)
            ?.takeIf { it.isFile && it.canRead() }
            ?.absolutePath
    }

    private fun sanitizeFileName(fileName: String): String {
        val sanitized = fileName
            .replace('/', '_')
            .replace('\\', '_')
            .replace(Regex("[\\u0000-\\u001F]"), "_")
            .trim()
            .take(MAX_FILE_NAME_LENGTH)

        return sanitized
            .takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: DEFAULT_FILE_NAME
    }

    private fun ensureTextExtension(fileName: String?): String {
        val name = fileName?.trim().orEmpty()
        return if (name.isBlank()) DEFAULT_FILE_NAME
        else if (name.contains('.')) name
        else "$name.txt"
    }

    @Suppress("DEPRECATION")
    private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? =
        getParcelableExtra(key) as? T

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_SEND,
            Intent.ACTION_VIEW,
            Intent.ACTION_EDIT,
            "com.google.android.gm.action.AUTO_SEND",
            "com.google.android.gms.actions.CREATE_NOTE",
        )
        const val DEFAULT_FILE_NAME = "shared_note.txt"
        const val MAX_IMPORT_BYTES = 2 * 1024 * 1024
        const val MAX_FILE_NAME_LENGTH = 120
    }
}
