package com.bernaferrari.wordkeep.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import markor.shared.generated.resources.Res
import markor.shared.generated.resources.failed_to_load_document_with_arg
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.bernaferrari.wordkeep.domain.repository.IAssetRepository
import com.bernaferrari.wordkeep.domain.repository.ISettingsRepository
import com.bernaferrari.wordkeep.domain.service.PickedImage
import com.bernaferrari.wordkeep.domain.service.ShareService
import com.bernaferrari.wordkeep.ui.components.EditorAction
import com.bernaferrari.wordkeep.ui.components.FormatToolbar
import com.bernaferrari.wordkeep.ui.components.detectSlashCommand
import com.bernaferrari.wordkeep.ui.components.parseOutline
import com.bernaferrari.wordkeep.ui.components.resolveNoteSurfaceColor
import com.bernaferrari.wordkeep.ui.screens.editor.EditorFlipContent
import com.bernaferrari.wordkeep.ui.screens.editor.EditorKeyboardShortcutHandlers
import com.bernaferrari.wordkeep.ui.screens.editor.EditorOverlays
import com.bernaferrari.wordkeep.ui.screens.editor.EditorTopBar
import com.bernaferrari.wordkeep.ui.screens.editor.buildDocumentInfoStats
import com.bernaferrari.wordkeep.ui.screens.editor.editorKeyboardShortcuts
import com.bernaferrari.wordkeep.ui.screens.editor.insertAtCursor
import com.bernaferrari.wordkeep.ui.screens.editor.insertAtStartOfLine
import com.bernaferrari.wordkeep.ui.screens.editor.wrapSelection
import com.bernaferrari.wordkeep.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Platform-specific back handler.
 * On Android, uses BackHandler. On iOS, does nothing (no hardware back button).
 */
@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)

