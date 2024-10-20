package eu.jelinek.hranolky.ui.shared

enum class ScreenSize {
    PHONE, TABLET;

    fun isTablet() = this == TABLET
    fun isPhone() = this == PHONE

}