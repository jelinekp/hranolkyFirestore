package eu.jelinek.hranolky.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import eu.jelinek.hranolky.ui.AddActionScreen
import eu.jelinek.hranolky.ui.ResultScreen
import eu.jelinek.hranolky.ui.ShowLastActionsScreen
import eu.jelinek.hranolky.ui.StartScreen

@Composable
fun HranolkyNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: ScreenNames = ScreenNames.START_SCREEN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.name,
        modifier = modifier
    ) {
        composable(ScreenNames.ADD_ACTION.name) {
            AddActionScreen(
                navigateToResult = { navController.navigate(ScreenNames.RESULT.name) },
            )
        }
        composable(ScreenNames.RESULT.name) {
            ResultScreen(
                navigateUp = { navController.navigateUp() },
            )
        }
        composable(ScreenNames.START_SCREEN.name) {
            StartScreen(
                navigateToShowLastActions = { navController.navigate(ScreenNames.SHOW_LAST_ACTIONS.name) }
            )
        }
        composable(ScreenNames.SHOW_LAST_ACTIONS.name) {
            ShowLastActionsScreen(
                navigateUp = { navController.navigateUp() },
                navigateToAddAction = { navController.navigate(ScreenNames.ADD_ACTION.name) }
            )
        }

    }
}