package eu.jelinek.hranolky.data

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.showlast.ActionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SlotRepositoryImpl(private val firestoreDb: FirebaseFirestore) : SlotRepository {

    private val TAG = "SlotRepository"

    override fun getSlot(slotId: String): Flow<WarehouseSlot?> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .document(slotId)
            .addSnapshotListener { slotSnapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen to slot download failed", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (slotSnapshot != null && slotSnapshot.exists()) {
                    val firestoreSlot = slotSnapshot.toObject(FirestoreSlot::class.java)
                    val slot = firestoreSlot?.toWarehouseSlot(slotId)
                    trySend(slot).isSuccess
                } else {
                    trySend(null).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getSlotActions(slotId: String): Flow<List<SlotAction>> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .document(slotId)
            .collection("SlotActions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error receiving last slot actions from Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastActions = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(SlotAction::class.java)
                    }
                    trySend(lastActions).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addSlotAction(
        slotId: String,
        actionType: ActionType,
        quantity: Long,
        currentQuantity: Int,
        deviceId: String
    ) {
        val quantityChange = when (actionType) {
            ActionType.ADD -> quantity
            ActionType.REMOVE -> -quantity
        }

        val updateSlot = mapOf(
            "quantity" to FieldValue.increment(quantityChange),
            "lastModified" to FieldValue.serverTimestamp(),
        )

        val slotAction = hashMapOf(
            "action" to actionType.toString(),
            "userId" to deviceId,
            "quantityChange" to quantityChange,
            "newQuantity" to (currentQuantity + quantityChange),
            "timestamp" to FieldValue.serverTimestamp(),
        )

        try {
            firestoreDb.collection("WarehouseSlots")
                .document(slotId)
                .update(updateSlot)
                .await()
            Log.d(TAG, "Updated slot with ID: $slotId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating data in Firestore", e)
        }

        try {
            firestoreDb.collection("WarehouseSlots")
                .document(slotId)
                .collection("SlotActions")
                .add(slotAction)
                .await()
            Log.d(TAG, "Added slot action to slot $slotId with: $slotAction")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending slot Action to Firestore", e)
        }
    }

    override suspend fun createNewSlot(slotId: String, quantity: Int) {
        val slot = WarehouseSlot(productId = slotId, quantity = quantity).parsePropertiesFromProductId()

        if (slot.hasAllProperties()) {
            val slotToSend = hashMapOf(
                "quantity" to slot.quantity,
                "lastModified" to FieldValue.serverTimestamp(),
            )

            Log.d(TAG, "Sending data to Firestore: $slotToSend")

            try {
                firestoreDb.collection("WarehouseSlots")
                    .document(slot.productId)
                    .set(slotToSend, SetOptions.merge())
                    .await()
                Log.d(TAG, "DocumentSnapshot added with ID: ${slot.productId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending data to Firestore", e)
            }
        }
    }

    override fun getLastModifiedSlots(): Flow<List<WarehouseSlot>> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving last slots from Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastModifiedSlots = querySnapshot.documents.mapNotNull { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(document.id)
                    }
                    trySend(lastModifiedSlots).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getAllSlots(): Flow<List<WarehouseSlot>> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving all slots from Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val allSlots = querySnapshot.documents.mapNotNull {
                        val firestoreSlot = it.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(it.id)
                    }
                    trySend(allSlots).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
}