package com.bernaferrari.wordkeep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bernaferrari.wordkeep.ui.screens.EditorScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NoteEditorDialog(
    filePath: String,
    sourceBounds: Rect?,
    expansion: () -> Float,
    openKeyboardOnStart: Boolean = false,
    onDismiss: () -> Unit,
) {
    var dismissHandler by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dialogBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    // EditorScreen commits pending edits before invoking onDismiss.
                    onClick = { dismissHandler?.invoke() },
                ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxDialogHeight = maxHeight * 0.92f

            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .heightIn(max = maxDialogHeight)
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
                    .onGloballyPositioned { dialogBounds = it.boundsInRoot() },
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            val source = sourceBounds
                            val target = dialogBounds
                            val remaining = 1f - expansion()
                            alpha = if (source != null && target == null) 0f else 1f
                            if (source != null && target != null && !target.isEmpty) {
                                transformOrigin = TransformOrigin(0f, 0f)
                                translationX = (source.left - target.left) * remaining
                                translationY = (source.top - target.top) * remaining
                                scaleX = 1f + (source.width / target.width - 1f) * remaining
                                scaleY = 1f + (source.height / target.height - 1f) * remaining
                            }
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                ) {
                    EditorScreen(
                        filePath = filePath,
                        openKeyboardOnStart = openKeyboardOnStart,
                        onNavigateBack = onDismiss,
                        embeddedInDialog = true,
                        onRegisterDismissHandler = { dismissHandler = it },
                        viewModel = koinViewModel(key = filePath),
                    )
                }
            }
        }
    }
}
