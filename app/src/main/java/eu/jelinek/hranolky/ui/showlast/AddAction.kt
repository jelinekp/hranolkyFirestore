package eu.jelinek.hranolky.ui.showlast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.R


data class QuantityInputData(
    val quantity: String,
    val isError: Boolean,
    val onQuantityChanged: (String) -> Unit
)

@Composable
fun AddAction(viewModel: ShowLastActionsViewModel, modifier: Modifier = Modifier) {
    val quantityFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val validationFlow by viewModel.validationSharedFlowStream.collectAsStateWithLifecycle(
        initialValue = AddActionValidationState()
    )

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(16.dp) // Apply padding to the Column
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "Přidat pohyb:",
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            val quantityInputData = QuantityInputData(
                quantity = viewModel.quantityState.value,
                isError = validationFlow.isQuantityError,
                onQuantityChanged = viewModel::onQuantityChanged // Pass the callback
            )
            QuantityInput(quantityInputData, quantityFocusRequester, keyboardController)
        }

        // Error message (conditional)
        if (validationFlow.isQuantityError || validationFlow.isRemovedError) {
            val errorText = if (validationFlow.isQuantityError) {
                stringResource(R.string.zadej_platn_mno_stv)
            } else {
                stringResource(R.string.vydej_nemuze_byt_vetsi_nez_aktualni_stav)
            }
            ErrorText(text = errorText)
        }

        ActionButtons(viewModel, keyboardController)
    }
}

@Composable
fun QuantityInput(
    inputData: QuantityInputData,
    quantityFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?
) {
    OutlinedTextField(
        value = inputData.quantity,
        onValueChange = inputData.onQuantityChanged, // Use the callback
        label = { Text(stringResource(R.string.zadej_mno_stv)) },
        isError = inputData.isError,
        keyboardActions = KeyboardActions(onDone = {
            quantityFocusRequester.freeFocus()
            keyboardController?.hide()
        }),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .widthIn(max = 180.dp)
            .focusRequester(quantityFocusRequester)
            .padding(bottom = 8.dp), // Apply padding here
        singleLine = true
    )
}

@Composable
fun ActionButtons(
    viewModel: ShowLastActionsViewModel,
    keyboardController: SoftwareKeyboardController?
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.width(280.dp)
    ) {
        ActionButton(
            text = stringResource(R.string.prijem),
            icon = Icons.Default.Add,
            onClick = {
                keyboardController?.hide()
                viewModel.addActionToTheSlot(ActionType.ADD)
            }
        )

        Spacer(modifier = Modifier.width(32.dp))

        ActionButton(
            text = stringResource(R.string.vydej),
            icon = null, // No icon for "Výdej"
            onClick = {
                keyboardController?.hide()
                viewModel.addActionToTheSlot(ActionType.REMOVE)
            }
        )
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .padding(vertical = 4.dp)
    ) {
        Text(text, fontSize = 16.sp)
        if (icon != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = icon,
                contentDescription = null, // Provide a description if needed
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, // This holds the error message
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(top = 4.dp)
    )
}