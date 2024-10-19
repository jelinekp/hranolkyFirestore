package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.formatShortDate
import java.util.Date


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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotTable(
    lastModifiedSlots: List<WarehouseSlot?>,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        "Položky s posledními pohyby:",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 24.dp)
    )


    val alternateRowModifier =
        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
    LazyColumn(
        modifier = Modifier.widthIn(max = 500.dp)
    ) {
        stickyHeader { // Makes the header sticky
            HeaderLastSlotsContent() // Your header row composable function
        }
        itemsIndexedWithAlternatingModifier(
            lastModifiedSlots,
            alternateRowModifier
        ) { index, slot, modifier ->
            slot?.let {
                SlotRow(it, navigateToShowLastActions, modifier)
            }
        }
    }
}

@Composable
fun SlotRow(
    slot: WarehouseSlot,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        Text(
            slot.productId, modifier = Modifier.weight(6f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            slot.quantity.toString(),
            modifier = Modifier.weight(4f),
            textAlign = TextAlign.End
        )
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
        Text("Hranolky", modifier = Modifier.weight(6f))
        Text("Množství", modifier = Modifier.weight(4f), textAlign = TextAlign.End)
    }
}
