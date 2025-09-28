package eu.jelinek.hranolky.model

enum class ActionType {
    ADD, REMOVE, INVENTORY_CHECK;

    override fun toString(): String {
        return when (this) {
            ADD -> "prijem"
            REMOVE -> "vydej"
            INVENTORY_CHECK -> "inventura"
        }
    }
}