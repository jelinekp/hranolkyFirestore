package eu.jelinek.hranolky.navigation


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import eu.jelinek.hranolky.ui.overview.OverviewScreen
import eu.jelinek.hranolky.ui.shared.ScreenSize
import eu.jelinek.hranolky.ui.showlast.ShowLastActionsScreen
import eu.jelinek.hranolky.ui.start.StartScreen

@Composable
fun HranolkyNavHost(
    screenSize: ScreenSize,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {

    val startDestination: ScreenNames =
        if (screenSize.isPhone()) ScreenNames.START_SCREEN else ScreenNames.OVERVIEW

    NavHost(
        navController = navController,
        startDestination = startDestination.name,
        modifier = modifier
    ) {
        composable(route = Screen.StartScreen.route) {
            StartScreen(
                navigateToShowLastActions = {
                    navController.navigate(Screen.ShowLastActionsScreen(it).route)
                },
                navigateToOverview = { navController.navigate(ScreenNames.OVERVIEW.name) },
                screenSize = screenSize,
            )
        }

        composable(
            route = Screen.ShowLastActionsScreen("{${Screen.ShowLastActionsScreen.ID}}").route,
            arguments = listOf(
                navArgument(name = Screen.ShowLastActionsScreen.ID) {
                    type = NavType.StringType
                },
            )
        ) {
            ShowLastActionsScreen(
                navigateUp = { navController.navigateUp() },
                screenSize = screenSize,
            )
        }

        composable(route = ScreenNames.OVERVIEW.name) {
            OverviewScreen(
                navigateUp = { navController.navigate(Screen.StartScreen.route) },
                navigateToShowLastActions = {
                    navController.navigate(Screen.ShowLastActionsScreen(it).route)
                },
                screenSize = screenSize,
            )
        }

    }
}