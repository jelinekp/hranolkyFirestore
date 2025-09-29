package eu.jelinek.hranolky.ui.manageitem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InventoryCheck(
    quantity: String,
    onQuantityChanged: (String) -> Unit,
    validationState: AddActionValidationState,
    onSetClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isInventoryCheckDone: Boolean = false,
    label: String = "Opravit množství na:"
) {
    val quantityFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current


    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(all = 8.dp)
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .border(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large,
                width = 4.dp
            )
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = if (isInventoryCheckDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    shape = MaterialTheme.shapes.large
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Inventura " + if (isInventoryCheckDone) "provedena" else "neprovedena",
                fontSize = 16.sp,
                color = if (isInventoryCheckDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            /*Text(
                text = label,
                fontSize = 16.sp,
                modifier = Modifier
                    .weight(2f)
                    .padding(top = 8.dp)
            )*/
            val quantityInputData = QuantityInputData(
                quantity = quantity,
                isError = validationState.isQuantityError,
                onQuantityChanged = onQuantityChanged
            )
            QuantityInput(
                inputData = quantityInputData,
                quantityFocusRequester = quantityFocusRequester,
                keyboardController = keyboardController,
                modifier = Modifier.weight(3f),
                label = "Zadej stav při inventuře"
            )
            ActionButton(
                text = "Odeslat",
                onClick = {
                    onSetClicked()
                    keyboardController?.hide()
                },
                icon = Icons.AutoMirrored.Default.Send,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

    }
}

@Composable
fun ConfirmSettingPopup(
    modifier: Modifier = Modifier,
    dialogTitle: String,
    dialogMessage: String,
    showYesNoDialog: Boolean,
    onDismissYesNoDialog: () -> Unit,
    onConfirmYesNoDialog: () -> Unit,
) {
    if (showYesNoDialog) {
        AlertDialog(
            onDismissRequest = onDismissYesNoDialog,
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                Button(onClick = onConfirmYesNoDialog) {
                    Text("Opravit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissYesNoDialog) {
                    Text("Zrušit")
                }
            },
            modifier = modifier
        )
    }
}