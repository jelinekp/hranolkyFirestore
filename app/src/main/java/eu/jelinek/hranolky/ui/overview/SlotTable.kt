package eu.jelinek.hranolky.ui.overview

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.formatCubicMeters
import eu.jelinek.hranolky.ui.shared.formatDate
import eu.jelinek.hranolky.ui.start.itemsIndexedWithAlternatingModifier
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllSlotsContent(
    slots: List<WarehouseSlot?>,
    slotSum: SlotSum,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alternateRowModifier =
        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

    val fontWeight = FontWeight.Bold

    Log.d("AllSlotsContent", "slots: ${slots.size}")

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp)
        ) {
            stickyHeader {
                OverviewRowHeader(fontWeight)
            }
            itemsIndexedWithAlternatingModifier(
                slots,
                alternateModifier = alternateRowModifier
            ) { index, slot, modifier ->
                slot?.let { slot ->
                    OverviewRow(slot, navigateToShowLastActions, modifier)
                }
            }

            stickyHeader(
                key = "sum"
            ) {

            }
        }

        SumRow(
            slotSum = slotSum,
            fontWeight = fontWeight
        )
    }


}

@Composable
fun OverviewRowHeader(
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Poslední pohyb",
            fontWeight = fontWeight,
            modifier = Modifier.weight(5f)
        )
        Text(
            "Hranolky",
            fontWeight = fontWeight,
            modifier = Modifier.weight(6f)
        )
        Text(
            "Kvalita",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f)
        )
        Text(
            "Tloušťka",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            "Šířka",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            "Délka",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            "Na skladě",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            "Objem",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun OverviewRow(
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
        val readableDate = formatDate(date)

        Text(
            readableDate,
            modifier = Modifier.weight(5f),
        )
        Text(
            slot.productId,
            modifier = Modifier.weight(6f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            slot.quality.toString(),
            modifier = Modifier.weight(3f)
        )
        Text(
            slot.thickness.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            slot.width.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            slot.length.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )

        Text(
            slot.quantity.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold, // quantity is crucial for warehouse workers
        )

        val volume = slot.getVolume()
        val formattedVolume = String.format("%.3f", volume)
        Text(
            "$formattedVolume m³",
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun SumRow(
    slotSum: SlotSum = SlotSum.EMPTY,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Součet: ", modifier = Modifier.weight(5f),
            fontWeight = fontWeight,
        )
        Text(
            slotSum.count.toString(), modifier = Modifier.weight(6f),
            fontWeight = fontWeight,
        )
        Spacer(
            modifier = Modifier.weight(4 * 3f)
        )
        Text(
            slotSum.quantitySum.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            fontWeight = fontWeight,
        )
        Text(
            formatCubicMeters(slotSum.volumeSum),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            fontWeight = fontWeight,
        )
    }
}