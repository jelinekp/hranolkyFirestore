package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.ui.shared.formatDate
import eu.jelinek.hranolky.ui.shared.slotActionsIndexedWithAlternatingModifier
import java.util.Date


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LastActions(
    lastActions: List<SlotAction>,
    modifier: Modifier = Modifier
) {

    val alternateRowModifier =
        Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

    Text(
        text = "Poslední akce",
        style = MaterialTheme.typography.headlineSmall,
    )
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 550.dp)
            .fillMaxWidth()
            .padding(
                // horizontal = 16.dp
            )
            .heightIn(min = 100.dp),
    ) {
        stickyHeader { // Makes the header sticky
            HeaderRowContent() // Your header row composable function
        }
        slotActionsIndexedWithAlternatingModifier(lastActions, alternateRowModifier) { index, slotAction, modifier ->
            LastActionRow(slotAction, modifier)
        }
    }
}


@Composable
fun HeaderRowContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                vertical = 4.dp,
                horizontal = 8.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Datum a čas",
            modifier = Modifier.weight(8f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Pohyb",
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Změna",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Stav",
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun LastActionRow(
    slotAction: SlotAction,
    modifier: Modifier = Modifier
) {
    val action = when (slotAction.action) {
        "prijem" -> "Příjem"
        "vydej" -> "Výdej"
        else -> "Error"
    }
    val date = slotAction.timestamp?.toDate() ?: Date()
    val readableDate = formatDate(date)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp
            ),
    ) {
        Text(
            readableDate,
            modifier = Modifier.weight(8f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            action,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = slotAction.quantityChange.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = slotAction.newQuantity.toString(),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(3f),
            //style = MaterialTheme.typography.bodySmall
        )
    }
}