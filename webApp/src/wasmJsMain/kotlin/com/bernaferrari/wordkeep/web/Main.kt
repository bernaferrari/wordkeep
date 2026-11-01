package com.bernaferrari.wordkeep.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.bernaferrari.wordkeep.AppContent
import com.bernaferrari.wordkeep.di.initKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport {
        AppContent(
            systemInternalFilesDir = "/Notebook",
            onExit = { /* Web apps stay in the browser tab */ },
        )
    }
}