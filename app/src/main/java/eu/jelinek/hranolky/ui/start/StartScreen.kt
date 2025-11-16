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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

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

    Scaffold(
        topBar = {
            if (screenSize.isTablet()) StartScreenTopBar(
                navigateToOverview = navigateToOverview
            )
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

    // FIX: Combined, robust logic for keyboard handling
    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            // Request focus for the scanner
            focusRequester.requestFocus()
            // CRITICAL: We must wait for the system to process the focus event
            // (which triggers "Show Keyboard") before we send the "Hide Keyboard" command.
            // If we hide too early, the system's "Show" wins.
            delay(200)
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
                modifier = if (!isManualInput) Modifier.height(0.dp) else Modifier
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

    // FIX: Combined, robust logic for keyboard handling
    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusRequester.requestFocus()
            // CRITICAL: Wait for system to register focus and show keyboard, then hide it.
            delay(300)
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