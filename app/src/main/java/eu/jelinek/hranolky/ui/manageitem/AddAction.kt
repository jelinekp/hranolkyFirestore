package eu.jelinek.hranolky.ui.manageitem

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
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.model.ActionType

data class QuantityInputData(
    val quantity: String,
    val isError: Boolean,
    val onQuantityChanged: (String) -> Unit
)

@Composable
fun AddAction(
    quantity: String,
    onQuantityChanged: (String) -> Unit,
    validationState: AddActionValidationState,
    onAddActionClick: (ActionType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Přidat pohyb:"
) {
    val quantityFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 8.dp, end = 8.dp)
            )
            val quantityInputData = QuantityInputData(
                quantity = quantity,
                isError = validationState.isQuantityError,
                onQuantityChanged = onQuantityChanged
            )
            QuantityInput(
                inputData = quantityInputData,
                quantityFocusRequester = quantityFocusRequester,
                keyboardController = keyboardController
            )
        }

        if (validationState.errorMessage != null) {
            ErrorText(text = validationState.errorMessage)
        } else if (validationState.isQuantityError) { // Fallback generic messages if no specific errorMessage
            ErrorText(text = stringResource(R.string.zadej_platn_mno_stv))
        } else if (validationState.isRemovedError) {
            ErrorText(text = stringResource(R.string.vydej_nemuze_byt_vetsi_nez_aktualni_stav))
        }

        ActionButtons(
            onAddActionClick = onAddActionClick,
            keyboardController = keyboardController
        )
    }
}

@Composable
fun QuantityInput(
    inputData: QuantityInputData,
    quantityFocusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.zadej_mno_stv),
    keyboardType: KeyboardType = KeyboardType.Number
) {
    OutlinedTextField(
        value = inputData.quantity,
        onValueChange = inputData.onQuantityChanged,
        label = { Text(label) },
        isError = inputData.isError,
        keyboardActions = KeyboardActions(onDone = {
            quantityFocusRequester.freeFocus()
            keyboardController?.hide()
        }),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
            .widthIn(max = 180.dp)
            .focusRequester(quantityFocusRequester),
        singleLine = true
    )
}

@Composable
fun ActionButtons(
    onAddActionClick: (ActionType) -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(280.dp)
            .padding(vertical = 6.dp)
    ) {
        ActionButton(
            text = stringResource(R.string.prijem),
            icon = Icons.Default.Add,
            onClick = {
                keyboardController?.hide()
                onAddActionClick(ActionType.ADD)
            }
        )

        Spacer(modifier = Modifier.width(32.dp))

        ActionButton(
            text = stringResource(R.string.vydej),
            icon = null,
            onClick = {
                keyboardController?.hide()
                onAddActionClick(ActionType.REMOVE)
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
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(top = 4.dp)
    )
}