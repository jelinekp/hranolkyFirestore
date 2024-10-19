package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun FilterSlots(
    modifier: Modifier = Modifier,
    allFilters: SlotFilters = SlotFilters.ALL,
    selectedFilters: SlotFilters,
    onQualityFilterChange: (List<String>) -> Unit,
    onThicknessFilterChange: (List<Float>) -> Unit,
    onWidthFilterChange: (List<Float>) -> Unit,
    onLengthFilterChange: (List<IntervalMm>) -> Unit,
    onFilterClear: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Filtrování:",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        FilterChipGroup(
            label = "Filtr kvality",
            filters = allFilters.qualityFilters,
            selectedFilters = selectedFilters.qualityFilters,
            onFilterChange = onQualityFilterChange as (List<Any>) -> Unit
        )
        FilterChipGroup(
            label = "Filtr tloušťky",
            filters = allFilters.thicknessFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.thicknessFilters,
            onFilterChange = onThicknessFilterChange as (List<Any>) -> Unit
        )
        FilterChipGroup(
            label = "Filtr šířky",
            filters = allFilters.widthFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.widthFilters,
            onFilterChange = onWidthFilterChange as (List<Any>) -> Unit
        )
        FilterChipGroup(
            label = "Filtr délky",
            filters = allFilters.lengthFilters,
            selectedFilters = selectedFilters.lengthFilters,
            onFilterChange = onLengthFilterChange as (List<Any>) -> Unit
        )
        HorizontalDivider()
        OutlinedButton(
            onClick = onFilterClear,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(text = "Resetovat všechny filtry")
        }
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
        // modifier = Modifier.fillMaxWidth()
    )

    Column(
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Text(text = label, Modifier.padding(bottom = 2.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement
                    .spacedBy(10.dp),
                modifier = Modifier.weight(8f)
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
                        label = { Text(filter.toString() + suffix) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
            IconButton(
                onClick = { onFilterChange(listOf()) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resetovat filtry"
                )
            }
        }

    }
}