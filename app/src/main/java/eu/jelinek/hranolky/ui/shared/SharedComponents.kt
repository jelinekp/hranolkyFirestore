package eu.jelinek.hranolky.ui.shared

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.jelinek.hranolky.model.SlotAction
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatShortDate(date: Date): String {
    val inputLocalDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when (inputLocalDate) {
        today -> "dnes"
        yesterday -> "včera"
        else -> {
            val formatter = SimpleDateFormat("dd.MM.", Locale.getDefault())
            formatter.format(date)
        }
    }
}

fun LazyListScope.slotActionsIndexedWithAlternatingModifier(
    items: List<SlotAction>,
    alternateModifier: Modifier,
    itemContent: @Composable (index: Int, item: SlotAction, modifier: Modifier) -> Unit
) {
    itemsIndexed(items) { index, item ->
        val modifier = if (index % 2 == 0) {
            Modifier.background(MaterialTheme.colorScheme.surface)
        } else {
            alternateModifier
        }
        itemContent(index, item, modifier)
    }
}

fun formatCubicMeters(volume: Double?): String {
    return if (volume == null) {
        "0.0 m³"
    } else
        String.format("%.3f m³", volume)
}

fun formatCubicMetersTwo(volume: Double?): String {
    return if (volume == null) {
        "0.0"
    } else
        String.format("%.2f", volume)
}