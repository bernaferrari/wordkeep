package com.bernaferrari.wordkeep.ui.screens

import com.bernaferrari.wordkeep.ui.icons.MaterialSymbols

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.wordkeep.ui.components.markdownToAnnotatedString
import com.bernaferrari.wordkeep.ui.components.resolveMarkdownColorPalette
import com.bernaferrari.wordkeep.ui.components.supportsSharedStorageMode
import com.bernaferrari.wordkeep.ui.viewmodel.IntroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import markor.shared.generated.resources.Res
import markor.shared.generated.resources.back
import markor.shared.generated.resources.choose_storage_location
import markor.shared.generated.resources.get_started
import markor.shared.generated.resources.intro_description
import markor.shared.generated.resources.intro_local_first
import markor.shared.generated.resources.private_storage
import markor.shared.generated.resources.recommended
import markor.shared.generated.resources.saved
import markor.shared.generated.resources.select_storage_location
import markor.shared.generated.resources.shared_storage
import markor.shared.generated.resources.storage_external_description
import markor.shared.generated.resources.storage_internal_description
import markor.shared.generated.resources.storage_permission_denied
import markor.shared.generated.resources.welcome_to_markor
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class IntroStep {
    Welcome,
    Storage,
}

@Composable
fun IntroScreen(
    onIntroFinished: () -> Unit,
    systemInternalFilesDir: String,
    viewModel: IntroViewModel = koinViewModel(),
) {
    val supportsSharedStorage = remember { supportsSharedStorageMode() }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(IntroStep.Welcome) }
    var requestPermissions by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var isFinishing by remember { mutableStateOf(false) }

    fun finishWithPrivateStorage() {
        if (isFinishing) return
        isFinishing = true
        scope.launch {
            viewModel.setStorageMode(isExternal = false, internalPath = systemInternalFilesDir)
            onIntroFinished()
        }
    }

    com.bernaferrari.wordkeep.ui.components.HandleStoragePermissions(
        onRequest = requestPermissions,
        onGranted = {
            if (!isFinishing) {
                isFinishing = true
                scope.launch {
                    viewModel.setStorageMode(isExternal = true, internalPath = systemInternalFilesDir)
                    onIntroFinished()
                    requestPermissions = false
                }
            }
        },
        onDenied = {
            requestPermissions = false
            permissionDenied = true
        },
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (step) {
                IntroStep.Welcome -> WelcomePage(
                    onContinue = {
                        if (supportsSharedStorage) step = IntroStep.Storage
                        else finishWithPrivateStorage()
                    },
                    enabled = !isFinishing,
                )

                IntroStep.Storage -> StorageSelectionPage(
                    onBack = {
                        permissionDenied = false
                        step = IntroStep.Welcome
                    },
                    onPrivateSelected = ::finishWithPrivateStorage,
                    onSharedSelected = {
                        if (!isFinishing) {
                            permissionDenied = false
                            requestPermissions = true
                        }
                    },
                    permissionDenied = permissionDenied,
                    enabled = !isFinishing && !requestPermissions,
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(
    onContinue: () -> Unit,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            WelcomeNote(modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onContinue,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = stringResource(Res.string.get_started),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Icon(
                imageVector = MaterialSymbols.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val WelcomeNoteFileName = "welcome.md"
private const val WelcomeNoteFontSize = 18
private const val TypingStartDelayMillis = 300L
private const val TypingCharsPerSecond = 90
private const val CaretBlinkMillis = 530L

/**
 * The welcome message, written as a Markdown note the way the editor shows one: same highlighter,
 * same monospace body, on a colored note surface.
 */
@Composable
private fun WelcomeNote(modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.welcome_to_markor)
    val description = stringResource(Res.string.intro_description)
    val localFirst = stringResource(Res.string.intro_local_first)
    val getStarted = stringResource(Res.string.get_started)
    val source = remember(title, description, localFirst, getStarted) {
        "# $title\n\n$description\n\n- [x] $localFirst\n- [ ] $getStarted"
    }

    val colorScheme = MaterialTheme.colorScheme
    val noteColor = colorScheme.primaryContainer
    val palette = remember(colorScheme, noteColor) {
        resolveMarkdownColorPalette(colorScheme, noteColor)
    }
    val highlighted = remember(source, colorScheme, noteColor) {
        markdownToAnnotatedString(
            text = source,
            colorScheme = colorScheme,
            backgroundColor = noteColor,
            editorFontSize = WelcomeNoteFontSize,
        )
    }

    var typed by rememberSaveable(source) { mutableStateOf(false) }
    var typedChars by remember(source) { mutableIntStateOf(if (typed) source.length else 0) }
    var caretVisible by remember { mutableStateOf(true) }
    LaunchedEffect(source) {
        // Animations switched off in system settings: show the finished note, steady caret.
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f) {
            typedChars = source.length
            typed = true
            return@LaunchedEffect
        }
        if (!typed) {
            delay(TypingStartDelayMillis)
            // Driven by the clock, not by a delay per character, so slow frames don't slow the typing.
            val start = withFrameMillis { it }
            while (typedChars < source.length) {
                withFrameMillis { now ->
                    typedChars = ((now - start) * TypingCharsPerSecond / 1000)
                        .toInt()
                        .coerceIn(typedChars, source.length)
                }
            }
            typed = true
        }
        while (true) {
            delay(CaretBlinkMillis)
            caretVisible = !caretVisible
        }
    }

    // The whole note is laid out from the first frame and the untyped part is hidden, so words
    // never jump to the next line mid-type.
    val text = remember(highlighted, typedChars) {
        buildAnnotatedString {
            append(highlighted)
            if (typedChars < highlighted.length) {
                // A brush, not a color: headings are colored with a brush, which a color can't override.
                addStyle(
                    SpanStyle(brush = SolidColor(Color.Transparent)),
                    typedChars,
                    highlighted.length,
                )
            }
        }
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        // A second note underneath, so the screen reads as a notebook, not a dialog.
        Surface(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = -5f
                }
                .clearAndSetSemantics { },
            shape = MaterialTheme.shapes.extraLarge,
            color = colorScheme.tertiaryContainer,
        ) {}

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = noteColor,
            contentColor = palette.body,
        ) {
            Column(modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp)
                        .clearAndSetSemantics { },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = WelcomeNoteFileName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = palette.subtle,
                    )
                    AnimatedVisibility(
                        visible = typed,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = palette.subtle,
                            )
                            Text(
                                text = stringResource(Res.string.saved),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = palette.subtle,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val caretColor = palette.accent
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 40.dp)
                        .drawWithContent {
                            drawContent()
                            val caret = layout?.getCursorRect(typedChars)
                            if (caret != null && caretVisible) {
                                val width = 2.dp.toPx()
                                drawRoundRect(
                                    color = caretColor,
                                    topLeft = Offset(caret.left, caret.top),
                                    size = Size(width, caret.height),
                                    cornerRadius = CornerRadius(width / 2),
                                )
                            }
                        }
                        .clearAndSetSemantics {
                            contentDescription = "$title. $description $localFirst"
                            heading()
                        },
                    onTextLayout = { layout = it },
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = WelcomeNoteFontSize.sp,
                        lineHeight = (WelcomeNoteFontSize * 1.45f).sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StorageSelectionPage(
    onBack: () -> Unit,
    onPrivateSelected: () -> Unit,
    onSharedSelected: () -> Unit,
    permissionDenied: Boolean,
    enabled: Boolean,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = MaterialSymbols.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back),
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(Res.string.choose_storage_location),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.select_storage_location),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(36.dp))

        StorageOption(
            title = stringResource(Res.string.private_storage),
            description = stringResource(Res.string.storage_internal_description),
            icon = MaterialSymbols.Filled.Lock,
            recommended = true,
            enabled = enabled,
            onClick = onPrivateSelected,
        )

        Spacer(modifier = Modifier.height(12.dp))

        StorageOption(
            title = stringResource(Res.string.shared_storage),
            description = stringResource(Res.string.storage_external_description),
            icon = MaterialSymbols.Filled.FolderOpen,
            recommended = false,
            enabled = enabled,
            onClick = onSharedSelected,
        )

        if (permissionDenied) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.storage_permission_denied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StorageOption(
    title: String,
    description: String,
    icon: ImageVector,
    recommended: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (recommended) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (recommended) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (recommended) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (recommended) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (recommended) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = stringResource(Res.string.recommended),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = MaterialSymbols.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
