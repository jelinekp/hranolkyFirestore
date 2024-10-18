package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun FilterSlots(
    modifier: Modifier = Modifier,
    allFilters: SlotFilters = SlotFilters.ALL,
    selectedFilters: List<Any>,
    onFilterChange: (List<Any>) -> Unit
) {
    Column (
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Filtrování:",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        FilterChipGroup(
            label = "Filtr kvality",
            filters = allFilters.qualityFilters,
            selectedFilters = selectedFilters,
            onFilterChange = onFilterChange
        )
        FilterChipGroup(
            label = "Filtr tloušťky",
            filters = allFilters.thicknessFilters,
            suffix = " mm",
            selectedFilters = selectedFilters,
            onFilterChange = onFilterChange
        )
        FilterChipGroup(
            label = "Filtr šířky",
            filters = allFilters.widthFilters,
            suffix = " mm",
            selectedFilters = selectedFilters,
            onFilterChange = onFilterChange
        )
        FilterChipGroup(
            label = "Filtr délky",
            filters = allFilters.lengthFilters,
            selectedFilters = selectedFilters,
            onFilterChange = onFilterChange
        )
    }
}



@Composable
fun FilterChipGroup(
    label: String,
    filters: List<Any>,
    suffix: String = "",
    selectedFilters: List<Any>,
    onFilterChange: (List<Any>) -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth()
    )
    Column(
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Text(text = label, Modifier.padding(bottom = 2.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilters.contains(filter)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = if (isSelected) {
                            selectedFilters - filter
                        } else {
                            selectedFilters + filter
                        }
                        onFilterChange(newSelection)
                    },
                    label = { Text(filter.toString() + suffix) }
                )
            }
        }
    }
}