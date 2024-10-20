package eu.jelinek.hranolky.ui.shared

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.jelinek.hranolky.model.SlotAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

fun formatShortDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.", Locale.getDefault())
    return formatter.format(date)
}

fun LazyListScope.slotActionsIndexedWithAlternatingModifier(
    items: List<SlotAction>,
    alternateModifier: Modifier,
    itemContent: @Composable (index: Int, item: SlotAction, modifier: Modifier) -> Unit
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

fun formatCubicMeters(volume: Double?): String {
    if (volume == null) {
        return "0.0 m³"
    } else
    return String.format("%.3f m³", volume)
}

fun formatCubicMetersTwo(volume: Double?): String {
    return if (volume == null) {
        "0.0"
    } else
        String.format("%.2f", volume)
}