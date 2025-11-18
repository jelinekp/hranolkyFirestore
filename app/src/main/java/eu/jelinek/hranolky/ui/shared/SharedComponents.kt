package eu.jelinek.hranolky.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.model.SlotAction
import eu.jelinek.hranolky.ui.start.StartUiState


fun LazyListScope.slotActionsIndexedWithAlternatingModifier(
    items: List<SlotAction>,
    alternateModifier: Modifier,
    itemContent: @Composable (index: Int, item: SlotAction, modifier: Modifier) -> Unit
) {
    itemsIndexed(items) { index, item ->
        val modifier = if (index % 2 == 0) {
            Modifier.background(MaterialTheme.colorScheme.surface)
        } else {
            alternateModifier
        }
        itemContent(index, item, modifier)
    }
}

@Composable
fun SignInStatus(
    screenState: StartUiState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (screenState.isSigningIn || screenState.isSignInProblem) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (screenState.isSigningIn) {
                Text("Přihlašování")
                CircularProgressIndicator()
            } else { // This implies isSignInProblem is true
                ErrorText("Přihlášení selhalo")
            }
        }
    } else {
        content()
    }
}


@Composable
fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
fun AutoScanToggle(
    isAutoScanEnabled: Boolean,
    onAutoScanToggleChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(280.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
        Switch(
            checked = isAutoScanEnabled,
            onCheckedChange = onAutoScanToggleChange
        )
    }
}

@Composable
fun ScannedCodeInput(
    scannedText: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isError: Boolean,
    onDoneAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = scannedText,
        onValueChange = { newValue ->
            onValueChange(newValue.uppercase())
        },
        label = { Text(stringResource(R.string.naskenuj_kod)) },
        modifier = modifier.focusRequester(focusRequester),
        isError = isError,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onDoneAction()
                focusRequester.freeFocus()
            }
        )
    )
}

@Composable
fun ManualScanButton(
    onClicked: () -> Unit
) {
    Button(
        onClick = { onClicked() }
    ) {
        Text("Přejít na položku")
        Icon(
            Icons.AutoMirrored.Default.ArrowForward,
            contentDescription = stringResource(R.string.forward_icon),
            modifier = Modifier.padding(all = 4.dp)
        )
    }
}

@Composable
fun NavigationActionButton(
    text: String,
    iconPainter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Allow passing additional modifiers
) {
    Button(
        onClick = onClick,
        // Apply a common modifier first, then any additional specific modifiers
        modifier = modifier, // Chain any passed-in modifier
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = contentDescription,
            modifier = Modifier.padding(all = 6.dp) // Common padding for the icon
        )
        Text(text)
    }
}
