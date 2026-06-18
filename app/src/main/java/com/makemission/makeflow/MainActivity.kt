package com.makemission.makeflow

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import com.makemission.makeflow.ui.AppNavigationEngine
import com.makemission.makeflow.ui.screens.LockScreen
import com.makemission.makeflow.ui.theme.MakeFlowTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "Crash in enableEdgeToEdge", e)
        }
        try {
            setContent {
                val context = LocalContext.current
                val sharedPrefs = remember { context.getSharedPreferences("make_flow_prefs", Context.MODE_PRIVATE) }
                
                var themeMode by remember {
                    mutableStateOf(sharedPrefs.getString("theme_mode", "dark") ?: "dark")
                }
                var themePalette by remember {
                    mutableStateOf(sharedPrefs.getString("theme_palette", "forest") ?: "forest")
                }
                var isUnlocked by remember {
                    mutableStateOf(!sharedPrefs.getBoolean("screen_lock_enabled", false))
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, sharedPrefs) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            if (sharedPrefs.getBoolean("screen_lock_enabled", false)) {
                                isUnlocked = false
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                
                DisposableEffect(sharedPrefs) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                        if (key == "theme_mode") {
                            themeMode = prefs.getString("theme_mode", "dark") ?: "dark"
                        } else if (key == "theme_palette") {
                            themePalette = prefs.getString("theme_palette", "forest") ?: "forest"
                        }
                    }
                    sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose {
                        sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                    }
                }

                MakeFlowTheme(
                    themeMode = themeMode,
                    themePalette = themePalette
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!isUnlocked) {
                            LockScreen(onUnlock = { isUnlocked = true })
                        } else {
                            AppNavigationEngine()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "Crash in MainActivity setContent", e)
        }
    }
}