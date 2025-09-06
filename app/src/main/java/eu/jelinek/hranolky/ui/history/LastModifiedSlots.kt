package eu.jelinek.hranolky.ui.history // Assuming this is your package

// Import Pager and Tab related components
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.shared.formatShortDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class) // For PagerState
@Composable
fun SlotTable(
    lastModifiedBeamSlots: List<WarehouseSlot>,
    lastModifiedJointerSlots: List<WarehouseSlot>,
    navigateToManageItem: (String) -> Unit,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { SlotType.entries.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)) // Clip only top corners for tabs
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Main Title (Optional, if you want a title above the tabs)
        Text(
            "Položky s posledními pohyby:",
            style = if (screenSize.isPhone()) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        TabHeader(pagerState, coroutineScope)
        TabContent(pagerState, lastModifiedBeamSlots, lastModifiedJointerSlots, navigateToManageItem)
    }
}

@Composable
private fun TabContent(
    pagerState: PagerState,
    lastModifiedBeamSlots: List<WarehouseSlot>,
    lastModifiedJointerSlots: List<WarehouseSlot>,
    navigateToManageItem: (String) -> Unit
) {
    HorizontalPager(
        state = pagerState,
    ) { pageIndex ->
        val currentSlots = when (pageIndex) {
            0 -> lastModifiedBeamSlots
            1 -> lastModifiedJointerSlots
            else -> emptyList()
        }
        // Each page content will be a list
        Column(
            modifier = Modifier
                .fillMaxSize() // Ensure column fills width
                .background(MaterialTheme.colorScheme.primaryContainer) // Background for list content
        ) {
            HeaderLastSlotsContent() // Display header for each list

            if (currentSlots.isEmpty()) {
                Text(
                    text = "Žádné ${
                        SlotType.entries[pageIndex].toLongName().lowercase()
                    } s posledními pohyby.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f) // LazyColumn takes remaining space in its parent Column
                ) {
                    itemsIndexed(
                        items = currentSlots,
                    ) { index, slot ->
                        SlotRow(
                            slot = slot,
                            navigateToShowLastActions = navigateToManageItem,
                            backgroundColor = if (index % 2 == 0) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeader(
    pagerState: PagerState,
    coroutineScope: CoroutineScope
) {
    TabRow(
        selectedTabIndex = pagerState.currentPage,
        modifier = Modifier.fillMaxWidth(), // Make the TabRow take the full width
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                height = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        divider = {}
    ) {
        SlotType.entries.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = {
                    Row() {
                        Icon(
                            painter = painterResource(title.smallIcon()),
                            contentDescription = title.toLongName(),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = title.toLongName(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SlotRow(
    slot: WarehouseSlot,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val date = slot.lastModified?.toDate() ?: Date()
        val readableDate = formatShortDate(date)

        Text(
            readableDate,
            modifier = Modifier.weight(4f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface // Adjust color as per row background
        )

        Text(
            text = slot.productId,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(11f)
                .clickable { navigateToShowLastActions(slot.productId) },
            color = MaterialTheme.colorScheme.primary, // Keep primary for clickable
            fontWeight = FontWeight.Bold
        )

        Text(
            slot.quantity.toString(),
            modifier = Modifier.weight(3f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface // Adjust color
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
            // .background(MaterialTheme.colorScheme.primaryContainer) // Background now handled by Pager's Column
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val headerColor = MaterialTheme.colorScheme.onPrimaryContainer // Color for text on primaryContainer
        Text("Datum", modifier = Modifier.weight(4f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = headerColor)
        Text("Kód", modifier = Modifier.weight(9f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = headerColor) // Changed from "Hranolky" to generic "Kód"
        Text(
            "Množství",
            modifier = Modifier.weight(5f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            color = headerColor
        )
    }
}