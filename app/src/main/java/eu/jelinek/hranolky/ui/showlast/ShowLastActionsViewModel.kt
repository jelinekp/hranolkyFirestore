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
import eu.jelinek.hranolky.model.Quantity
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

    var radioState = mutableStateOf("")
        private set

    var quantityState = mutableStateOf("")
        private set

    fun onRadioSelected(selectedOption: String) {
        radioState.value = selectedOption
    }

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
    }

    val TAG = "Firestore"

    private var slotListener: ListenerRegistration? = null
    private var actionsListener: ListenerRegistration? = null

    init {
        viewModelScope.launch {
            fetchSlotFromFirestore()
        }
    }

    private fun updateSlot(slot: WarehouseSlot?) {
        if (slot != null) {
            val parsedSlot =
                slot.parsePropertiesFromProductId() // crucial part - extracting data from productId to unassigned slot properties

            _screenStateStream.update {
                it.copy(slot = parsedSlot)
            }
        }
    }

    fun fetchSlotFromFirestore() {

        slotId?.let { id ->
            slotListener = firestoreDb.collection("WarehouseSlots")
                .document(id)
                .addSnapshotListener { slotSnapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen to slot download failed", error)
                        return@addSnapshotListener
                    }

                    if (slotSnapshot != null && slotSnapshot.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: ${slotSnapshot.data}")
                        val warehouseQuantity =
                            slotSnapshot.toObject(Quantity::class.java) // Convert to mock data class
                        var slot = WarehouseSlot(
                            productId = id,
                            quantity = warehouseQuantity?.quantity ?: 0,
                            slotActions = listOf(),
                        )

                        fetchSlotActionsInRealtime(id)

                        updateSlot(slot)

                    } else {
                        Log.w(TAG, "Document not found creating a new one")
                        sendNewSlotToFirestore(0)
                    }
                }
        }
    }

    private fun fetchSlotActionsInRealtime(slotId: String) {
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

                    // Update the UI state with the new actions
                    _screenStateStream.update {
                        it.copy(slot = it.slot?.copy(slotActions = lastActions))
                    }
                }
            }
    }

    fun sendNewSlotToFirestore(quantity: Int) {
        val slot = WarehouseSlot(
            productId = slotId!!,
            //productId = "DUB-A-20-42-0480", is already the unique id of the document
            quantity = quantity,
            /*quality = , decided to not implement now, is maybe a problem in the future
            thickness = ,
            width = ,
            length = ,*/
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

    fun addActionToTheSlot() {
        if (validateInputs()) {

            val quantityChange = when (radioState.value) {
                "prijem" -> quantityState.value.toLong()
                "vydej" -> -quantityState.value.toLong()
                else -> return
            }

            val slotAction = hashMapOf(
                "action" to radioState.value,
                "quantityChange" to quantityChange,
                "newQuantity" to (screenStateStream.value.slot?.quantity?.plus(quantityChange)
                    ?: 0),
                "timestamp" to FieldValue.serverTimestamp(),
            )

            viewModelScope.launch {
                try {
                    firestoreDb.collection("WarehouseSlots")
                        .document(slotId!!)
                        .update(
                            "quantity",
                            FieldValue.increment(quantityChange)
                        ) // crucial part - updating by increment, possible inconsistency with newQuantity which is based on last downloaded quantity, but THIS is the truth maker
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
        radioState.value = ""
        quantityState.value = ""

        viewModelScope.launch {
            _validationSharedFlowStream.emit(AddActionValidationState())
        }
    }

    private fun validateInputs(): Boolean {
        if (radioState.value.isEmpty()) {
            viewModelScope.launch {
                _validationSharedFlowStream.emit(AddActionValidationState(isRadioError = true))
            }
            return false
        } else {
            val quantity = quantityState.value.toDoubleOrNull()
            if (quantity == null || quantity <= 0 || quantity > 9_999) {
                viewModelScope.launch {
                    _validationSharedFlowStream.emit(AddActionValidationState(isQuantityError = true))
                }
                return false
            } else if (radioState.value == "vydej" && quantity > screenStateStream.value.slot!!.quantity) {
                viewModelScope.launch {
                    _validationSharedFlowStream.emit(AddActionValidationState(isRemovedError = true))
                }
                return false
            }
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
    val loading: Boolean = false,
    val error: String? = null,
)


data class AddActionValidationState(
    val isQuantityError: Boolean = false,
    val isRemovedError: Boolean = false,
    val isRadioError: Boolean = false,
)

enum class ResultStatus {
    LOADING, SUCCESS, NETWORK_ERROR, DATA_ERROR, OTHER_ERROR
}