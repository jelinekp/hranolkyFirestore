package eu.jelinek.hranolky.ui.start

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.history.TabletSlotTable
import eu.jelinek.hranolky.ui.shared.ScreenSize
import org.koin.androidx.compose.koinViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
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
    val focusRequester = remember { FocusRequester() }
    var scannedText by remember { mutableStateOf("") }
    var isAutoScanEnabled by remember { mutableStateOf(true) }
    var isFormatError by remember { mutableStateOf(false) }

    val onSubmit = {
        if (isValidScannedTextFormat(scannedText)) {
            navigateToManageItem(scannedText)
        } else {
            Log.d("Scanned", "Invalid format $scannedText") // S-DUB-A-27-0-5225
            isFormatError = true
        }
    }

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
                //verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_jelinek),
                    contentDescription = "Logo JELÍNEK",
                    modifier = Modifier.padding(horizontal = 64.dp)
                )

                AutoScanToggle(isAutoScanEnabled, onAutoScanToggleChange = { isAutoScanEnabled = it })

                ScannedCodeInput(
                    scannedText,
                    onValueChange = { text ->
                        scannedText = text
                        if (isAutoScanEnabled && isValidScannedTextFormat(text)) {
                            navigateToManageItem(text)
                        }
                    },
                    focusRequester = focusRequester,
                    isError = isFormatError,
                    onDoneAction = onSubmit,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (isFormatError) {
                    WrongLengthError()
                }

                if (!isAutoScanEnabled) {
                    ManualScanButton(onClicked = {onSubmit})
                }

                Spacer(modifier = Modifier.height(8.dp))

                NavigationActionButton(
                    text = "Historie pohybů",
                    iconPainter = painterResource(R.drawable.outline_history_24),
                    contentDescription = "Ikona historie",
                    onClick = navigateToHistory,
                )

                NavigationActionButton(
                    text = "Přehled všech položek",
                    iconPainter = painterResource(R.drawable.outline_lists_24),
                    contentDescription = "Ikona položek",
                    onClick = navigateToOverview,
                )

                Text(
                    text = "Terminál: " + screenState.shortenedDeviceId,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 2.dp)
                )
                Text(
                    text = "Verze aplikace: " + screenState.appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 2.dp)
                )
            }
        } else {
            Row(
                modifier = modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                Column(modifier = Modifier
                    .weight(5f)
                    .padding(16.dp)) {
                    TabletSlotTable(
                        navigateToManageItem = { navigateToManageItem },
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
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .weight(5f)
                        .padding(horizontal = 64.dp)
                        .padding(top = 32.dp)
                        .widthIn(max = 220.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Zobrazit detail, přidat pohyb, zobrazit poslední pohyby:",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    AutoScanToggle(isAutoScanEnabled, onAutoScanToggleChange = { isAutoScanEnabled = it })

                    ScannedCodeInput(
                        scannedText,
                        onValueChange = { text ->
                            scannedText = text
                            if (isAutoScanEnabled && text.length == 16) {
                                navigateToManageItem(text)
                            }
                        },
                        focusRequester = focusRequester,
                        isError = isFormatError,
                        onDoneAction = onSubmit,
                    )

                    if (isFormatError) {
                        WrongLengthError()
                    }

                    if (!isAutoScanEnabled) {
                        ManualScanButton(onClicked = {onSubmit})
                    }

                    // OverviewButton(navigateToOverview)

                }
            }
        }
    }
    LaunchedEffect(Unit) { if (screenSize.isPhone()) focusRequester.requestFocus() }
}

// Extracted composables:
@Composable
fun AutoScanToggle(isAutoScanEnabled: Boolean, onAutoScanToggleChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(280.dp)
            .padding(top = 4.dp)
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
    isError: Boolean,
    onDoneAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = scannedText,
        onValueChange = { newValue ->
            onValueChange(newValue.uppercase())
        },
        label = { Text(stringResource(R.string.naskenuj_kod)) },
        modifier = modifier.focusRequester(focusRequester),
        isError = isError,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDoneAction()
                focusRequester.freeFocus()
            }
        )
    )
}

@Composable
fun WrongLengthError() {
    Text(
        text = "Špatná délka kódu, kód musí začínat na \'H\' nebo \'S\' a být 18 znaků dlouhý, nebo být 16 znaků dlouhý.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun ManualScanButton(
    onClicked: () -> Unit = {}
) {
    Button(
        onClick = { onClicked() }
    ) {
        Text("Přejít na položku")
        Icon(
            Icons.AutoMirrored.Default.ArrowForward,
            contentDescription = stringResource(R.string.forward_icon),
            modifier = Modifier.padding(all = 4.dp)
        )
    }
}

@Composable
fun NavigationActionButton(
    text: String,
    iconPainter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Allow passing additional modifiers
) {
    Button(
        onClick = onClick,
        // Apply a common modifier first, then any additional specific modifiers
        modifier = Modifier
            .padding(vertical = 16.dp)
            .then(modifier) // Chain any passed-in modifier
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = contentDescription,
            modifier = Modifier.padding(all = 4.dp) // Common padding for the icon
        )
        Text(text)
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
