package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.ui.history.TabletSlotTable
import eu.jelinek.hranolky.ui.shared.AutoScanToggle
import eu.jelinek.hranolky.ui.shared.ErrorText
import eu.jelinek.hranolky.ui.shared.ManualScanButton
import eu.jelinek.hranolky.ui.shared.ScannedCodeInput
import kotlinx.coroutines.delay

@Composable
internal fun StartScreenTabletLayout(
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

