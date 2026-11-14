package com.bernaferrari.wordkeep.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.bernaferrari.wordkeep.activity.MainActivity
import com.bernaferrari.wordkeep.domain.repository.IDocumentRepository
import com.bernaferrari.wordkeep.domain.repository.IFavoritesRepository
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import org.koin.android.ext.android.getKoin

internal suspend fun widgetRecentNotes(context: Context): List<String> {
    val koin = (context.applicationContext as android.app.Application).getKoin()
    val favorites = koin.get<IFavoritesRepository>()
    val documents = koin.get<IDocumentRepository>()
    return favorites.recentFiles.first()
        .filter { ".trash" !in it.toPath().segments }
        .filter { documents.documentExists(it.toPath()) }
        .take(4)
}

internal fun widgetRecentNoteIntent(context: Context, path: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_OPEN_WIDGET
        data = Uri.Builder().scheme("wordkeep").authority("recent").appendPath(path).build()
        putExtra(MainActivity.EXTRA_WIDGET_NOTE, "recent")
        putExtra(MainActivity.EXTRA_NOTE_PATH, path)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
