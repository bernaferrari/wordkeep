package com.bernaferrari.wordkeep.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun HandleStoragePermissions(
    onRequest: Boolean,
    onGranted: () -> Unit,
    onDenied: () -> Unit
)
