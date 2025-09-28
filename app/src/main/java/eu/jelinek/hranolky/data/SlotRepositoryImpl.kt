import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import eu.jelinek.hranolky.data.LastModifiedSlots
import eu.jelinek.hranolky.data.SlotRepository
import eu.jelinek.hranolky.model.ActionType
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.SlotType
import eu.jelinek.hranolky.model.WarehouseSlot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SlotRepositoryImpl(private val firestoreDb: FirebaseFirestore) : SlotRepository {

    private val TAG = "SlotRepository"

    // --- Helper function for ID normalization ---
    private fun getNormalizedId(slotId: String): String {
        return if (slotId.length > 16 && slotId.startsWith('H') && slotId[1] == '-') {
            slotId.substring(2)
        } else {
            slotId
        }
    }

    override fun getSlot(slotId: String): Flow<WarehouseSlot?> = callbackFlow {
        Log.d(TAG, "getSlot: Attempting to fetch slot for original ID $slotId")
        var listenerRegistration: ListenerRegistration? = null
        var attemptedFallback = false

        fun fetchSlotInternal(idToFetch: String, isFallbackAttempt: Boolean) {
            Log.d(
                TAG,
                "getSlot: Fetching document with ID $idToFetch (isFallback: $isFallbackAttempt)"
            )
            listenerRegistration?.remove() // Remove previous listener if any

            listenerRegistration = firestoreDb.collection("WarehouseSlots")
                .document(idToFetch)
                .addSnapshotListener { slotSnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "getSlot (id: $idToFetch): Listen failed.", error)
                        // If primary fails, and we haven't tried fallback, try it.
                        // If fallback fails, then close with error.
                        if (!isFallbackAttempt && !attemptedFallback) {
                            val normalizedId = getNormalizedId(slotId)
                            if (normalizedId != slotId) {
                                Log.d(
                                    TAG,
                                    "getSlot: Primary fetch failed for $slotId, trying fallback $normalizedId due to error."
                                )
                                attemptedFallback = true
                                fetchSlotInternal(normalizedId, true)
                                return@addSnapshotListener
                            }
                        }
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
                        if (!isFallbackAttempt && !attemptedFallback) {
                            val normalizedId = getNormalizedId(slotId)
                            if (normalizedId != slotId) { // Only try fallback if ID actually changes
                                Log.d(
                                    TAG,
                                    "getSlot: Original ID $slotId not found, trying fallback $normalizedId"
                                )
                                attemptedFallback = true
                                fetchSlotInternal(normalizedId, true)
                            } else {
                                // Normalization didn't change ID, and it wasn't found
                                trySend(null).isSuccess
                            }
                        } else {
                            // Fallback ID also not found, or normalization didn't change ID
                            trySend(null).isSuccess
                        }
                    }
                }
        }

        fetchSlotInternal(slotId, false) // Start with original ID

        awaitClose {
            Log.d(TAG, "getSlot: Closing listener for initial ID $slotId")
            listenerRegistration?.remove()
        }
    }


    override fun getSlotActions(slotId: String): Flow<List<SlotAction>> = callbackFlow {
        Log.d(TAG, "getSlotActions: Attempting to fetch actions for original ID $slotId")
        var listenerRegistration: ListenerRegistration? = null
        var attemptedFallback = false

        fun fetchSlotActionsInternal(idToFetch: String, isFallbackAttempt: Boolean) {
            Log.d(
                TAG,
                "getSlotActions: Fetching actions for document ID $idToFetch (isFallback: $isFallbackAttempt)"
            )
            listenerRegistration?.remove()

            // First, check if the parent document exists. Firestore doesn't error if the subcollection
            // is queried on a non-existent document; it just returns an empty snapshot.
            // So, we need to handle the "document not found" case for fallbacks explicitly.
            firestoreDb.collection("WarehouseSlots").document(idToFetch).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (!documentSnapshot.exists()) {
                        Log.d(TAG, "getSlotActions (id: $idToFetch): Parent document not found.")
                        if (!isFallbackAttempt && !attemptedFallback) {
                            val normalizedId = getNormalizedId(slotId)
                            if (normalizedId != slotId) {
                                Log.d(
                                    TAG,
                                    "getSlotActions: Parent for $slotId not found, trying fallback $normalizedId"
                                )
                                attemptedFallback = true
                                fetchSlotActionsInternal(normalizedId, true)
                            } else {
                                trySend(emptyList()).isSuccess // Normalization didn't change ID
                            }
                        } else {
                            trySend(emptyList()).isSuccess // Fallback parent also not found
                        }
                        return@addOnSuccessListener
                    }

                    // Parent document exists, now listen for actions
                    listenerRegistration = firestoreDb.collection("WarehouseSlots")
                        .document(idToFetch)
                        .collection("SlotActions")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(10)
                        .addSnapshotListener { querySnapshot, error ->
                            if (error != null) {
                                Log.e(
                                    TAG,
                                    "getSlotActions (id: $idToFetch): Error receiving slot actions",
                                    error
                                )
                                // Similar to getSlot, if primary listener errors, try fallback
                                if (!isFallbackAttempt && !attemptedFallback) {
                                    val normalizedId = getNormalizedId(slotId)
                                    if (normalizedId != slotId) {
                                        Log.d(
                                            TAG,
                                            "getSlotActions: Listener error for $slotId, trying fallback $normalizedId."
                                        )
                                        attemptedFallback = true
                                        fetchSlotActionsInternal(normalizedId, true)
                                        return@addSnapshotListener
                                    }
                                }
                                close(error)
                                return@addSnapshotListener
                            }

                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                val lastActions = querySnapshot.documents.mapNotNull { document ->
                                    document.toObject(SlotAction::class.java)
                                }
                                Log.d(
                                    TAG,
                                    "getSlotActions (id: $idToFetch): Received ${lastActions.size} actions."
                                )
                                trySend(lastActions).isSuccess
                            } else {
                                Log.d(
                                    TAG,
                                    "getSlotActions (id: $idToFetch): No actions found or query snapshot empty."
                                )
                                trySend(emptyList()).isSuccess
                            }
                        }
                }
                .addOnFailureListener { error ->
                    Log.e(
                        TAG,
                        "getSlotActions (id: $idToFetch): Failed to check parent document existence.",
                        error
                    )
                    // If checking parent fails on primary, try fallback. If fallback check fails, close with error.
                    if (!isFallbackAttempt && !attemptedFallback) {
                        val normalizedId = getNormalizedId(slotId)
                        if (normalizedId != slotId) {
                            Log.d(
                                TAG,
                                "getSlotActions: Parent check failed for $slotId, trying fallback $normalizedId."
                            )
                            attemptedFallback = true
                            fetchSlotActionsInternal(normalizedId, true)
                            return@addOnFailureListener
                        }
                    }
                    close(error)
                }
        }

        fetchSlotActionsInternal(slotId, false) // Start with original ID

        awaitClose {
            Log.d(TAG, "getSlotActions: Closing listener for initial ID $slotId")
            listenerRegistration?.remove()
        }
    }


    override suspend fun addSlotAction(
        slotId: String,
        actionType: ActionType,
        quantity: Long,
        currentQuantity: Long,
        deviceId: String
    ) {
        val change = when (actionType) {
            ActionType.ADD -> quantity
            ActionType.REMOVE -> -quantity
            ActionType.INVENTORY_CHECK -> quantity - currentQuantity
        }

        val nextEst = currentQuantity + change
        if (nextEst < 0L) throw IllegalStateException("Quantity cannot go negative. current: $currentQuantity, change: $change")

        suspend fun tryAddActionToId(idToUse: String, isFallback: Boolean): Boolean {
            Log.d(TAG, "addSlotAction: Attempting on ID: $idToUse (isFallback: $isFallback)")
            val docRef = firestoreDb.collection("WarehouseSlots").document(idToUse)

            // First, check if the document exists before trying to update
            val docSnapshot = try {
                docRef.get().await()
            } catch (e: Exception) {
                Log.e(TAG, "addSlotAction: Error checking document existence for $idToUse", e)
                return false // Indicate failure to check, allowing fallback if applicable
            }

            if (!docSnapshot.exists()) {
                Log.w(TAG, "addSlotAction: Document $idToUse does not exist. Cannot add action.")
                return false // Indicate document not found, allowing fallback
            }

            // Document exists, proceed with adding action
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

            var success = true
            try {
                docRef.update(updateSlot).await()
                Log.d(TAG, "addSlotAction: Updated slot $idToUse (increment $change)")
            } catch (e: Exception) {
                Log.e(TAG, "addSlotAction: Error updating slot quantity for $idToUse", e)
                success = false // Mark as failed but continue to try adding action log
            }

            try {
                docRef.collection("SlotActions").add(slotAction).await()
                Log.d(TAG, "addSlotAction: Added slot action to $idToUse: $slotAction")
            } catch (e: Exception) {
                Log.e(TAG, "addSlotAction: Error logging slot action for $idToUse", e)
                success = false // The overall action wasn't fully successful
            }
            return success // Returns true if document existed and both operations were attempted (even if one failed)
            // More precisely, it returns true if the document existed, false otherwise.
            // The success of the individual operations is logged.
        }

        Log.d(TAG, "addSlotAction: Received request for original ID $slotId")
        if (tryAddActionToId(slotId, false)) {
            Log.d(TAG, "addSlotAction: Successfully processed or attempted on original ID $slotId.")
            return // Success or document found on original ID
        }

        // Original ID failed (likely document not found or error checking it)
        // Try normalized ID if different
        val normalizedId = getNormalizedId(slotId)
        if (normalizedId != slotId) {
            Log.d(
                TAG,
                "addSlotAction: Failed on original ID $slotId. Trying fallback ID $normalizedId."
            )
            if (tryAddActionToId(normalizedId, true)) {
                Log.d(
                    TAG,
                    "addSlotAction: Successfully processed or attempted on fallback ID $normalizedId."
                )
                return // Success or document found on fallback ID
            } else {
                Log.w(
                    TAG,
                    "addSlotAction: Fallback ID $normalizedId also failed or document not found."
                )
                // Optionally throw an exception here if both attempts fail and the document must exist
                // throw Exception("Failed to add slot action to $slotId or $normalizedId. Document not found or error.")
            }
        } else {
            Log.w(
                TAG,
                "addSlotAction: Original ID $slotId failed, and normalization yielded the same ID. No fallback attempt."
            )
            // Optionally throw an exception here
            // throw Exception("Failed to add slot action to $slotId. Document not found or error.")
        }
    }

    override suspend fun createNewSlot(slotId: String, quantity: Long) {
        val slot = WarehouseSlot(productId = slotId, quantity = quantity)
            .parsePropertiesFromProductId()

        if (!slot.hasAllProperties()) {
            Log.w(
                TAG,
                "createNewSlot: Cannot create slot $slotId, missing properties after parsing."
            )
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
                    Log.d(
                        TAG,
                        "createNewSlot: Slot ${slot.productId} already exists. No action taken."
                    )
                    null
                } else {
                    // Document does not exist, create it
                    transaction.set(docRef, data)
                    Log.d(TAG, "createNewSlot: Creating new slot with ID: ${slot.productId}")
                    null
                }
            }.await() // await the completion of the transaction
            Log.d(TAG, "createNewSlot: Transaction for creating slot ${slot.productId} completed.")

        } catch (e: Exception) {
            Log.e(TAG, "createNewSlot: Error or conflict creating slot ${slot.productId}", e)
        }
    }


    override fun getLastModifiedSlots(): Flow<LastModifiedSlots> = callbackFlow {
        val listener = firestoreDb.collection("WarehouseSlots")
            .orderBy("lastModified", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(
                        "Firestore",
                        "getLastModifiedSlots: Error receiving last slots from Firestore",
                        error
                    )
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastModifiedBeamSlots = querySnapshot.documents.filter { document ->
                        !document.id.startsWith("S")
                    }.mapNotNull { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(document.id)
                    }
                    val lastModifiedJointerSlots = querySnapshot.documents.filter { document ->
                        document.id.startsWith("S")
                    }.mapNotNull { document ->
                        val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(document.id)
                    }
                    trySend(
                        LastModifiedSlots(
                            lastModifiedBeamSlots,
                            lastModifiedJointerSlots
                        )
                    ).isSuccess
                } else {
                    trySend(LastModifiedSlots.EMPTY).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }


    override fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>> = callbackFlow {
        var query: Query = firestoreDb.collection("WarehouseSlots")

        if (slotType == SlotType.Jointer) {
            query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), "S")
                .whereLessThan(FieldPath.documentId(), "T")
        } else if (slotType == SlotType.Beam) {
            query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), "D")
                .whereLessThan(FieldPath.documentId(), "I")
        }

        val listener = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                Log.e("Firestore", "getAllSlots: Error receiving all slots from Firestore", error)
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