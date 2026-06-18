package com.makemission.makeflow.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }

    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
    var pinEntry by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // When pinEntry reaches 4 digits, proceed to step 2
    LaunchedEffect(pinEntry) {
        if (pinEntry.length == 4) {
            if (step == 1) {
                step = 2
            }
        }
    }

    // When pinConfirm reaches 4 digits, validate and save
    LaunchedEffect(pinConfirm) {
        if (pinConfirm.length == 4) {
            if (pinConfirm == pinEntry) {
                sharedPrefs.edit()
                    .putString("screen_lock_pin", pinEntry)
                    .putBoolean("screen_lock_enabled", true)
                    .apply()
                onSuccess()
            } else {
                errorMessage = "PINs do not match. Restarting setup."
                // Reset state
                step = 1
                pinEntry = ""
                pinConfirm = ""
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Set Security PIN",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Title and status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (step == 1) "Enter a 4-digit PIN" else "Confirm your PIN",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (step == 1) "Choose a PIN to secure your financial database" else "Re-enter the PIN to confirm",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // PIN indicator dots
                val currentPinLength = if (step == 1) pinEntry.length else pinConfirm.length
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    for (i in 1..4) {
                        val isFilled = i <= currentPinLength
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) MaterialTheme.colorScheme.primary 
                                    else Color.White.copy(alpha = 0.2f)
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
                        listOf("", "0", "backspace")
                    )

                    keys.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowKeys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(if (key.isNotEmpty() && key != "backspace") Color(0xFF1E2124) else Color.Transparent)
                                        .clickable(enabled = key.isNotEmpty()) {
                                            errorMessage = ""
                                            if (key == "backspace") {
                                                if (step == 1 && pinEntry.isNotEmpty()) {
                                                    pinEntry = pinEntry.dropLast(1)
                                                } else if (step == 2 && pinConfirm.isNotEmpty()) {
                                                    pinConfirm = pinConfirm.dropLast(1)
                                                }
                                            } else {
                                                if (step == 1 && pinEntry.length < 4) {
                                                    pinEntry += key
                                                } else if (step == 2 && pinConfirm.length < 4) {
                                                    pinConfirm += key
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "backspace") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = "Backspace",
                                            tint = Color.White
                                        )
                                    } else if (key.isNotEmpty()) {
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
