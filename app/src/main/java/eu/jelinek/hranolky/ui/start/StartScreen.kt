package eu.jelinek.hranolky.ui.start

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.formatShortDate
import org.koin.androidx.compose.koinViewModel
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navigateToShowLastActions: (String) -> Unit,
    viewModel: StartViewModel = koinViewModel()
) {
    var scannedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var isAutoScanEnabled by remember { mutableStateOf(true) }
    var isWrongLength by remember { mutableStateOf(false) }
    val screenState by viewModel.startScreenState.collectAsStateWithLifecycle()

    Scaffold (
        topBar = { StartScreenTopBar() }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Text("Začněte naskenováním kódu")

            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(width = 280.dp)
                ) {
                Text(
                    text = "Automatický mód",
                    style = MaterialTheme.typography.bodyLarge,
                    )
                Switch(
                    checked = isAutoScanEnabled,
                    onCheckedChange = { isAutoScanEnabled = it },
                    )
            }

            TextField(
                value = scannedText,
                onValueChange = { text ->
                    scannedText = text
                    if (isAutoScanEnabled && text.length == 16)
                        navigateToShowLastActions(text)
                },
                modifier = modifier.focusRequester(focusRequester),
                isError = isWrongLength
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

            Text("Položky s posledními pohyby", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 32.dp))


            val alternateRowModifier =
                Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
            LazyColumn {
                stickyHeader { // Makes the header sticky
                    HeaderLastSlotsContent() // Your header row composable function
                }
                itemsIndexedWithAlternatingModifier(screenState.lastModifiedSlots, alternateRowModifier) { index, slot, modifier ->
                    slot?.let {
                        Row(
                            // extract and make it clickable
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = modifier
                                .clickable {
                                    navigateToShowLastActions(slot.productId)
                                }
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            val date = slot.lastModified?.toDate() ?: Date()
                            val readableDate = formatShortDate(date)

                            Text(readableDate, modifier = Modifier.weight(4f))
                            Text(slot.productId, modifier = Modifier.weight(5f))
                            Text(
                                slot.quantity.toString(),
                                modifier = Modifier.weight(4f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

fun LazyListScope.itemsIndexedWithAlternatingModifier(
    items: List<WarehouseSlot?>,
    alternateModifier: Modifier,
    itemContent: @Composable (index: Int, item: WarehouseSlot?, modifier: Modifier) -> Unit
) {
    itemsIndexed(items) { index, item ->
        val modifier = if (index % 2 == 0) {
            Modifier
        } else {
            alternateModifier
        }
        itemContent(index, item, modifier) // Pass the modifier to itemContent
    }
}

@Composable
fun HeaderLastSlotsContent(
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text("Datum", modifier = Modifier.weight(4f))
        Text("Hranolky", modifier = Modifier.weight(5f))
        Text("Množství", modifier = Modifier.weight(4f), textAlign = TextAlign.End)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier
    )
}
