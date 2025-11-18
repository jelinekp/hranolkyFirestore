package eu.jelinek.hranolky.navigation

sealed class Screen(val route: String) {

    data object StartScreen: Screen(ScreenNames.START_SCREEN.name)

    class ManageItemScreen(slotId: String): Screen("${ScreenNames.MANAGE_ITEM_SCREEN}/$slotId") {
        companion object {
            const val ID = "id"
        }
    }
}
