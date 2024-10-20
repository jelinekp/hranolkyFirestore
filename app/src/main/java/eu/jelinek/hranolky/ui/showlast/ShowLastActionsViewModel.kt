package eu.jelinek.hranolky.ui.showlast

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import eu.jelinek.hranolky.model.FirestoreSlot
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.model.WarehouseSlot
import eu.jelinek.hranolky.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShowLastActionsViewModel(
    savedStateHandle: SavedStateHandle,
    private val firestoreDb: FirebaseFirestore,
) : ViewModel() {

    val slotId: String? = savedStateHandle[Screen.ShowLastActionsScreen.ID]
    private val _screenStateStream =
        MutableStateFlow<ShowLastActionsScreenState>(ShowLastActionsScreenState())
    val screenStateStream get() = _screenStateStream.asStateFlow()

    private val _validationSharedFlowStream = MutableSharedFlow<AddActionValidationState>()
    val validationSharedFlowStream get() = _validationSharedFlowStream.asSharedFlow()

    var quantityState = mutableStateOf("")
        private set

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
    }

    val TAG = "Firestore"

    private var slotListener: ListenerRegistration? = null
    private var actionsListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            fetchSlotFromFirestoreInRealTime()
        }
    }

    private fun updateSlot(slot: WarehouseSlot?, slotActions: List<SlotAction>) {
        if (slot != null) {
            val parsedSlot = slot.parsePropertiesFromProductId()
                .copy(slotActions = slotActions)  // Assign slotActions here
            _screenStateStream.update {
                it.copy(slot = parsedSlot, resultStatus = ResultStatus.SUCCESS)
            }
        } else {
            _screenStateStream.update {
                it.copy(resultStatus = ResultStatus.DATA_ERROR)
            }
        }
    }

    private fun fetchSlotFromFirestoreInRealTime() {
        slotId?.let { id ->
            slotListener = firestoreDb.collection("WarehouseSlots")
                .document(id)
                .addSnapshotListener { slotSnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to slot download failed", error)
                        return@addSnapshotListener
                    }

                    if (slotSnapshot != null) {
                        if (slotSnapshot.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: ${slotSnapshot.data}")
                            val firestoreSlot =
                                slotSnapshot.toObject(FirestoreSlot::class.java)
                            var slot = WarehouseSlot(
                                productId = id,
                                quantity = firestoreSlot?.quantity ?: 0,
                                lastModified = firestoreSlot?.lastModified,
                                slotActions = listOf(),
                            )

                            fetchSlotActionsInRealtime(id, slot) //Fetch actions passing the slot
                        } else {
                            Log.w(TAG, "Document not found creating a new one")
                            sendNewSlotToFirestore(0)
                        }
                    } else {
                        Log.d(TAG, "Not connected to the internet")
                        _screenStateStream.update {
                            it.copy(resultStatus = ResultStatus.NETWORK_ERROR)
                        }
                    }
                }
        }
    }

    private fun fetchSlotActionsInRealtime(slotId: String, slot: WarehouseSlot) {
        firestoreDb.collection("WarehouseSlots")
            .document(slotId)
            .collection("SlotActions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error receiving last slot actions from Firestore", error)
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val lastActions = querySnapshot.documents.map { document ->
                        document.toObject(SlotAction::class.java) ?: SlotAction()
                    }

                    Log.d(TAG, "Last actions received: $lastActions")

                    updateSlot(slot, lastActions) // Update with both slot and actions
                }
            }
    }

    fun sendNewSlotToFirestore(quantity: Int) {
        val slot =
            WarehouseSlot(productId = slotId!!, quantity = quantity).parsePropertiesFromProductId()

        if (slot.hasAllProperties()) {
            val slotToSend = hashMapOf(
                "quantity" to slot.quantity,
            )

            Log.d(TAG, "Sending data to Firestore: $slotToSend")

            viewModelScope.launch {
                try {
                    firestoreDb.collection("WarehouseSlots")
                        .document(slot.productId)
                        .set(slotToSend) // TODO replace .set with something that does not overwrite data!
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
        } else {
            _screenStateStream.update {
                it.copy(
                    resultStatus = ResultStatus.DATA_ERROR,
                )
            }
        }
    }

    fun addActionToTheSlot(actionType: ActionType) {
        if (validateInputs(actionType)) {

            val quantityChange = when (actionType) {
                ActionType.ADD -> quantityState.value.toLong()
                ActionType.REMOVE -> -quantityState.value.toLong()
                else -> return
            }

            val updateSlot = mapOf(
                "quantity" to FieldValue.increment(quantityChange), // crucial part - updating by increment, possible inconsistency with newQuantity which is based on last downloaded quantity, but THIS is the truth maker
                "lastModified" to FieldValue.serverTimestamp(),
            )

            val slotAction = hashMapOf(
                "action" to actionType.toString(),
                "quantityChange" to quantityChange,
                "newQuantity" to (screenStateStream.value.slot?.quantity?.plus(quantityChange)
                    ?: 0),
                "timestamp" to FieldValue.serverTimestamp(),
            )

            viewModelScope.launch {
                try {
                    firestoreDb.collection("WarehouseSlots")
                        .document(slotId!!)
                        .update(updateSlot)
                        .addOnSuccessListener {
                            Log.d(TAG, "Updated slot with ID: $slotId")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating data in Firestore", e)
                }

                try {
                    firestoreDb.collection("WarehouseSlots")
                        .document(slotId!!)
                        .collection("SlotActions")
                        .add(slotAction)
                        .addOnSuccessListener {
                            Log.d(TAG, "Added slot action to slot $slotId with: $slotAction")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding slot Action", e)
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending slot Action to Firestore", e)
                }
                // updateSlot(fetchSlotFromFirestore())
            }

            resetFields()
        }
    }

    private fun resetFields() {
        quantityState.value = ""

        viewModelScope.launch {
            _validationSharedFlowStream.emit(AddActionValidationState())
        }
    }

    private fun validateInputs(actionType: ActionType): Boolean {

        val quantity = quantityState.value.toDoubleOrNull()
        if (quantity == null || quantity <= 0 || quantity > 9_999) {
            viewModelScope.launch {
                _validationSharedFlowStream.emit(AddActionValidationState(isQuantityError = true))
            }
            return false
        } else if (actionType == ActionType.REMOVE && quantity > screenStateStream.value.slot!!.quantity) {
            viewModelScope.launch {
                _validationSharedFlowStream.emit(AddActionValidationState(isRemovedError = true))
            }
            return false
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        slotListener?.remove()
        actionsListener?.remove()
    }

}

data class ShowLastActionsScreenState(
    val slot: WarehouseSlot? = null,
    val resultStatus: ResultStatus = ResultStatus.LOADING,
    val error: String? = null,
)


data class AddActionValidationState(
    val isQuantityError: Boolean = false,
    val isRemovedError: Boolean = false,
)

enum class ResultStatus {
    LOADING, SUCCESS, NETWORK_ERROR, DATA_ERROR, OTHER_ERROR
}

enum class ActionType {
    ADD, REMOVE;

    override fun toString(): String {
        return when (this) {
            ADD -> "prijem"
            REMOVE -> "vydej"
        }
    }
}