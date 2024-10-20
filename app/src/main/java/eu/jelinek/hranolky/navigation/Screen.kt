package eu.jelinek.hranolky.navigation

sealed class Screen(val route: String) {

    data object StartScreen: Screen(ScreenNames.START_SCREEN.name)

    class ShowLastActionsScreen(slotId: String): Screen("${ScreenNames.SHOW_LAST_ACTIONS}/$slotId") {
        companion object {
            const val ID = "id"
        }
    }
}
