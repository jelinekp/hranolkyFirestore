package eu.jelinek.hranolky.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
        Log.d(TAG, "getSlot: Fetching slot $slotId")

        var listenerRegistration: ListenerRegistration? = null

        fun fetchActualSlot(idToFetch: String, isFallback: Boolean) {
            // Remove previous listener if any (important for fallback)
            listenerRegistration?.remove()

            listenerRegistration = firestoreDb.collection("WarehouseSlots")
                .document(idToFetch)
                .addSnapshotListener { slotSnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "getSlot (id: $idToFetch): Listen to slot download failed", error)
                        // If it's a fallback attempt and it fails, we might still want to signal null
                        // or close with error depending on desired behavior.
                        // For now, if any fetch attempt errors out, we close the flow.
                        close(error)
                        return@addSnapshotListener
                    }

                    if (slotSnapshot != null && slotSnapshot.exists()) {
                        val firestoreSlot = slotSnapshot.toObject(FirestoreSlot::class.java)
                        val slot = firestoreSlot?.toWarehouseSlot(idToFetch)
                        Log.d(TAG, "getSlot (id: $idToFetch): Received slot $slot")
                        trySend(slot).isSuccess
                    } else {
                        Log.d(TAG, "getSlot (id: $idToFetch): Slot not found.")
                        if (!isFallback) {
                            // Original ID not found, try the normalized ID
                            val normalizedId = if (slotId.length > 16 && slotId.startsWith('H') && slotId[1] == '-') {
                                slotId.substring(2)
                            } else {
                                // If normalization doesn't change the ID, and it wasn't found,
                                // we've already tried, so send null.
                                slotId
                            }

                            if (normalizedId != idToFetch) { // Important: Only try fallback if ID is different
                                Log.d(TAG, "getSlot: Original ID $slotId not found, trying fallback $normalizedId")
                                fetchActualSlot(normalizedId, true)
                            } else {
                                // Normalization didn't change ID, or it's already the fallback, and it wasn't found
                                trySend(null).isSuccess
                            }
                        } else {
                            // Fallback ID also not found
                            Log.d(TAG, "getSlot: Fallback ID $idToFetch also not found.")
                            trySend(null).isSuccess
                        }
                    }
                }
        }

        // Start fetching with the original slotId
        fetchActualSlot(slotId, false)

        awaitClose {
            Log.d(TAG, "getSlot: Closing listener for $slotId")
            listenerRegistration?.remove()
        }
    }

    override fun getSlotActions(slotId: String): Flow<List<SlotAction>> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .document(slotId)
            .collection("SlotActions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getSlotActions: Error receiving last slot actions from Firestore", error)
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

        val slotAction = hashMapOf(
            "action" to actionType.toString(),
            "userId" to deviceId,
            "quantityChange" to change,
            "newQuantity" to nextEst,
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
            Log.d(TAG, "addSlotAction: Added slot action to $slotId: $slotAction")
        } catch (e: Exception) {
            Log.e(TAG, "addSlotAction: Error logging slot action", e)
        }
    }

    override suspend fun createNewSlot(slotId: String, quantity: Long) {
        val slot = WarehouseSlot(productId = slotId, quantity = quantity)
            .parsePropertiesFromProductId()

        if (!slot.hasAllProperties()) {
            Log.w(TAG, "createNewSlot: Cannot create slot $slotId, missing properties after parsing.")
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
                    Log.d(TAG, "createNewSlot: Slot ${slot.productId} already exists. No action taken.")
                    // You might want to return something from the transaction to indicate success/failure/already_exists
                    // For now, null implies success of the transaction itself.
                    null
                } else {
                    // Document does not exist, create it
                    transaction.set(docRef, data)
                    Log.d(TAG, "createNewSlot: Creating new slot with ID: ${slot.productId}")
                    // Return something if needed, e.g., true for success
                    null
                }
            }.await() // await the completion of the transaction
            Log.d(TAG, "createNewSlot: Transaction for creating slot ${slot.productId} completed.")

        } catch (e: Exception) {
            // This catch block will handle exceptions during the transaction itself,
            // including if the transaction fails after multiple retries.
            Log.e(TAG, "createNewSlot: Error or conflict creating slot ${slot.productId}", e)
        }
    }



    override fun getLastModifiedSlots(): Flow<LastModifiedSlots> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "getLastModifiedSlots: Error receiving last slots from Firestore", error)
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
        } else if (slotType == SlotType.Beam) {
            // for items starting with "D" to "H" -- all beams
            query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), "D")
                .whereLessThan(
                    FieldPath.documentId(),
                    "I"
                )
        }

        val listener = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("Firestore", "getAllSlots: Error receiving all slots from Firestore", error)
                close(error)
                return@addSnapshotListener
            }

            if (querySnapshot != null && !querySnapshot.isEmpty) {
                var allSlots = querySnapshot.documents.mapNotNull {
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