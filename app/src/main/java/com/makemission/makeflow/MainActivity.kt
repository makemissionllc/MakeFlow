package com.makemission.makeflow

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.makemission.makeflow.ui.AppNavigationEngine
import com.makemission.makeflow.ui.theme.MakeFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "Crash in enableEdgeToEdge", e)
        }
        try {
            setContent {
                MakeFlowTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigationEngine()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MakeFlowDebug", "Crash in MainActivity setContent", e)
        }
    }
}