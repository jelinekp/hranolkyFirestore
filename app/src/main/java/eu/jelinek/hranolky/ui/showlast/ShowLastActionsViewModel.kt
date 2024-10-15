package eu.jelinek.hranolky.ui.showlast

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.tasks.await

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

    val TAG = "Firestore"

    init {
        viewModelScope.launch {
            var slot = fetchSlotFromFirestore()

            if (slot != null) {
                val quality = slot.productId.take(5)
                val parts = slot.productId.split("-")
                val rawThickness = parts[2].toFloat()

                val thickness = when(rawThickness) {
                    20.0f -> 20.0f
                    27.0f -> 27.4f
                    42.0f -> 42.4f
                    else -> rawThickness
                }

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

    fun onRadioSelected(selectedOption: String) {
        radioState.value = selectedOption
    }

    fun onQuantityChanged(newQuantity: String) {
        quantityState.value = newQuantity
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
                    slotActions = getLastActionsForSlot(slotId),
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

    private suspend fun getLastActionsForSlot(documentId: String): List<SlotAction> {
        val firestore = FirebaseFirestore.getInstance()
        val slotActionsRef = firestore.collection("WarehouseSlots")
            .document(documentId)
            .collection("SlotActions")

        return try {
            val querySnapshot = slotActionsRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()

            querySnapshot.documents.map { document ->
                document.toObject(SlotAction::class.java) ?: SlotAction()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving last actions from Firestore", e)
            // Handle error, e.g., log it or return an empty list
            emptyList()
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

    fun addActionToTheSlot() {
        if (validateInputs()) {

            val quantityChange = when (radioState.value) {
                "prijem" -> quantityState.value.toLong()
                "vydej" -> -quantityState.value.toLong()
                else -> return
            }

            val quantityFieldUpdate = when (radioState.value) {
                "prijem" -> FieldValue.increment(quantityState.value.toLong())
                "vydej" -> FieldValue.increment(-quantityState.value.toLong())
                else -> return
            }

            val slotAction = hashMapOf(
                "action" to radioState.value,
                "quantityChange" to quantityChange,
                "newQuantity" to (screenStateStream.value.slot?.quantity?.plus(quantityChange) ?: 0),
                "timestamp" to FieldValue.serverTimestamp(),
            )

            viewModelScope.launch {
                try {
                    firestoreDb.collection("WarehouseSlots")
                        .document(slotId!!)
                        .update("quantity", quantityFieldUpdate)
                        .addOnSuccessListener {
                            Log.d(TAG, "Updated slot with ID: $slotId to new Value: $quantityFieldUpdate")
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

        // _status.postValue(ResultStatus.LOADING)
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
            }
        }
        return true
    }

}

data class ShowLastActionsScreenState(
    val slot: WarehouseSlot? = null,
    val loading: Boolean = false,
    val error: String? = null,
)


data class AddActionValidationState(
    val isQuantityError: Boolean = false,
    val isRadioError: Boolean = false,
)