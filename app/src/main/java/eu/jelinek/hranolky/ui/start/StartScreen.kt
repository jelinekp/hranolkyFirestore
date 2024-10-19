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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navigateToShowLastActions: (String) -> Unit,
    navigateToOverview: () -> Unit,
    viewModel: StartViewModel = koinViewModel()
) {
    var scannedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var isAutoScanEnabled by remember { mutableStateOf(true) }
    var isWrongLength by remember { mutableStateOf(false) }
    val screenState by viewModel.startScreenState.collectAsStateWithLifecycle()

    Scaffold(
        // topBar = { StartScreenTopBar() }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
                .widthIn(max = 200.dp),
        ) {
            // Text("Začněte naskenováním kódu")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .width(width = 280.dp)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Automatický mód",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Switch(
                    checked = isAutoScanEnabled,
                    onCheckedChange = { isAutoScanEnabled = it },
                )
            }

            OutlinedTextField(
                value = scannedText,
                onValueChange = { text ->
                    scannedText = text
                    if (isAutoScanEnabled && text.length == 16)
                        navigateToShowLastActions(text)
                },
                label = {
                    Text(
                        stringResource(R.string.naskenuj_kod),
                        //style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = modifier.focusRequester(focusRequester),
                isError = isWrongLength,
                singleLine = true
            )

            if (isWrongLength) {
                Text(
                    text = "Špatná délka kódu, kód musí být 16 znaků dlouhý.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (!isAutoScanEnabled) {
                Button(
                    onClick = {
                        Log.d("Scanned text", scannedText)
                        if (scannedText.length == 16)
                            navigateToShowLastActions(scannedText)
                        else
                            isWrongLength = true
                    }
                ) {
                    Text("Zobrazit stav a poslední pohyby")
                }
            }

            Button(
                onClick = { navigateToOverview() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Přehled všech hranolků")
            }

            SlotTable(screenState.lastModifiedSlots, navigateToShowLastActions)
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier
    )
}
