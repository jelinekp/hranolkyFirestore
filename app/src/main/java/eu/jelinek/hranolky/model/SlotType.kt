package eu.jelinek.hranolky.model

enum class SlotType {
    Beam, Jointer;

    override fun toString(): String {
        return when (this) {
            Beam -> "H"
            Jointer -> "S"
        }
    }
}