package eu.jelinek.hranolky.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import eu.jelinek.hranolky.ui.history.HistoryScreen
import eu.jelinek.hranolky.ui.manageitem.ManageItemScreen
import eu.jelinek.hranolky.ui.overview.OverviewScreen
import eu.jelinek.hranolky.ui.shared.ScreenSize
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
                navigateToManageItem = {
                    navController.navigate(Screen.ManageItemScreen(it).route)
                },
                navigateToOverview = { navController.navigate(ScreenNames.OVERVIEW.name) },
                navigateToHistory = { navController.navigate(ScreenNames.HISTORY.name) },
                screenSize = screenSize,
            )
        }

        composable(
            route = Screen.ManageItemScreen("{${Screen.ManageItemScreen.ID}}").route,
            arguments = listOf(
                navArgument(name = Screen.ManageItemScreen.ID) {
                    type = NavType.StringType
                },
            )
        ) {
            ManageItemScreen(
                navigateUp = { navController.navigateUp() },
                screenSize = screenSize,
            )
        }

        composable(route = ScreenNames.OVERVIEW.name) {
            OverviewScreen(
                navigateUp = { navController.navigate(Screen.StartScreen.route) },
                navigateToManageItemScreen = {
                    navController.navigate(Screen.ManageItemScreen(it).route)
                },
                screenSize = screenSize,
            )
        }

        composable(route = ScreenNames.HISTORY.name) {
            HistoryScreen(
                navigateUp = { navController.navigate(Screen.StartScreen.route) },
                navigateToManageItem = {
                    navController.navigate(Screen.ManageItemScreen(it).route)
                },
                screenSize = screenSize,
            )
        }

    }
}