package eu.jelinek.hranolky.ui.showlast

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import eu.jelinek.hranolky.model.Quantity
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShowLastActionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val firestoreDb: FirebaseFirestore,
) : ViewModel() {

    val slotId: String? = savedStateHandle[Screen.ShowLastActionsScreen.ID]
    private val _screenStateStream =
        MutableStateFlow<ShowLastActionsScreenState>(ShowLastActionsScreenState())
    val screenStateStream get() = _screenStateStream.asStateFlow()

    val TAG = "Firestore"

    init {
        viewModelScope.launch {
            var slot = fetchSlotFromFirestore()

            if (slot != null) {
                val quality = slot.productId.take(5)
                val parts = slot.productId.split("-")
                val thickness = parts[2].toInt()
                val width = parts[3].toInt()
                val length = parts[4].toInt()

                slot = slot.copy(
                    quality = quality,
                    thickness = thickness,
                    width = width,
                    length = length,
                )
            }

            _screenStateStream.update {
                it.copy(
                    slot = slot
                )
            }
        }
    }

    suspend fun fetchSlotFromFirestore(): WarehouseSlot? {
        try {
            val documentSnapshot = firestoreDb.collection("WarehouseSlots")
                .document(slotId!!) // Specify the document ID
                .get()
                .await()

            if (documentSnapshot.exists()) {
                Log.d(TAG, "DocumentSnapshot data: ${documentSnapshot.data}")
                val warehouseQuantity =
                    documentSnapshot.toObject(Quantity::class.java) // Convert to your data class
                return WarehouseSlot(
                    productId = slotId,
                    quantity = warehouseQuantity?.quantity ?: 0,
                )
            } else {
                Log.w(TAG, "Document not found")
                sendNewSlotToFirestore(0)
                return null // Document not found
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting document.", exception)
            return null
        }
    }

    fun fetchAllDocumentsFromFirestore() {
        viewModelScope.launch {
            firestoreDb.collection("WarehouseSlots")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        Log.d(TAG, "${document.id} => ${document.data}")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents.", exception)
                }
        }
    }

    fun sendNewSlotToFirestore(quantity: Int) {
        // Create a new user with a first and last name
        val slot = WarehouseSlot(
            productId = slotId!!,
            //productId = "DUB-A-20-42-0480",
            quantity = quantity,
            /*quality = slotId!!.take(5),
            thickness = 20,
            width = 42,
            length = 480,*/
        )

        val slotToSend = hashMapOf(
            "quantity" to slot.quantity,
        )

        Log.d(TAG, "Sending data to Firestore: $slotToSend")

        viewModelScope.launch {
            try {
                firestoreDb.collection("WarehouseSlots")
                    .document(slot.productId)
                    .set(slotToSend)
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot added with ID: ${slot.productId}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending data to Firestore", e)
            }
        }
    }


}

data class ShowLastActionsScreenState(
    val slot: WarehouseSlot? = null,
    val loading: Boolean = false,
    val error: String? = null,
)