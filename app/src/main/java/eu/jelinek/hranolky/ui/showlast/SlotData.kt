package eu.jelinek.hranolky.ui.showlast

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.shared.formatCubicMeters

@Composable
fun SlotData(
    slot: WarehouseSlot,
    modifier: Modifier = Modifier,
    forceExpanded: Boolean = false,
    fontSize: TextUnit = 16.sp
) {
    var isShowMore by rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(12.dp)),
        ) {
            val volume = slot.getVolume() ?: 0.0
            val formattedVolume = formatCubicMeters(volume)

            val alternateRowModifier =
                Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)

            DataRow("Množství na skladě", slot.quantity.toString(), alternateRowModifier, fontSize = fontSize)
            DataRow("Objem dřeva", formattedVolume, fontSize = fontSize)
            if (isShowMore || forceExpanded) {
                DataRow("Kvalita", slot.quality.toString(), alternateRowModifier, fontSize = fontSize)
                DataRow("Tloušťka", "${slot.thickness} mm", fontSize = fontSize)
                DataRow("Šířka", "${slot.width} mm", alternateRowModifier, fontSize = fontSize)
                DataRow("Délka", "${slot.length} mm", fontSize = fontSize)
            }
        }

        if (!forceExpanded) {
            IconButton(
                onClick = { isShowMore = !isShowMore },
                modifier = Modifier.size(48.dp),
            ) {
                val icon = if (isShowMore) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "Zobrazit více",
                    modifier = Modifier.size(40.dp) // Adjust icon size
                )
            }
        }
    }

}

@Composable
fun DataRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp,
            ),
    ) {
        Text(
            text = "$label: ",
            fontSize = fontSize,
            // style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value ?: "neznámá hodnota",
            fontSize = fontSize,
            // style = MaterialTheme.typography.bodySmall
        )
    }
}