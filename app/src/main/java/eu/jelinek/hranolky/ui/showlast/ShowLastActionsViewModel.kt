package eu.jelinek.hranolky.ui.showlast

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen

class ShowLastActionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val firestoreDb: FirebaseFirestore,
) : ViewModel() {

    val slotId: String? = savedStateHandle[Screen.ShowLastActionsScreen.ID]
    val TAG = "Firestore"

    fun fetchLastActionsFromFirestore() {
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

    fun sendNewSlotToFirestore() {
        // Create a new user with a first and last name
        val slot = WarehouseSlot(
            productId = slotId!!,
            //productId = "DUB-A-20-42-0480",
            quantity = 630,
            /*quality = slotId!!.take(5),
            thickness = 20,
            width = 42,
            length = 480,*/
        )

        val slotToSend = hashMapOf(
            "quantity" to slot.quantity,
        )

        Log.d(TAG, "Sending data to Firestore: $slotToSend")

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

