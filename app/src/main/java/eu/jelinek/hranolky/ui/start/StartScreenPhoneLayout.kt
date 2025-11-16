package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.shared.AutoScanToggle
import eu.jelinek.hranolky.ui.shared.ErrorText
import eu.jelinek.hranolky.ui.shared.ManualScanButton
import eu.jelinek.hranolky.ui.shared.NavigationActionButton
import eu.jelinek.hranolky.ui.shared.ScannedCodeInput
import eu.jelinek.hranolky.ui.shared.SignInStatus
import kotlinx.coroutines.delay

@Composable
internal fun StartScreenPhoneLayout(
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

