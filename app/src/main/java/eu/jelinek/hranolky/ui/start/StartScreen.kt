package eu.jelinek.hranolky.ui.start

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.shared.ScreenSize
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navigateToShowLastActions: (String) -> Unit,
    navigateToOverview: () -> Unit,
    viewModel: StartViewModel = koinViewModel(),
    screenSize: ScreenSize = ScreenSize.PHONE
) {
    val screenState by viewModel.startScreenState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var scannedText by remember { mutableStateOf("") }
    var isAutoScanEnabled by remember { mutableStateOf(true) }
    var isWrongLength by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { if (screenSize.isTablet()) StartScreenTopBar(
            navigateToOverview = navigateToOverview
        ) }
    ) { padding ->
        if (screenSize.isPhone()) {
            Column(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize()
                    .widthIn(max = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AutoScanToggle(isAutoScanEnabled, onAutoScanToggleChange = { isAutoScanEnabled = it })

                ScannedCodeInput(
                    scannedText,
                    onValueChange = { text ->
                        scannedText = text
                        if (isAutoScanEnabled && text.length == 16) {
                            navigateToShowLastActions(text)
                        }
                    },
                    focusRequester = focusRequester,
                    isError = isWrongLength
                )

                if (isWrongLength) {
                    WrongLengthError()
                }

                if (!isAutoScanEnabled) {
                    ManualScanButton(scannedText, navigateToShowLastActions, onWrongLength = { isWrongLength = it })
                }

                OverviewButton(navigateToOverview)

                SlotTable(screenState.lastModifiedSlots, navigateToShowLastActions)
            }
        } else {
            Row(
                modifier = modifier.padding(padding).fillMaxSize(),
            ) {
                SlotTable(screenState.lastModifiedSlots, navigateToShowLastActions, modifier = Modifier.weight(5f))
                Column(
                    modifier = Modifier.weight(5f).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Zobrazit detail, přidat pohyb, zobrazit poslední pohyby:"
                    )

                    AutoScanToggle(isAutoScanEnabled, onAutoScanToggleChange = { isAutoScanEnabled = it })

                    ScannedCodeInput(
                        scannedText,
                        onValueChange = { text ->
                            scannedText = text
                            if (isAutoScanEnabled && text.length == 16) {
                                navigateToShowLastActions(text)
                            }
                        },
                        focusRequester = focusRequester,
                        isError = isWrongLength
                    )

                    if (isWrongLength) {
                        WrongLengthError()
                    }

                    if (!isAutoScanEnabled) {
                        ManualScanButton(scannedText, navigateToShowLastActions, onWrongLength = { isWrongLength = it })
                    }

                    // OverviewButton(navigateToOverview)

                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

// Extracted composables:
@Composable
fun AutoScanToggle(isAutoScanEnabled: Boolean, onAutoScanToggleChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(280.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Automatický mód",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
        Switch(
            checked = isAutoScanEnabled,
            onCheckedChange = onAutoScanToggleChange
        )
    }
}

@Composable
fun ScannedCodeInput(
    scannedText: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isError: Boolean
) {
    OutlinedTextField(
        value = scannedText,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.naskenuj_kod)) },
        modifier = Modifier.focusRequester(focusRequester),
        isError = isError,
        singleLine = true
    )
}

@Composable
fun WrongLengthError() {
    Text(
        text = "Špatná délka kódu, kód musí být 16 znaků dlouhý.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun ManualScanButton(scannedText: String, navigateToShowLastActions: (String) -> Unit, onWrongLength: (Boolean) -> Unit) {
    Button(
        onClick = {
            Log.d("Scanned text", scannedText)
            if (scannedText.length == 16) {
                navigateToShowLastActions(scannedText)
            } else {
                onWrongLength(true)
            }
        }
    ) {
        Text("Zobrazit stav a poslední pohyby")
    }
}

@Composable
fun OverviewButton(navigateToOverview: () -> Unit) {
    Button(
        onClick = navigateToOverview,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text("Přehled všech hranolků")
        Icon(
            Icons.AutoMirrored.Default.ArrowForward,
            contentDescription = stringResource(R.string.forward_icon)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenTopBar(modifier: Modifier = Modifier, navigateToOverview: () -> Unit) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier,
        actions = { OverviewButton(navigateToOverview) }
    )
}