/**
 * Platform-specific image picker.
 * Returns null if cancelled or not supported.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImagePicked: (PickedImage?) -> Unit,
): () -> Unit

@Composable
fun EditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    openKeyboardOnStart: Boolean = false,
    embeddedInDialog: Boolean = false,
    onRegisterDismissHandler: ((() -> Unit) -> Unit)? = null,
    viewModel: EditorViewModel = koinViewModel(key = filePath),
    shareService: ShareService = koinInject(),
) {
    val settingsRepository: ISettingsRepository = koinInject()
    val showLineNumbers by settingsRepository.isShowLineNumbers.collectAsState(initial = false)
    val editorFontSize by settingsRepository.getEditorFontSize.collectAsState(initial = 16)
    val wordWrap by settingsRepository.isWordWrap.collectAsState(initial = true)

    val autoFormat by settingsRepository.isEditorAutoFormat.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val undoRedo = viewModel.undoRedo

    var isPreviewMode by remember { mutableStateOf(false) }
    val document = viewModel.document
    val activeFilePath = document?.path?.toString() ?: filePath
    val content = viewModel.content
    val titleInput = if (document == null) filePath.toPath().name.substringBeforeLast(".")
    else viewModel.titleInput
    var initialAutoFocusConsumed by remember { mutableStateOf(false) }
    var editorFocusNonce by remember { mutableIntStateOf(0) }
    var isFocusMode by remember { mutableStateOf(false) }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showOutlinePanel by remember { mutableStateOf(false) }
    var showDocumentInfoDialog by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }

    val noteColor = viewModel.noteColor
    val isArchived = viewModel.isArchived
    val assetManager: IAssetRepository = koinInject()
    var pendingImageInsert by remember { mutableStateOf<PickedImage?>(null) }
    val launchImagePicker = rememberImagePickerLauncher { pendingImageInsert = it }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val outlineItems = remember(content.text) { parseOutline(content.text) }
    val documentInfoStats = remember(content.text, outlineItems.size) {
        buildDocumentInfoStats(content.text, outlineItems.size)
    }
    val slashCommandState = remember(content.text, content.selection) {
        detectSlashCommand(content)
    }

    val navigateBack by rememberUpdatedState(onNavigateBack)
    DisposableEffect(onRegisterDismissHandler) {
        onRegisterDismissHandler?.invoke {
            scope.launch {
                if (viewModel.save(commitTitle = true)) navigateBack()
            }
        }
        onDispose { onRegisterDismissHandler?.invoke {} }
    }

    PlatformBackHandler(enabled = true) {
        scope.launch {
            if (viewModel.save(commitTitle = true)) navigateBack()
        }
    }

    LaunchedEffect(filePath) { viewModel.load(filePath) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flushOnStop() }

    LaunchedEffect(pendingImageInsert) {
        val image = pendingImageInsert ?: return@LaunchedEffect
        pendingImageInsert = null
        val relativePath = assetManager.addImage(activeFilePath.toPath(), image.data, image.fileName)
        if (relativePath != null) {
            undoRedo.pushImmediate(viewModel.content)
            viewModel.updateContent(insertAtCursor(viewModel.content, "![]($relativePath)", ""))
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val editorBackgroundColor = colorScheme.background
    val editorContentSurfaceColor = remember(noteColor, colorScheme) {
        resolveNoteSurfaceColor(noteColor, colorScheme, fallback = colorScheme.surfaceContainerLow)
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(
                if (document == null) Modifier else Modifier.editorKeyboardShortcuts(
                    EditorKeyboardShortcutHandlers(
                        showSearchDialog = showSearchDialog,
                        onDismissSearch = { showSearchDialog = false },
                        onShowSearch = { showSearchDialog = true },
                        content = content,
                        onContentChange = { viewModel.updateContent(it) },
                        onSave = { scope.launch { viewModel.save(commitTitle = true, notifySuccess = true) } },
                        undoRedo = undoRedo,
                        scope = scope,
                    ),
                ),
            ),
        containerColor = if (embeddedInDialog) {
            MaterialTheme.colorScheme.surface
        } else {
            editorBackgroundColor
        },
        // Dialog content still inherits the host window's inset metrics; clear them so the
        // Keep-style card doesn't pad for the status bar of the screen behind it.
        contentWindowInsets = if (embeddedInDialog) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            EditorTopBar(
                scrollBehavior = scrollBehavior,
                isPreviewMode = isPreviewMode,
                canUndo = undoRedo.undoStack.isNotEmpty(),
                canRedo = undoRedo.redoStack.isNotEmpty(),
                hasOutline = outlineItems.isNotEmpty(),
                isFocusMode = isFocusMode,
                isArchived = isArchived,
                embeddedInDialog = embeddedInDialog,
                onBack = {
                    scope.launch {
                        if (viewModel.save(commitTitle = true)) navigateBack()
                    }
                },
                onTogglePreview = {
                    scope.launch {
                        if (viewModel.save(commitTitle = true)) {
                            isPreviewMode = !isPreviewMode
                        }
                    }
                },
                onShowColorSheet = { showColorSheet = true },
                onArchive = {
                    scope.launch {
                        if (viewModel.save(commitTitle = true)) {
                            viewModel.setArchived(!isArchived)
                            onNavigateBack()
                        }
                    }
                },
                onUndo = {
                    undoRedo.undo(content)?.let {
                        viewModel.updateContent(it)
                    }
                },
                onRedo = {
                    undoRedo.redo(content)?.let {
                        viewModel.updateContent(it)
                    }
                },
                onFindReplace = { showSearchDialog = true },
                onShowOutline = { showOutlinePanel = true },
                onShowDocumentInfo = { showDocumentInfoDialog = true },
                onExport = { showExportDialog = true },
                onToggleFocusMode = { isFocusMode = !isFocusMode },
            )
        },
        bottomBar = {
            if (!isPreviewMode && document != null) {
                FormatToolbar { action ->
                    val updated = when (action) {
                        EditorAction.BOLD -> wrapSelection(content, "**", "**")
                        EditorAction.ITALIC -> wrapSelection(content, "_", "_")
                        EditorAction.STRIKETHROUGH -> wrapSelection(content, "~~", "~~")
                        EditorAction.CODE -> wrapSelection(content, "`", "`")
                        EditorAction.LINK -> insertAtCursor(content, "[", "](url)")
                        EditorAction.IMAGE -> {
                            launchImagePicker()
                            null
                        }
                        EditorAction.HEADER -> insertAtStartOfLine(content, "# ")
                        EditorAction.LIST_BULLET -> insertAtStartOfLine(content, "- ")
                        EditorAction.LIST_NUMBERED -> insertAtStartOfLine(content, "1. ")
                        EditorAction.LIST_TASK -> insertAtStartOfLine(content, "- [ ] ")
                        EditorAction.QUOTE -> insertAtStartOfLine(content, "> ")
                        EditorAction.HORIZONTAL_RULE -> insertAtCursor(content, "\n---\n", "")
                        EditorAction.UNDO, EditorAction.REDO, EditorAction.SEARCH -> null
                    }
                    if (updated != null && updated.text != content.text) {
                        undoRedo.pushImmediate(content)
                        viewModel.updateContent(updated)
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            EditorFlipContent(
                isPreviewMode = isPreviewMode,
                activeFilePath = activeFilePath,
                titleInput = titleInput,
                content = content,
                showLineNumbers = showLineNumbers,
                editorFontSize = editorFontSize,
                wordWrap = wordWrap,
                autoFormat = autoFormat,
                editable = document != null,
                surfaceColor = editorContentSurfaceColor,
                noteAccentColor = noteColor?.let(::Color),
                isFocusMode = isFocusMode,
                editorFocusNonce = editorFocusNonce,
                // Shared bounds vs. still-visible grid cards crash Lookahead on web/dialog overlay.
                enableSharedElements = !embeddedInDialog,
                autoFocusOnStart = openKeyboardOnStart &&
                    !initialAutoFocusConsumed &&
                    document?.content?.isBlank() == true,
                onAutoFocusConsumed = { initialAutoFocusConsumed = true },
                onTitleChange = { viewModel.titleInput = it },
                onTitleCommit = { scope.launch { viewModel.save(commitTitle = true) } },
                onContentChange = { viewModel.updateContent(it, trackTyping = true) },
                onTapToEdit = {
                    editorFocusNonce++
                    isPreviewMode = false
                },
            )

            if (document == null) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    Text(
                        stringResource(Res.string.failed_to_load_document_with_arg, filePath),
                        Modifier.align(Alignment.Center),
                    )
                }
                return@Box
            }
            EditorOverlays(
                showSearchDialog = showSearchDialog,
                showExportDialog = showExportDialog,
                showOutlinePanel = showOutlinePanel,
                showDocumentInfoDialog = showDocumentInfoDialog,
                showColorSheet = showColorSheet,
                showSlashMenu = slashCommandState != null,
                isPreviewMode = isPreviewMode,
                slashQuery = slashCommandState?.second ?: "",
                slashStartIndex = slashCommandState?.first ?: -1,
                content = content,
                document = document,
                activeFilePath = activeFilePath,
                noteColor = noteColor,
                outlineItems = outlineItems,
                documentInfoStats = documentInfoStats,
                undoRedo = undoRedo,
                shareService = shareService,
                onContentChange = { viewModel.updateContent(it) },
                onDismissSearch = { showSearchDialog = false },
                onDismissExport = { showExportDialog = false },
                onDismissOutline = { showOutlinePanel = false },
                onDismissDocumentInfo = { showDocumentInfoDialog = false },
                onDismissColorSheet = { showColorSheet = false },
                onColorSelected = { color -> viewModel.setColor(color) },
            )
        }
    }
}
