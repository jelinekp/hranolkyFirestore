package eu.jelinek.hranolky.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.jelinek.hranolky.model.SlotAction


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
