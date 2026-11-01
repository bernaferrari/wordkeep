package com.bernaferrari.wordkeep.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bernaferrari.wordkeep.domain.service.PickedImage
import com.bernaferrari.wordkeep.platform.IosImagePicker

@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (PickedImage?) -> Unit
): () -> Unit = remember(onImagePicked) {
    { IosImagePicker.launch(onImagePicked) }
}