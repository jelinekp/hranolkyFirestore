package eu.jelinek.hranolky.navigation


import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import eu.jelinek.hranolky.ui.overview.OverviewScreen
import eu.jelinek.hranolky.ui.showlast.ShowLastActionsScreen
import eu.jelinek.hranolky.ui.start.StartScreen

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
        composable(route = Screen.StartScreen.route) {
            StartScreen(
                navigateToShowLastActions = {
                    navController.navigate(Screen.ShowLastActionsScreen(it).route) },
                navigateToOverview = { navController.navigate(ScreenNames.OVERVIEW.name) }
            )
        }

        composable(route = Screen.ShowLastActionsScreen("{${Screen.ShowLastActionsScreen.ID}}").route,
            arguments = listOf(
                navArgument(name = Screen.ShowLastActionsScreen.ID) {
                    type = NavType.StringType
                },)) {
            ShowLastActionsScreen(
                navigateUp = { navController.navigateUp() },
            )
        }

        composable(route = ScreenNames.OVERVIEW.name) {
            OverviewScreen(
                navigateUp = { navController.navigateUp() },
                navigateToShowLastActions = {
                    navController.navigate(Screen.ShowLastActionsScreen(it).route) },
            )
        }

    }
}