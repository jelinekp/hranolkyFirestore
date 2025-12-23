package eu.jelinek.hranolky.model

data class SlotAction(
    val documentId: String? = null, // Firestore document ID for undo functionality
    val action: String? = null,
    val quantityChange: Int = 0,
    val newQuantity: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)
