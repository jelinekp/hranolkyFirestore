package eu.jelinek.hranolky.model

import eu.jelinek.hranolky.R

enum class SlotType {
    Beam, Jointer;

    override fun toString(): String {
        return when (this) {
            Beam -> "H"
            Jointer -> "S"
        }
    }

    fun toLongName(): String {
        return when (this) {
            Beam -> "Hranolky"
            Jointer -> "Spárovky"
        }
    }

    fun icon() : Int {
        return when (this) {
            Beam -> R.drawable.ic_launcher_foreground
            Jointer -> R.drawable.jointer
        }
    }
}