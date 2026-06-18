package com.makemission.makeflow.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }

    var themeMode by remember {
        mutableStateOf(sharedPrefs.getString("theme_mode", "dark") ?: "dark")
    }
    var themePalette by remember {
        mutableStateOf(sharedPrefs.getString("theme_palette", "forest") ?: "forest")
    }
    var hapticEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("haptic_enabled", true))
    }
    var privacyChecked by remember {
        mutableStateOf(sharedPrefs.getBoolean("privacy_enabled", true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1011),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "General Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ── Theme Mode Section ──
            Text(
                text = "Theme Mode",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2124), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                    val isSelected = themeMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                themeMode = mode
                                sharedPrefs.edit().putString("theme_mode", mode).apply()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.6f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Theme Color Palette Section ──
            Text(
                text = "Color Palette",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val palettes = listOf(
                    "forest" to ("Forest" to Color(0xFF6DD881)),
                    "ocean" to ("Ocean" to Color(0xFF63B3F4)),
                    "sunset" to ("Sunset" to Color(0xFFFFB595)),
                    "purple" to ("Purple" to Color(0xFFD0BCFF))
                )

                // Show dynamic wallpaper option on Android S+ (Android 12+)
                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    palettes + ("dynamic" to ("Wallpaper" to Color.Transparent))
                } else {
                    palettes
                }

                options.forEach { (paletteId, info) ->
                    val (label, themeColor) = info
                    val isSelected = themePalette == paletteId

                    Row(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color(0xFF1E2124),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                themePalette = paletteId
                                sharedPrefs.edit().putString("theme_palette", paletteId).apply()
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (paletteId == "dynamic") {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.sweepGradient(
                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                        )
                                    )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(themeColor, CircleShape)
                            )
                        }
                        
                        Text(
                            text = label, 
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f), 
                            fontSize = 13.sp, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Haptic Feedback Toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2124), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    Text(text = "Haptic Feedback", color = Color.White, fontSize = 16.sp)
                }
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        hapticEnabled = it
                        sharedPrefs.edit().putBoolean("haptic_enabled", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                        uncheckedTrackColor = Color(0xFF2C2F33)
                    )
                )
            }

            // ── Privacy Toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2124), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    Text(text = "Privacy Mode", color = Color.White, fontSize = 16.sp)
                }
                Checkbox(
                    checked = privacyChecked,
                    onCheckedChange = {
                        privacyChecked = it
                        sharedPrefs.edit().putBoolean("privacy_enabled", it).apply()
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.Transparent,
                        checkmarkColor = Color.White,
                        uncheckedColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
