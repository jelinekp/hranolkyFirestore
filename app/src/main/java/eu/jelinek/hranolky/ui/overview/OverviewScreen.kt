package eu.jelinek.hranolky.ui.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel

@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    navigateUp: () -> Unit,
    viewModel: OverViewModel = koinViewModel()
) {
    Scaffold (
        topBar = { OverviewTopBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            Text("placeholder")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewTopBar(
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("Přehled skladových zásob") },
    )
}