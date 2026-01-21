
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import eu.jelinek.hranolky.config.FirestoreConfig
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

    private fun getNormalizedIdAndSlotType(slotId: String): Pair<String, SlotType> {
        return if (slotId.length > 16 && (slotId.startsWith('S') || slotId.startsWith('H')) && slotId[1] == '-') {
            val normalizedId = slotId.substring(2)
            val slotType = if (slotId.startsWith('S')) SlotType.Jointer else SlotType.Beam
            Pair(normalizedId, slotType)
        } else {
            Pair(slotId, SlotType.Beam)
        }
    }

    override fun getSlot(fullSlotId: String): Flow<WarehouseSlot?> = callbackFlow {
        val (normalizedId, slotType) = getNormalizedIdAndSlotType(fullSlotId)
        val collectionName = slotType.toFirestoreCollectionName()

        Log.d(TAG, "getSlot: Fetching slot $normalizedId from $collectionName")

        val listenerRegistration = firestoreDb.collection(collectionName)
            .document(normalizedId)
            .addSnapshotListener { slotSnapshot, error ->
                if (error != null) {
                    Log.w(TAG, "getSlot ($normalizedId): Listen failed.", error)
                    return@addSnapshotListener
                }

                if (slotSnapshot != null && slotSnapshot.exists()) {
                    val firestoreSlot = slotSnapshot.toObject(FirestoreSlot::class.java)
                    val slot = firestoreSlot?.toWarehouseSlot(slotType, normalizedId)
                    Log.d(TAG, "getSlot ($normalizedId): Received slot $slot")
                    trySend(slot).isSuccess
                } else {
                    Log.d(TAG, "getSlot ($normalizedId): Slot not found")
                    trySend(null).isSuccess
                }
            }

        awaitClose {
            Log.d(TAG, "getSlot: Closing listener for $normalizedId")
            listenerRegistration.remove()
        }
    }


    override fun getSlotActions(fullSlotId: String): Flow<List<SlotAction>> = callbackFlow {
        val (normalizedId, slotType) = getNormalizedIdAndSlotType(fullSlotId)
        val collectionName = slotType.toFirestoreCollectionName()

        Log.d(TAG, "getSlotActions: Fetching actions for $normalizedId from $collectionName")

        val listenerRegistration = firestoreDb.collection(collectionName)
            .document(normalizedId)
            .collection(FirestoreConfig.Subcollections.SLOT_ACTIONS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getSlotActions ($normalizedId): Error receiving slot actions", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastActions = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(SlotAction::class.java)?.copy(documentId = document.id)
                    }
                    Log.d(TAG, "getSlotActions ($normalizedId): Received ${lastActions.size} actions")
                    trySend(lastActions).isSuccess
                } else {
                    Log.d(TAG, "getSlotActions ($normalizedId): No actions found")
                    trySend(emptyList()).isSuccess
                }
            }

        awaitClose {
            Log.d(TAG, "getSlotActions: Closing listener for $normalizedId")
            listenerRegistration.remove()
        }
    }


    override suspend fun addSlotAction(
        fullSlotId: String,
        actionType: ActionType,
        quantity: Long,
        currentQuantity: Long,
        deviceId: String
    ): String {
        val change = when (actionType) {
            ActionType.ADD -> quantity
            ActionType.REMOVE -> -quantity
            ActionType.INVENTORY_CHECK -> quantity - currentQuantity
        }

        val nextEst = currentQuantity + change
        if (nextEst < 0L) {
            throw IllegalStateException("Quantity cannot go negative. current: $currentQuantity, change: $change")
        }

        val (normalizedId, slotType) = getNormalizedIdAndSlotType(fullSlotId)
        val collectionName = slotType.toFirestoreCollectionName()

        Log.d(TAG, "addSlotAction: Processing action for $normalizedId in $collectionName")

        val docRef = firestoreDb.collection(collectionName).document(normalizedId)

        // Check if document exists
        val docSnapshot = try {
            docRef.get().await()
        } catch (e: Exception) {
            Log.e(TAG, "addSlotAction: Error checking document existence for $normalizedId", e)
            throw e
        }

        if (!docSnapshot.exists()) {
            Log.w(TAG, "addSlotAction: Document $normalizedId does not exist")
            throw IllegalStateException("Slot $normalizedId does not exist")
        }

        // Update slot quantity
        val updateSlot = mapOf(
            "quantity" to FieldValue.increment(change),
            "lastModified" to FieldValue.serverTimestamp(),
        )

        try {
            docRef.update(updateSlot).await()
            Log.d(TAG, "addSlotAction: Updated slot $normalizedId (increment $change)")
        } catch (e: Exception) {
            Log.e(TAG, "addSlotAction: Error updating slot quantity for $normalizedId", e)
            throw e
        }

        // Add action to subcollection
        val slotAction = hashMapOf(
            "action" to actionType.toString(),
            "userId" to deviceId,
            "quantityChange" to change,
            "newQuantity" to nextEst,
            "timestamp" to FieldValue.serverTimestamp(),
        )

        val actionDocRef = try {
            docRef.collection(FirestoreConfig.Subcollections.SLOT_ACTIONS).add(slotAction).await()
        } catch (e: Exception) {
            Log.e(TAG, "addSlotAction: Error logging slot action for $normalizedId", e)
            throw e
        }

        Log.d(TAG, "addSlotAction: Added slot action to $normalizedId with ID ${actionDocRef.id}")
        return actionDocRef.id
    }

    override suspend fun undoSlotAction(
        fullSlotId: String,
        actionDocumentId: String,
        quantityChange: Long
    ) {
        val (normalizedId, slotType) = getNormalizedIdAndSlotType(fullSlotId)
        val collectionName = slotType.toFirestoreCollectionName()

        Log.d(TAG, "undoSlotAction: Undoing action $actionDocumentId for $normalizedId in $collectionName")

        val docRef = firestoreDb.collection(collectionName).document(normalizedId)
        val actionDocRef = docRef.collection(FirestoreConfig.Subcollections.SLOT_ACTIONS).document(actionDocumentId)

        try {
            firestoreDb.runTransaction { transaction ->
                // Reverse the quantity change
                val reverseChange = -quantityChange
                transaction.update(docRef, mapOf(
                    "quantity" to FieldValue.increment(reverseChange),
                    "lastModified" to FieldValue.serverTimestamp()
                ))

                // Delete the action document
                transaction.delete(actionDocRef)

                Log.d(TAG, "undoSlotAction: Transaction prepared - reversing $quantityChange, deleting $actionDocumentId")
                null
            }.await()

            Log.d(TAG, "undoSlotAction: Successfully undid action $actionDocumentId for $normalizedId")
        } catch (e: Exception) {
            Log.e(TAG, "undoSlotAction: Error undoing action $actionDocumentId for $normalizedId", e)
            throw e
        }
    }

    override suspend fun createNewSlot(fullSlotId: String, quantity: Long) {
        val slot = WarehouseSlot(fullProductId = fullSlotId, quantity = quantity)
            .parsePropertiesFromProductId()

        if (!slot.hasAllProperties()) {
            Log.w(
                TAG,
                "createNewSlot: Cannot create slot $fullSlotId, missing properties after parsing."
            )
            return
        }

        val collectionName = slot.slotType?.toFirestoreCollectionName()

        if (collectionName == null) {
            Log.w(TAG, "createNewSlot: Cannot create slot $fullSlotId, unknown slot type.")
            return
        }

        val documentPath = if (slot.fullProductId[1] == '-') slot.fullProductId.substring(2) else null

        if (documentPath == null) {
            Log.w(TAG, "createNewSlot: Cannot create slot $fullSlotId, wrong document path.")
            return
        }

        val docRef = firestoreDb.collection(collectionName).document(documentPath)
        val data = hashMapOf(
            "quantity" to slot.quantity,
            "lastModified" to FieldValue.serverTimestamp(),
            "quality" to slot.quality,
            "thickness" to slot.thickness,
            "width" to slot.width,
            "length" to slot.length,
        )

        try {
            firestoreDb.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    // Document already exists, do nothing as per your requirement
                    Log.d(
                        TAG,
                        "createNewSlot: Slot ${slot.slotType} ${slot.fullProductId} already exists. No action taken."
                    )
                    null
                } else {
                    // Document does not exist, create it
                    transaction.set(docRef, data)
                    Log.d(
                        TAG,
                        "createNewSlot: Creating new slot with ID: ${slot.slotType} ${slot.fullProductId}"
                    )
                    null
                }
            }.await() // await the completion of the transaction
            Log.d(TAG, "createNewSlot: Transaction for creating slot ${slot.fullProductId} completed.")

        } catch (e: Exception) {
            Log.e(TAG, "createNewSlot: Error or conflict creating slot ${slot.fullProductId}", e)
        }
    }


    override fun getLastModifiedSlots(slotType: SlotType): Flow<List<WarehouseSlot>> =
        callbackFlow {
            val collectionName = slotType.toFirestoreCollectionName()

            val listener = firestoreDb.collection(collectionName)
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "getLastModifiedSlots: Error receiving slots from Firestore", error)
                        close(error)
                        return@addSnapshotListener
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        val lastModifiedSlots = querySnapshot.documents.mapNotNull { document ->
                            val firestoreSlot = document.toObject(FirestoreSlot::class.java)
                            firestoreSlot?.toWarehouseSlot(slotType, document.id)
                        }
                        trySend(lastModifiedSlots).isSuccess
                    } else {
                        trySend(emptyList()).isSuccess
                    }
                }
            awaitClose { listener.remove() }
        }


    override fun getAllSlots(slotType: SlotType): Flow<List<WarehouseSlot>> = callbackFlow {
        val collectionName = slotType.toFirestoreCollectionName()

        val listener = firestoreDb.collection(collectionName)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "getAllSlots: Error receiving $slotType slots from Firestore", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val allSlots = querySnapshot.documents.mapNotNull {
                        val firestoreSlot = it.toObject(FirestoreSlot::class.java)
                        firestoreSlot?.toWarehouseSlot(slotType, it.id)
                    }
                    trySend(allSlots).isSuccess
                } else {
                    trySend(emptyList()).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
}