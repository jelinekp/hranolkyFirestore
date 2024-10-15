package eu.jelinek.hranolky.model

data class SlotAction(
    var action: String? = null,
    var quantityChange: Int = 0,
    var newQuantity: Int = 0,
    var timestamp: com.google.firebase.Timestamp? = null
)
