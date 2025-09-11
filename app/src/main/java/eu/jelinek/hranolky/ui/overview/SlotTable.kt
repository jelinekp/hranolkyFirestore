package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.data.helpers.formatCubicMeters
import eu.jelinek.hranolky.data.helpers.formatCubicMetersTwo

@Composable
fun AllSlotsContent(
    slots: List<WarehouseSlot>,
    slotSum: SlotSum,
    sortingBy: String,
    sortingDirection: SortingDirection,
    updateSorting: (String) -> Unit,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier,
    screenSize: ScreenSize = ScreenSize.TABLET
) {
    val fontWeight = FontWeight.Bold

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        OverviewRowHeader(
            fontWeight = fontWeight,
            sortingBy = sortingBy,
            sortingDirection = sortingDirection,
            updateSorting = updateSorting,
            screenSize = screenSize,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 44.dp)
        ) {
            itemsIndexed(
                slots,
            ) { index, slot ->
                OverviewRow(
                    slot = slot,
                    navigateToShowLastActions = navigateToShowLastActions,
                    modifier = modifier,
                    screenSize = screenSize,
                    backgroundColor = if (index % 2 == 0) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        if (screenSize.isTablet()) {
            SumRow(
                slotSum = slotSum,
                fontWeight = fontWeight
            )
        } else {
            SumMobileRow(
                slotSum = slotSum,
                fontWeight = fontWeight
            )
        }
    }


}

@Composable
fun OverviewRowHeader(
    fontWeight: FontWeight,
    sortingBy: String,
    sortingDirection: SortingDirection,
    updateSorting: (String) -> Unit,
    modifier: Modifier = Modifier,
    screenSize: ScreenSize = ScreenSize.TABLET
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        /*
        Text(
            "Poslední pohyb",
            fontWeight = fontWeight,
            modifier = Modifier.weight(5f)
        )
        Text(
            "Hranolky",
            fontWeight = fontWeight,
            modifier = Modifier.weight(6f)
        )*/
        Text(
            text = if (screenSize.isTablet()) "Kvalita" else "K",
            fontWeight = fontWeight,
            modifier = if (screenSize.isTablet()) Modifier.weight(2f) else Modifier.weight(1f)
        )
        HeaderItem(
            text = if (screenSize.isTablet()) "Tloušťka" else "T",
            fontWeight = fontWeight,
            isSorteredBy = sortingBy == "thickness",
            sortingDirection = sortingDirection,
            modifier = Modifier
                .weight(3f)
                .clickable(
                    onClick = {
                        updateSorting("thickness")
                    }
                ),
            textAlign = TextAlign.End
        )
        HeaderItem(
            text = if (screenSize.isTablet()) "Šířka" else "Š",
            fontWeight = fontWeight,
            isSorteredBy = sortingBy == "width",
            sortingDirection = sortingDirection,
            modifier = Modifier
                .weight(3f)
                .clickable(
                    onClick = {
                        updateSorting("width")
                    }
                ),
            textAlign = TextAlign.End
        )
        HeaderItem(
            text = if (screenSize.isTablet()) "Délka" else "D",
            fontWeight = fontWeight,
            isSorteredBy = sortingBy == "length",
            sortingDirection = sortingDirection,
            modifier = Modifier
                .weight(3f)
                .clickable(
                    onClick = {
                        updateSorting("length")
                    }
                ),
            textAlign = TextAlign.End
        )
        HeaderItem(
            text = if (screenSize.isTablet()) "Na skladě" else "#",
            fontWeight = fontWeight,
            isSorteredBy = sortingBy == "quantity",
            sortingDirection = sortingDirection,
            modifier = Modifier
                .weight(3f)
                .clickable(
                    onClick = {
                        updateSorting("quantity")
                    }
                ),
            textAlign = TextAlign.End
        )
        Text(
            text = if (screenSize.isTablet()) "Objem" else "m³",
            fontWeight = fontWeight,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun HeaderItem(
    text: String,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    isSorteredBy: Boolean = false,
    sortingDirection: SortingDirection = SortingDirection.NONE
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
    ) {
        if (isSorteredBy) {
            if (sortingDirection == SortingDirection.ASC) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "sorting",
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "sorting",
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "sorting",
            )
        }

        Text(
            text = text,
            fontWeight = fontWeight,
            textAlign = textAlign,
            modifier = Modifier
        )
    }
}

@Composable
fun OverviewRow(
    slot: WarehouseSlot,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier,
    screenSize: ScreenSize = ScreenSize.TABLET,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    Row(
        // extract and make it clickable
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .clickable {
                navigateToShowLastActions(slot.productId)
            }
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        /*
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
        )*/
        Text(
            text = if (screenSize.isTablet()) slot.quality.toString() else slot.quality?.getOrNull(4).toString(),
            modifier = if (screenSize.isTablet()) Modifier.weight(2f) else Modifier.weight(1f)
        )
        Text(
            slot.thickness.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
        Text(
            text = formatSlotDimension(slot.width ?: 0.0f),
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
        val formattedVolume = if (screenSize.isTablet()) formatCubicMeters(volume) else formatCubicMetersTwo(volume)
        Text(
            formattedVolume,
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun SumRow(
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    slotSum: SlotSum = SlotSum.EMPTY
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            "Součet: ", modifier = Modifier.weight(2f),
            fontWeight = fontWeight,
        )
        Text(
            slotSum.count.toString(), modifier = Modifier.weight(3f),
            fontWeight = fontWeight,
        )
        Spacer(
            modifier = Modifier.weight(2 * 3f)
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

@Composable
fun SumMobileRow(
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    slotSum: SlotSum = SlotSum.EMPTY
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            slotSum.count.toString(), modifier = Modifier.weight(3f),
            fontWeight = fontWeight,
        )
        Text(
            "Součet: ", modifier = Modifier.weight(4f),
            fontWeight = fontWeight,
        )
        Spacer(
            modifier = Modifier.weight(3f)
        )
        Text(
            slotSum.quantitySum.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            fontWeight = fontWeight,
        )
        Text(
            formatCubicMetersTwo(slotSum.volumeSum),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            fontWeight = fontWeight,
        )
    }
}