package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.ui.shared.NavigationActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartScreenTopBar(
    modifier: Modifier = Modifier,
    navigateToOverview: () -> Unit
) {
    TopAppBar(
        title = { Text("Hranolky") },
        modifier = modifier,
        navigationIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "ikona aplikace",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp),
                tint = Color.Unspecified
            )
        },
        actions = {
            NavigationActionButton(
                text = "Přehled všech položek",
                iconPainter = painterResource(R.drawable.outline_lists_24),
                contentDescription = "Ikona položek",
                onClick = navigateToOverview,
            )
        }
    )
}

