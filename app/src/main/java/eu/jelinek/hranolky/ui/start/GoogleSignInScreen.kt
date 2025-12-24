package eu.jelinek.hranolky.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.jelinek.hranolky.R
import eu.jelinek.hranolky.domain.AuthState


@Composable
fun GoogleSignInScreen(
    authState: AuthState,
    onSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_jelinek),
                contentDescription = "Logo JELÍNEK",
                modifier = Modifier.padding(horizontal = 48.dp),
                colorFilter = if (isSystemInDarkTheme()) {
                    ColorFilter.tint(colorScheme.onBackground)
                } else {
                    null
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "Logo aplikace Hranolky",
                modifier = Modifier.size(200.dp),
            )

            Text(
                text = "Hranolky a Spárovky",
                style = typography.headlineLarge,
                color = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Je potřeba se přihlásit",
                style = typography.titleMedium,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (authState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Přihlašování...",
                    style = typography.bodyMedium
                )
            } else {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text("Přihlásit se pomocí Google")
                }

                authState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
