package eu.jelinek.hranolky.ui.start

import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.jelinek.hranolky.ui.shared.ScreenSize
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartScreen(
    modifier: Modifier = Modifier,
    navigateToManageItem: (String) -> Unit,
    navigateToOverview: () -> Unit,
    navigateToHistory: () -> Unit,
    viewModel: StartViewModel = koinViewModel(),
    screenSize: ScreenSize = ScreenSize.PHONE
) {
    val screenState by viewModel.startScreenState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showReleaseNotes by remember { mutableStateOf(false) }

    var isL1Pressed by remember { mutableStateOf(false) }
    var isR1Pressed by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success snackbar when app was just updated
    LaunchedEffect(updateState.justUpdated) {
        if (updateState.justUpdated) {
            snackbarHostState.showSnackbar(
                message = "Aplikace byla úspěšně aktualizována! 🎉",
                duration = SnackbarDuration.Long
            )
        }
    }

    // Animate the pads sliding in from the edges with smooth motion
    val leftPadOffset by animateFloatAsState(
        targetValue = if (isL1Pressed) -27f else -33f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "leftPadOffset"
    )

    val rightPadOffset by animateFloatAsState(
        targetValue = if (isR1Pressed) 27f else 33f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rightPadOffset"
    )

    // Show release notes dialog when update is available
    LaunchedEffect(updateState.isUpdateAvailable) {
        if (updateState.isUpdateAvailable && updateState.releaseNotes.isNotEmpty()) {
            showReleaseNotes = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToManageItem.collect { route ->
            navigateToManageItem(route)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearScannedCode()
            // hide keyboard on screen exit
            keyboardController?.hide()
        }
    }

    // Release notes dialog
    if (showReleaseNotes && updateState.isUpdateAvailable) {
        AlertDialog(
            onDismissRequest = { showReleaseNotes = false },
            title = { Text("Nová verze ${updateState.latestVersion} k dispozici") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Co je nového:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(updateState.releaseNotes, style = typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Až se dokončí stahování aktualizace, povolte oprávnění instalovat a klikněte na \"Instalovat\". Poté znovu otevřete aplikaci.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showReleaseNotes = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Show full-screen installation overlay when installing (blocks all interaction)
    if (updateState.isInstalling) {
        InstallationOverlay(updateState = updateState)
        return
    }

    // Show sign-in screen if not authenticated
    if (!authState.isSignedIn) {
        GoogleSignInScreen(
            authState = authState,
            onSignInClick = { viewModel.signInWithGoogle(context) }
        )
        return
    }

    Scaffold(
        modifier = Modifier.onKeyEvent { keyEvent ->
            when (keyEvent.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_BUTTON_L1 -> {
                    isL1Pressed = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                    true
                }
                KeyEvent.KEYCODE_BUTTON_R1 -> {
                    isR1Pressed = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN
                    true
                }
                else -> false
            }
        },
        topBar = {
            if (screenSize.isTablet()) StartScreenTopBar(
                navigateToOverview = navigateToOverview
            )
        },
        bottomBar = {
            // Show update progress at the bottom
            if (updateState.isDownloading || updateState.isInstalling) {
                UpdateProgressBar(updateState = updateState)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            if (screenSize.isPhone()) {
                StartScreenPhoneLayout(
                    modifier = modifier.padding(padding),
                    screenState = screenState,
                    viewModel = viewModel,
                    navigateToHistory = navigateToHistory,
                    navigateToOverview = navigateToOverview,
                    keyboardController = keyboardController,
                    focusRequester = focusRequester
                )
            } else {
                StartScreenTabletLayout(
                    modifier = modifier.padding(padding),
                    screenState = screenState,
                    viewModel = viewModel,
                    navigateToManageItem = navigateToManageItem,
                    focusRequester = focusRequester
                )
            }

            // Button indicator pads with subtle pulse
            ButtonPad(
                offset = leftPadOffset,
                isPressed = isL1Pressed,
                modifier = Modifier.align(Alignment.TopStart)
            )
            ButtonPad(
                offset = rightPadOffset,
                isPressed = isR1Pressed,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}