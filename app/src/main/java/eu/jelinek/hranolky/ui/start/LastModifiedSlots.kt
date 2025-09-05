package eu.jelinek.hranolky.ui.start // Assuming this is your package

// Import Pager and Tab related components
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.shared.formatShortDate
import kotlinx.coroutines.launch
import java.util.Date


// Your itemsIndexedWithAlternatingModifier can remain the same
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
        itemContent(index, item, modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class) // For PagerState
@Composable
fun SlotTable(
    lastModifiedBeamSlots: List<WarehouseSlot>,
    lastModifiedJointerSlots: List<WarehouseSlot>,
    navigateToShowLastActions: (String) -> Unit,
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
) {
    val tabTitles = listOf("Hranolky", "Spárovky")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Clip only top corners for tabs
            .background(MaterialTheme.colorScheme.surface), // Use surface for the whole tab container
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Main Title (Optional, if you want a title above the tabs)
        Text(
            "Položky s posledními pohyby:",
            style = if (screenSize.isPhone()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(), // Make the TabRow take the full width
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {} // No divider or a custom one if needed
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    text = { Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content of the tabs using HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f) // Pager should take available space
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

                val alternateRowModifier = MaterialTheme.colorScheme.surfaceContainer

                if (currentSlots.isEmpty()) {
                    Text(
                        text = "Žádné ${tabTitles[pageIndex].lowercase()} s posledními pohyby.",
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
                        itemsIndexedWithAlternatingModifier(
                            items = currentSlots,
                            alternateModifier = Modifier.background(color = alternateRowModifier)
                        ) { _, slot, itemModifier ->
                            SlotRow(
                                slot = slot,
                                navigateToShowLastActions = navigateToShowLastActions,
                                modifier = itemModifier
                            )
                        }
                    }
                }
            }
        }
    }
}


// SlotRow and HeaderLastSlotsContent can remain largely the same,
// just ensure they don't have conflicting backgrounds if the parent provides one.

@Composable
fun SlotRow(
    slot: WarehouseSlot,
    navigateToShowLastActions: (String) -> Unit,
    modifier: Modifier = Modifier, // This modifier will come from itemsIndexedWithAlternatingModifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier // Apply the alternating background modifier here
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val date = slot.lastModified?.toDate() ?: Date()
        val readableDate = formatShortDate(date)

        Text(
            readableDate,
            modifier = Modifier.weight(4f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface // Adjust color as per row background
        )

        Text(
            text = slot.productId,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(6f)
                .clickable { navigateToShowLastActions(slot.productId) },
            color = MaterialTheme.colorScheme.primary, // Keep primary for clickable
            fontWeight = FontWeight.Bold
        )

        Text(
            slot.quantity.toString(),
            modifier = Modifier.weight(4f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
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
        Text("Datum", modifier = Modifier.weight(4f), fontWeight = FontWeight.Bold, color = headerColor)
        Text("Kód", modifier = Modifier.weight(6f), fontWeight = FontWeight.Bold, color = headerColor) // Changed from "Hranolky" to generic "Kód"
        Text(
            "Množství",
            modifier = Modifier.weight(4f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            color = headerColor
        )
    }
}