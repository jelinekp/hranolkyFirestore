package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.domain.update.UpdateState

@Composable
internal fun UpdateProgressBar(
    updateState: UpdateState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.primaryContainer)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    updateState.isInstalling -> "Instalace aktualizace..."
                    updateState.isDownloading -> "Stahování aktualizace..."
                    else -> "Připravuji aktualizaci..."
                },
                style = typography.bodyMedium,
                color = colorScheme.onPrimaryContainer
            )

            if (updateState.isDownloading) {
                Text(
                    text = "${updateState.downloadProgress}%",
                    style = typography.bodySmall,
                    color = colorScheme.onPrimaryContainer
                )
            } else if (updateState.isInstalling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (updateState.isDownloading) {
            LinearProgressIndicator(
                progress = { updateState.downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        } else if (updateState.isInstalling) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        }

        if (updateState.error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chyba: ${updateState.error}",
                style = typography.bodySmall,
                color = colorScheme.error
            )
        }
    }
}

