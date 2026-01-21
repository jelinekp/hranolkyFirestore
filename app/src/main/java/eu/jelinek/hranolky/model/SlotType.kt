package eu.jelinek.hranolky.model

import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.config.FirestoreConfig

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

    fun toFirestoreCollectionName(): String {
        return when (this) {
            Beam -> FirestoreConfig.Collections.BEAMS
            Jointer -> FirestoreConfig.Collections.JOINTERS
        }
    }

    fun icon() : Int {
        return when (this) {
            Beam -> R.drawable.ic_launcher_foreground
            Jointer -> R.drawable.sparovky_foreground
        }
    }

    fun smallIcon() : Int {
        return when (this) {
            Beam -> R.drawable.ic_launcher_foreground
            Jointer -> R.drawable.jointer
        }
    }
}