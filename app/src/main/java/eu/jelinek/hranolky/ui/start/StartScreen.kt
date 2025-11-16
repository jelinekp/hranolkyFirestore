package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.history.TabletSlotTable
import eu.jelinek.hranolky.ui.shared.AutoScanToggle
import eu.jelinek.hranolky.ui.shared.ErrorText
import eu.jelinek.hranolky.ui.shared.ManualScanButton
import eu.jelinek.hranolky.ui.shared.NavigationActionButton
import eu.jelinek.hranolky.ui.shared.ScannedCodeInput
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.shared.SignInStatus
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navigateToManageItem: (String) -> Unit,
    navigateToOverview: () -> Unit,
    navigateToHistory: () -> Unit,
    viewModel: StartViewModel = koinViewModel(),
    screenSize: ScreenSize = ScreenSize.PHONE
) {
    val screenState by viewModel.startScreenState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showReleaseNotes by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // Show release notes dialog when update is available
    LaunchedEffect(updateState.isUpdateAvailable) {
        if (updateState.isUpdateAvailable && updateState.releaseNotes.isNotEmpty()) {
            showReleaseNotes = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToManageItem.collect { route ->
            navigateToManageItem(route)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearScannedCode()
            // hide keyboard on screen exit
            keyboardController?.hide()
        }
    }

    // Release notes dialog
    if (showReleaseNotes && updateState.isUpdateAvailable) {
        AlertDialog(
            onDismissRequest = { showReleaseNotes = false },
            title = { Text("Nová verze ${updateState.latestVersion} k dispozici") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Co je nového:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(updateState.releaseNotes, style = typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Až se dokončí stahování aktualizace, povolte oprávnění instalovat a klikněte na \"Instalovat\". Poté znovu otevřete aplikaci.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showReleaseNotes = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (screenSize.isTablet()) StartScreenTopBar(
                navigateToOverview = navigateToOverview
            )
        },
        bottomBar = {
            // Show update progress at the bottom
            if (updateState.isDownloading || updateState.isInstalling) {
                UpdateProgressBar(updateState = updateState)
            }
        }
    ) { padding ->
        if (screenSize.isPhone()) {
            StartScreenPhoneLayout(
                modifier = modifier.padding(padding),
                screenState = screenState,
                viewModel = viewModel,
                navigateToHistory = navigateToHistory,
                navigateToOverview = navigateToOverview,
                keyboardController = keyboardController,
                focusRequester = focusRequester
            )
        } else {
            StartScreenTabletLayout(
                modifier = modifier.padding(padding),
                screenState = screenState,
                viewModel = viewModel,
                navigateToManageItem = navigateToManageItem,
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun StartScreenPhoneLayout(
    modifier: Modifier = Modifier,
    screenState: StartUiState,
    viewModel: StartViewModel,
    navigateToHistory: () -> Unit,
    navigateToOverview: () -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusRequester: FocusRequester
) {
    var isManualInput by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_jelinek),
            contentDescription = "Logo JELÍNEK",
            modifier = Modifier.padding(horizontal = 80.dp),
            colorFilter = if (isSystemInDarkTheme()) {
                ColorFilter.tint(colorScheme.onBackground)
            } else {
                null
            }
        )

        AutoScanToggle(
            isManualInput,
            onAutoScanToggleChange = { isManualInput = it },
            label = "Zadat kód manuálně"
        )

        if (!isManualInput) {
            Text(
                text = "Namiř terminál na QR kód a zmáčkni žluté tlačítko na boku terminálu.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp),
                style = typography.bodySmall
            )
        }

        // Input field for scanner - always present for focus but hidden when not in manual mode
        ScannedCodeInput(
            screenState.scannedCode,
            onValueChange = { viewModel.onScannedCodeChange(it, isManualInput) },
            focusRequester = focusRequester,
            isError = screenState.isFormatError,
            onDoneAction = {
                if (isManualInput) {
                    // In manual mode, just hide keyboard - user must click button to navigate
                    keyboardController?.hide()
                } else {
                    // In auto-scan mode, submit immediately
                    viewModel.onSubmit()
                }
            },
            modifier = Modifier
                .padding(vertical = 8.dp)
                // FIX: Immediately hide keyboard on focus when manual input is disabled.
                // This runs synchronously on focus change, reducing/eliminating the flash.
                .onFocusChanged {
                    if (it.isFocused && !isManualInput) {
                        keyboardController?.hide()
                    }
                }
                .then(
                    if (!isManualInput) Modifier.height(0.dp)
                    else Modifier
                )
        )

        if (screenState.isSignInError) {
            ErrorText("Počkejte na přihlášení. Jste připojeni k internetu?")
        }

        if (screenState.isFormatError) {
            ErrorText(
                "Špatná délka kódu, kód musí začínat na 'H' a být 18 znaků dlouhý, " +
                        "nebo 'S' a být 22 znaků dlouhý, nebo být 16 znaků dlouhý."
            )
        }

        if (isManualInput) {
            ManualScanButton(onClicked = { viewModel.onSubmit() })
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavigationActionButton(
                text = "Historie pohybů",
                iconPainter = painterResource(R.drawable.outline_history_24),
                contentDescription = "Ikona historie",
                onClick = navigateToHistory,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            NavigationActionButton(
                text = "Přehled všech položek",
                iconPainter = painterResource(R.drawable.outline_lists_24),
                contentDescription = "Ikona položek",
                onClick = navigateToOverview,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SignInStatus(screenState = screenState) { // Extracted Composable
            // This block will be the content for the 'else' case in SignInStatus
            if (screenState.isInventoryCheckPermitted) {
                AutoScanToggle(
                    screenState.isInventoryCheckEnabled,
                    onAutoScanToggleChange = { viewModel.toggleInventoryCheck(it) },
                    label = "Inventura ${if (screenState.isInventoryCheckEnabled) "zapnuta" else "vypnuta"}"
                )
            }

            Text(
                text = "Terminál: " + screenState.shortenedDeviceId,
                style = typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 2.dp)
            )
            if (screenState.deviceName != null) {
                Text(
                    text = "Název zařízení: " + screenState.deviceName,
                    style = typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 2.dp)
                )
            }
            Text(
                text = "Verze aplikace: " + screenState.appVersion,
                style = typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 2.dp)
            )
        }
    }

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusRequester.requestFocus()
            // Backup: Wait a tiny bit to ensure system "Show" is overridden if onFocusChanged missed it.
            // Reduced delay to 10ms to minimize visual flash.
            delay(10)
            keyboardController?.hide()
        }
    }
}

@Composable
private fun StartScreenTabletLayout(
    modifier: Modifier = Modifier,
    screenState: StartUiState,
    viewModel: StartViewModel,
    navigateToManageItem: (String) -> Unit,
    focusRequester: FocusRequester
) {
    var isManualInput by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .weight(5f)
                .padding(16.dp)
        ) {
            TabletSlotTable(
                navigateToManageItem = navigateToManageItem,
            )
            Text(
                text = "Terminál: " + screenState.shortenedDeviceId,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 6.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.primaryContainer)
                .weight(5f)
                .padding(horizontal = 64.dp)
                .padding(top = 32.dp)
                .widthIn(max = 220.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Zobrazit detail, přidat pohyb, zobrazit poslední pohyby:",
                style = typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = colorScheme.onPrimaryContainer
            )

            AutoScanToggle(
                isAutoScanEnabled = isManualInput,
                onAutoScanToggleChange = { isManualInput = it },
                label = "Zadat kód manuálně"
            )

            // Input field for scanner - always present for focus but hidden when not in manual mode
            ScannedCodeInput(
                scannedText = screenState.scannedCode,
                onValueChange = { viewModel.onScannedCodeChange(it, isManualInput) },
                focusRequester = focusRequester,
                isError = screenState.isFormatError,
                onDoneAction = {
                    if (isManualInput) {
                        // In manual mode, just hide keyboard - user must click button to navigate
                        keyboardController?.hide()
                    } else {
                        // In auto-scan mode, submit immediately
                        viewModel.onSubmit()
                    }
                },
                modifier = Modifier
                    // FIX: Immediately hide keyboard on focus when manual input is disabled.
                    .onFocusChanged {
                        if (it.isFocused && !isManualInput) {
                            keyboardController?.hide()
                        }
                    }
                    .then(if (!isManualInput) Modifier.height(0.dp) else Modifier)
            )

            if (screenState.isFormatError) {
                ErrorText(
                    "Špatná délka kódu, kód musí začínat na 'H' a být 18 znaků dlouhý, " +
                            "nebo 'S' a být 22 znaků dlouhý, nebo být 16 znaků dlouhý."
                )
            }

            if (isManualInput) {
                ManualScanButton(onClicked = { viewModel.onSubmit() })
            }
        }
    }

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusRequester.requestFocus()
            // Backup: Wait a tiny bit to ensure system "Show" is overridden if onFocusChanged missed it.
            // Reduced delay to 10ms to minimize visual flash.
            delay(10)
            keyboardController?.hide()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenTopBar(modifier: Modifier = Modifier, navigateToOverview: () -> Unit) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier,
        navigationIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "ikona aplikace",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
                tint = Color.Unspecified
            )
        },
        actions = {
            NavigationActionButton(
                text = "Přehled všech položek",
                iconPainter = painterResource(R.drawable.outline_lists_24),
                contentDescription = "Ikona položek",
                onClick = navigateToOverview,
            )
        }
    )
}

@Composable
private fun UpdateProgressBar(
    updateState: eu.jelinek.hranolky.domain.UpdateState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.primaryContainer)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    updateState.isInstalling -> "Instalace aktualizace..."
                    updateState.isDownloading -> "Stahování aktualizace..."
                    else -> "Připravuji aktualizaci..."
                },
                style = typography.bodyMedium,
                color = colorScheme.onPrimaryContainer
            )

            if (updateState.isDownloading) {
                Text(
                    text = "${updateState.downloadProgress}%",
                    style = typography.bodySmall,
                    color = colorScheme.onPrimaryContainer
                )
            } else if (updateState.isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (updateState.isDownloading) {
            LinearProgressIndicator(
                progress = { updateState.downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        } else if (updateState.isInstalling) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        }

        if (updateState.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chyba: ${updateState.error}",
                style = typography.bodySmall,
                color = colorScheme.error
            )
        }
    }
}
