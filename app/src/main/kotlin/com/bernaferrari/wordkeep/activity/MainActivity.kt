package com.bernaferrari.wordkeep.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.bernaferrari.wordkeep.AppContent
import com.bernaferrari.wordkeep.domain.repository.IDocumentRepository
import com.bernaferrari.wordkeep.domain.repository.ISettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import com.bernaferrari.wordkeep.domain.repository.IFavoritesRepository
import com.bernaferrari.wordkeep.widget.MarkorNotesWidget
import com.bernaferrari.wordkeep.widget.RecentFilesWidget
import com.bernaferrari.wordkeep.widget.TodoWidget
import androidx.glance.appwidget.updateAll
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainActivity : ComponentActivity() {
    private val settingsRepository: ISettingsRepository by inject()
    private val documentRepository: IDocumentRepository by inject()
    private val defaultNotebookPath: String by inject(named("default_notebook_path"))

    private val favoritesRepository: IFavoritesRepository by inject()

    private val incomingDocument: IncomingDocumentState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (incomingDocument.path == null) {
            incomingDocument.path = savedInstanceState?.getString(STATE_INCOMING_PATH)
        }

        setContent {
            AppContent(
                systemInternalFilesDir = filesDir.absolutePath,
                onExit = { finish() },
                initialFilePath = incomingDocument.path,
            )
        }

        if (savedInstanceState == null) handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_INCOMING_PATH, incomingDocument.path)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            TodoWidget().updateAll(this@MainActivity)
            RecentFilesWidget().updateAll(this@MainActivity)
            MarkorNotesWidget().updateAll(this@MainActivity)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val importer = IncomingDocumentImporter(
            contentResolver = contentResolver,
            settingsRepository = settingsRepository,
            documentRepository = documentRepository,
            defaultNotebookPath = defaultNotebookPath,
        )
        val settings = settingsRepository
        val documents = documentRepository
        val favorites = favoritesRepository
        val defaultPath = defaultNotebookPath
        incomingDocument.open {
            if (intent.action == ACTION_OPEN_WIDGET) {
                openWidgetNote(intent, settings, documents, favorites, defaultPath)
            } else {
                importer.import(intent)
            }
        }
    }

    companion object {
        internal const val ACTION_OPEN_WIDGET = "com.bernaferrari.wordkeep.OPEN_WIDGET"
        internal const val EXTRA_WIDGET_NOTE = "widget_note"
        internal const val EXTRA_NOTE_PATH = "note_path"
        private const val STATE_INCOMING_PATH = "incoming_path"
    }
}


internal class IncomingDocumentState : ViewModel() {
    var path by mutableStateOf<String?>(null)

    fun open(resolve: suspend () -> String?) {
        viewModelScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) { resolve() }
                if (opened != null) path = opened
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                // A provider can revoke access or disappear before its stream is opened.
            }
        }
    }
}

private suspend fun openWidgetNote(
    intent: Intent,
    settings: ISettingsRepository,
    documents: IDocumentRepository,
    favorites: IFavoritesRepository,
    defaultPath: String,
): String? {
    if (intent.getStringExtra(MainActivity.EXTRA_WIDGET_NOTE) == "recent") {
        return intent.getStringExtra(MainActivity.EXTRA_NOTE_PATH)?.takeIf {
            ".trash" !in it.toPath().segments &&
                it in favorites.recentFiles.first() && documents.documentExists(it.toPath())
        }
    }
    val notebook = settings.getNotebookDirectory.first().ifBlank { defaultPath }.toPath()
    val path = when (intent.getStringExtra(MainActivity.EXTRA_WIDGET_NOTE)) {
        "quick_note" -> settings.getQuickNoteFilePath.first()
            .ifEmpty { (notebook / "quicknote.md").toString() }
        "todo" -> settings.getTodoFilePath.first()
            .ifEmpty { (notebook / "todo.txt").toString() }
        else -> return null
    }
    return if (documents.documentExists(path.toPath())) path
    else documents.createDocument(path.toPath(), "")?.absolutePath
}
