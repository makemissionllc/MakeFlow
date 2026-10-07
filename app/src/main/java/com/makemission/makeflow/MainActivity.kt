package com.makemission.makeflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.makemission.makeflow.data.local.UserPreferencesDataStore
import com.makemission.makeflow.data.local.userPrefDataStore
import com.makemission.makeflow.core.notifications.MpesaAutoImportNotification
import com.makemission.makeflow.domain.MpesaMessageParser
import com.makemission.makeflow.ui.navigation.AppNavigationEngine
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    var sharedText by mutableStateOf<String?>(null)
        private set

    private var pendingMpesaReview by mutableStateOf<MpesaMessageParser.MpesaParsedMessage?>(null)

    private var splashReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !splashReady }
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            UserPreferencesDataStore(this@MainActivity.userPrefDataStore).isProfiled.first()
            splashReady = true
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        handleShareIntent(intent)
        setContent {
            MakeFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationEngine(
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedText = null },
                        importedMpesaMessage = pendingMpesaReview,
                        onImportedMpesaMessageConsumed = { pendingMpesaReview = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        when {
            intent?.action == MpesaAutoImportNotification.ACTION_REVIEW -> {
                pendingMpesaReview = MpesaAutoImportNotification.readFromIntent(intent)
            }
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }
    }
}
