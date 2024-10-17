package eu.jelinek.hranolky.model

data class SlotAction(
    val action: String? = null,
    val quantityChange: Int = 0,
    val newQuantity: Int = 0,
    val timestamp: com.google.firebase.Timestamp? = null
)
