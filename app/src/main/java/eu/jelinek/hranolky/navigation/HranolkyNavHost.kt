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
import eu.jelinek.hranolky.ui.AddActionScreen
import eu.jelinek.hranolky.ui.ResultScreen
import eu.jelinek.hranolky.ui.start.StartScreen
import eu.jelinek.hranolky.ui.showlast.ShowLastActionsScreen

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
        composable(Screen.StartScreen.route) {
            StartScreen(
                navigateToShowLastActions = {
                    Log.d("Navigation", it)
                    navController.navigate(Screen.ShowLastActionsScreen(it).route) }
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

    }
}