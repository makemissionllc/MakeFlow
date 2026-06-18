package com.makemission.makeflow.ui.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.makemission.makeflow.ui.components.BiometricHelper
import com.makemission.makeflow.ui.components.findActivity
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }
    val savedPin = remember { sharedPrefs.getString("screen_lock_pin", "") ?: "" }
    val biometricsEnabled = remember { sharedPrefs.getBoolean("screen_lock_biometrics_enabled", false) }
    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    var pinEntry by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    // Helper to launch biometric prompt
    val launchBiometrics = {
        val activity = context.findActivity()
        if (activity != null && biometricsEnabled && isBiometricAvailable) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    onUnlock()
                }
            )
        }
    }

    // Auto-launch biometrics on startup
    LaunchedEffect(Unit) {
        launchBiometrics()
    }

    // Validate PIN when 4 digits are entered
    LaunchedEffect(pinEntry) {
        if (pinEntry.length == 4) {
            if (pinEntry == savedPin) {
                onUnlock()
            } else {
                isError = true
                coroutineScope.launch {
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 300
                            -20f at 50
                            20f at 100
                            -15f at 150
                            15f at 200
                            -10f at 250
                        }
                    )
                    pinEntry = ""
                    isError = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1011))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer to center visual elements
            Spacer(modifier = Modifier.height(16.dp))

            // Lock Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "App Locked",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter security PIN to access your workspace",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // PIN Dots with shake animation
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .graphicsLayer(translationX = shakeOffset.value)
                    .padding(vertical = 32.dp)
            ) {
                for (i in 1..4) {
                    val isFilled = i <= pinEntry.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> MaterialTheme.colorScheme.error
                                    isFilled -> MaterialTheme.colorScheme.primary
                                    else -> Color.White.copy(alpha = 0.15f)
                                }
                            )
                    )
                }
            }

            // Numerical Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("biometric", "0", "backspace")
                )

                keys.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            val isKeyEnabled = when (key) {
                                "biometric" -> biometricsEnabled && isBiometricAvailable
                                else -> true
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (key != "backspace" && key != "biometric") Color(0xFF1E2124) 
                                        else Color.Transparent
                                    )
                                    .clickable(enabled = isKeyEnabled) {
                                        if (key == "backspace") {
                                            if (pinEntry.isNotEmpty()) {
                                                pinEntry = pinEntry.dropLast(1)
                                            }
                                        } else if (key == "biometric") {
                                            launchBiometrics()
                                        } else {
                                            if (pinEntry.length < 4) {
                                                pinEntry += key
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when (key) {
                                    "backspace" -> {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = "Backspace",
                                            tint = Color.White
                                        )
                                    }
                                    "biometric" -> {
                                        if (biometricsEnabled && isBiometricAvailable) {
                                            Icon(
                                                imageVector = Icons.Outlined.Fingerprint,
                                                contentDescription = "Biometric Lock",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = key,
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
