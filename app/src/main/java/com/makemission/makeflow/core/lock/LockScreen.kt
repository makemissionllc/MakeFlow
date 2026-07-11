package com.makemission.makeflow.core.lock

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun LockScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return

    var authState by remember { mutableStateOf(LockAuthState.REQUIRED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authState = LockAuthState.AUTHENTICATED
                onUnlocked()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                authState = LockAuthState.ERROR
                errorMessage = errString.toString()
            }

            override fun onAuthenticationFailed() {
                errorMessage = "Biometric not recognized. Try again."
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("App Lock")
            .setSubtitle("Authenticate to access MakeFlow")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        authState = LockAuthState.AUTHENTICATING
        errorMessage = null
        prompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF121315)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0xFF1B1C1F)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MF",
                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "MakeFlow",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Authenticate to unlock the app",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            if (authState == LockAuthState.ERROR && errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = androidx.compose.ui.graphics.Color(0xFFEF5350),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { retryTrigger++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = if (authState == LockAuthState.AUTHENTICATING) "Authenticating..." else "Unlock",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Uses fingerprint, face, or device PIN",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private enum class LockAuthState {
    REQUIRED,
    AUTHENTICATING,
    AUTHENTICATED,
    ERROR
}
