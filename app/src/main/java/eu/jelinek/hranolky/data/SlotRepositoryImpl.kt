package eu.jelinek.hranolky.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.ui.manageitem.ActionType
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
        currentQuantity: Long,            // ← Long
        deviceId: String
    ) {
        val change = when (actionType) {
            ActionType.ADD -> quantity
            ActionType.REMOVE -> -quantity
        }

        val nextEst = currentQuantity + change
        if (nextEst < 0L) throw IllegalStateException("Quantity cannot go negative")

        val docRef = firestoreDb.collection("WarehouseSlots").document(slotId)

        // 1) Stav skladu – výhradně increment + serverový timestamp
        val updateSlot = mapOf(
            "quantity" to FieldValue.increment(change),
            "lastModified" to FieldValue.serverTimestamp(),
        )

        // 2) Log akce – "newQuantityClientEst" jen jako klientský odhad
        val slotAction = hashMapOf(
            "action" to actionType.toString(),
            "userId" to deviceId,
            "quantityChange" to change,
            "newQuantityClientEst" to nextEst, // ← přejmenováno, aby bylo jasné, že je to odhad
            "timestamp" to FieldValue.serverTimestamp(),
        )

        try {
            docRef.update(updateSlot).await()
            Log.d(TAG, "Updated slot $slotId (increment $change)")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating slot quantity", e)
        }

        try {
            docRef.collection("SlotActions").add(slotAction).await()
            Log.d(TAG, "Added slot action to $slotId: $slotAction")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging slot action", e)
        }
    }

    override suspend fun createNewSlot(slotId: String, quantity: Long) {
        val slot = WarehouseSlot(productId = slotId, quantity = quantity)
            .parsePropertiesFromProductId()

        if (!slot.hasAllProperties()) {
            Log.w(TAG, "Cannot create slot $slotId, missing properties after parsing.")
            return
        }

        val docRef = firestoreDb.collection("WarehouseSlots").document(slot.productId)
        val data = hashMapOf(
            "quantity" to slot.quantity,
            "lastModified" to FieldValue.serverTimestamp(),
            // Add other static fields from the 'slot' object if necessary, e.g.,
            // "propertyX" to slot.propertyX
            // Ensure these are fields you want to initialize at creation.
        )

        try {
            firestoreDb.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    // Document already exists, do nothing as per your requirement
                    Log.d(TAG, "Slot ${slot.productId} already exists. No action taken.")
                    // You might want to return something from the transaction to indicate success/failure/already_exists
                    // For now, null implies success of the transaction itself.
                    null
                } else {
                    // Document does not exist, create it
                    transaction.set(docRef, data)
                    Log.d(TAG, "Creating new slot with ID: ${slot.productId}")
                    // Return something if needed, e.g., true for success
                    null
                }
            }.await() // await the completion of the transaction
            Log.d(TAG, "Transaction for creating slot ${slot.productId} completed.")

        } catch (e: Exception) {
            // This catch block will handle exceptions during the transaction itself,
            // including if the transaction fails after multiple retries.
            Log.e(TAG, "Error or conflict creating slot ${slot.productId}", e)
        }
    }



    override fun getLastModifiedSlots(): Flow<LastModifiedSlots> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error receiving last slots from Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastModifiedBeamSlots = querySnapshot.documents.filter { document ->
                        // Filter: only include documents whose ID does NOT start with "S"
                        !document.id.startsWith("S")
                    }.mapNotNull { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(document.id)
                    }
                    val lastModifiedJointerSlots = querySnapshot.documents.filter { document ->
                        // Filter: only include documents whose ID starts with "S"
                        document.id.startsWith("S")
                    }.mapNotNull { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(document.id)
                    }
                    trySend(LastModifiedSlots(lastModifiedBeamSlots, lastModifiedJointerSlots)).isSuccess
                } else {
                    trySend(LastModifiedSlots.EMPTY).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }


    override fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>> = callbackFlow {
        var query: Query = firestoreDb.collection("WarehouseSlots")

        // Example based on ID prefix, assuming:
        // SlotType.JOINTER means ID starts with "S"
        // SlotType.BEAM means ID does NOT start with "S" (requires client-side filtering)

        // Note: If SlotType.BEAM needs server-side filtering, you'd need a different data model
        // (like a boolean field `isJointerType`) for efficient server-side "not S" filtering.

        if (slotType == SlotType.Jointer) {
            // Query for documents where ID starts with "S"
            // This range query works if your IDs are structured like Sxxxx
            query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), "S")
                .whereLessThan(
                    FieldPath.documentId(),
                    "T"
                ) // Assumes no valid IDs start with T, U, V etc. that are > "S" but not "Sxxxx"
        }
        // No direct server-side query for "ID does NOT start with S" for SlotType.BEAM
        // It would require client-side filtering if you don't add another field to your documents.

        val listener = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error receiving all slots from Firestore", error)
                close(error)
                return@addSnapshotListener
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                var allSlots = querySnapshot.documents.mapNotNull {
                    val firestoreSlot = it.toObject(FirestoreSlot::class.java)
                    firestoreSlot?.toWarehouseSlot(it.id)
                }

                // Client-side filtering if needed (e.g., for BEAM type)
                if (slotType == SlotType.Beam) {
                    allSlots = allSlots.filter { warehouseSlot ->
                        !warehouseSlot.productId.startsWith("S")
                    }
                }

                trySend(allSlots).isSuccess
            } else {
                trySend(emptyList()).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }
}