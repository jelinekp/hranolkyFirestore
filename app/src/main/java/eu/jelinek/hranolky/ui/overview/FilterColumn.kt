package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        FilterChipGroup<String>(
            label = "Filtr kvality",
            filters = allFilters.qualityFilters,
            selectedFilters = selectedFilters.qualityFilters,
            onFilterChange = onQualityFilterChange
        )
        FilterChipGroup<Float>(
            label = "Filtr tloušťky",
            filters = allFilters.thicknessFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.thicknessFilters,
            onFilterChange = onThicknessFilterChange
        )
        FilterChipGroup<Float>(
            label = "Filtr šířky",
            filters = allFilters.widthFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.widthFilters,
            onFilterChange = onWidthFilterChange
        )
        FilterChipGroup<IntervalMm>(
            label = "Filtr délky",
            filters = allFilters.lengthFilters,
            selectedFilters = selectedFilters.lengthFilters,
            onFilterChange = onLengthFilterChange
        )
        HorizontalDivider()

        if (selectedFilters.getNumberOfActiveFilters() > 0) {
            TextButton(
                onClick = { onFilterClear() },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(text = "Resetovat všechny filtry")
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resetovat filtry"
                )
            }
        }
    }
}

@Composable
fun FilterMobileSlots(
    modifier: Modifier = Modifier,
    allFilters: SlotFilters = SlotFilters.ALL,
    selectedFilters: SlotFilters,
    onQualityFilterChange: (List<String>) -> Unit,
    onThicknessFilterChange: (List<Float>) -> Unit,
    onWidthFilterChange: (List<Float>) -> Unit,
    onLengthFilterChange: (List<IntervalMm>) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        FilterMobileChipGroup<String>(
            label = "Filtr kvality",
            filters = allFilters.qualityFilters,
            selectedFilters = selectedFilters.qualityFilters,
            onFilterChange = onQualityFilterChange
        )
        FilterMobileChipGroup<Float>(
            label = "Filtr tloušťky",
            filters = allFilters.thicknessFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.thicknessFilters,
            onFilterChange = onThicknessFilterChange
        )
        FilterMobileChipGroup<Float>(
            label = "Filtr šířky",
            filters = allFilters.widthFilters,
            suffix = " mm",
            selectedFilters = selectedFilters.widthFilters,
            onFilterChange = onWidthFilterChange
        )
        FilterMobileChipGroup<IntervalMm>(
            label = "Filtr délky",
            filters = allFilters.lengthFilters,
            selectedFilters = selectedFilters.lengthFilters,
            onFilterChange = onLengthFilterChange
        )
    }
}


@Composable
fun <T> FilterChipGroup(
    label: String,
    filters: List<T>,
    suffix: String = "",
    selectedFilters: List<T>,
    onFilterChange: (List<T>) -> Unit
) {
    HorizontalDivider()

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
                modifier = Modifier.weight(8f).horizontalScroll(rememberScrollState())
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
            if (selectedFilters.isNotEmpty()) {
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
}


@Composable
fun <T> FilterMobileChipGroup(
    label: String,
    filters: List<T>,
    suffix: String = "",
    selectedFilters: List<T>,
    onFilterChange: (List<T>) -> Unit
) {
    HorizontalDivider()

    Column(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, Modifier.padding(bottom = 2.dp))
            if (selectedFilters.isNotEmpty()) {
                IconButton(
                    onClick = { onFilterChange(listOf()) },
                    modifier = Modifier
                        .padding(end = 10.dp).size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Resetovat filtry",
                        Modifier.size(24.dp)
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement
                .spacedBy(10.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
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

    }
}