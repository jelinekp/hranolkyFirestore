package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.shared.formatShortDate
import java.util.Date


fun LazyListScope.itemsIndexedWithAlternatingModifier(
    items: List<WarehouseSlot>,
    alternateModifier: Modifier,
    itemContent: @Composable (index: Int, item: WarehouseSlot, modifier: Modifier) -> Unit
) {
    itemsIndexed(items) { index, item ->
        val modifier = if (index % 2 == 0) {
            Modifier.background(color = MaterialTheme.colorScheme.surface)
        } else {
            alternateModifier
        }
        itemContent(index, item, modifier) // Pass the modifier to itemContent
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlotTable(
    lastModifiedSlots: List<WarehouseSlot>,
    navigateToShowLastActions: (String) -> Unit,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Hranolky s posledními pohyby:",
            style = if (screenSize.isPhone()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        val alternateRowModifier =
            Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

        val topPadding = if (screenSize.isPhone()) 4.dp else 12.dp

        HeaderLastSlotsContent()

        LazyColumn(
            modifier = Modifier
        ) {
            itemsIndexedWithAlternatingModifier(
                lastModifiedSlots,
                alternateRowModifier
            ) { index, slot, modifier ->
                SlotRow(slot, navigateToShowLastActions, modifier)
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
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val date = slot.lastModified?.toDate() ?: Date()
        val readableDate = formatShortDate(date)

        Text(
            readableDate,
            modifier = Modifier.weight(4f),
            //fontSize = 14.sp
        )

        Text(
            text = slot.productId,
            fontSize = 14.sp,
            modifier = modifier.weight(6f).clickable {
                navigateToShowLastActions(slot.productId)
            },
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
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
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("Datum", modifier = Modifier.weight(4f), fontWeight = FontWeight.Bold)
        Text("Hranolky", modifier = Modifier.weight(6f), fontWeight = FontWeight.Bold)
        Text(
            "Množství",
            modifier = Modifier.weight(4f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )
    }
}
