package com.makemission.makeflow.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.makemission.makeflow.BuildConfig
import com.makemission.makeflow.core.layout.DeviceLayoutMode
import com.makemission.makeflow.core.layout.LocalDeviceLayoutMode
import com.makemission.makeflow.core.device.LocalReducedMotion
import com.makemission.makeflow.ui.theme.LocalAppDimens
import com.makemission.makeflow.ui.theme.MakeFlowTheme
import com.makemission.makeflow.ui.theme.compactDimens
import com.makemission.makeflow.ui.theme.expandedDimens
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WhatsNewTest {
    @get:Rule val rule = createComposeRule()

    @Test fun updatesAndGuideAreAccessibleAndBackWorks() {
        var backCount = 0
        show(onBack = { backCount++ })
        rule.onNodeWithText("Updates").assertIsSelected()
        rule.onNodeWithText("v${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        rule.onNodeWithText("Feature guide").performClick().assertIsSelected()
        rule.onNodeWithText("Accounts and currencies").assertIsDisplayed()
        rule.onNodeWithTag("whats_new_list").performScrollToNode(hasText("surfaced amount display", substring = true))
        rule.onNodeWithText("surfaced amount display", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("whats_new_list").performScrollToNode(hasText("Widgets and quick access"))
        rule.onNodeWithText("Widgets and quick access").assertIsDisplayed()
        rule.onNodeWithText("Updates").performClick()
        rule.onNodeWithText("v${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test fun guideTabAndScrollSurviveRestorationOnTablet() {
        val restoration = StateRestorationTester(rule)
        show(tablet = true, restoration = restoration)
        rule.onNodeWithText("Feature guide").performClick()
        rule.onNodeWithTag("whats_new_list").performScrollToNode(hasText("Tablet workspaces"))
        rule.onNodeWithTag("whats_new_list").performScrollToNode(hasText("available balance", substring = true))
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Feature guide").assertIsSelected()
        rule.onNodeWithText("selected account's available balance", substring = true).assertIsDisplayed()
        saveScreenshot("whats_new_tablet.png")
    }

    @Test fun largeTextKeepsTabsBackAndLastGuideSectionReachable() {
        show(fontScale = 2f)
        rule.onNodeWithText("Feature guide").performClick()
        rule.onNodeWithTag("whats_new_list").performScrollToNode(hasText("Privacy, backup, and preferences"))
        rule.onNodeWithText("Privacy, backup, and preferences").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").assertIsDisplayed()
        saveScreenshot("whats_new_large_text.png")
        rule.onNodeWithText("Updates").performClick().assertIsSelected()
    }

    @Test fun interactiveTourCoversTheGuideAndOpensTransactionEntry() {
        var transactionEntryOpened = false
        show(onStartTransactionEntry = { transactionEntryOpened = true })
        rule.onNodeWithText("Feature guide").performClick()
        rule.onNodeWithTag("start_interactive_tour").performClick()
        rule.onNodeWithText("Accounts and currencies").assertIsDisplayed()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Transaction entry").assertIsDisplayed()
        rule.onNodeWithText("Try transaction entry").performClick()
        rule.runOnIdle { assertEquals(true, transactionEntryOpened) }
    }

    @Test fun expandedTourSelectsTabletTopicsBesideTheirDetails() {
        show(tablet = true)
        rule.onNodeWithText("Feature guide").performClick()
        rule.onNodeWithTag("start_interactive_tour").performClick()
        rule.onNodeWithText("Tablet workspaces").performClick()
        rule.onNodeWithTag("tour_step_features").performScrollToNode(
            hasText("Expanded tablets show a feature list beside animated step details", substring = true)
        )
        rule.onNodeWithText("Expanded tablets show a feature list beside animated step details", substring = true)
            .assertIsDisplayed()
        rule.onNodeWithText("Widgets and quick access").performClick()
        rule.onNodeWithTag("tour_widget_repeat_preview").assertIsDisplayed()
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.cacheDir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(tablet: Boolean = false, fontScale: Float = 1f,
        restoration: StateRestorationTester? = null, onBack: () -> Unit = {},
        onStartTransactionEntry: () -> Unit = {}) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                if (tablet) DpSize(1200.dp, 900.dp) else DpSize(400.dp, 800.dp)
            ) then DeviceConfigurationOverride.FontScale(fontScale)) {
                MakeFlowTheme {
                    CompositionLocalProvider(
                        LocalDeviceLayoutMode provides if (tablet) DeviceLayoutMode.EXPANDED else DeviceLayoutMode.COMPACT,
                        LocalAppDimens provides if (tablet) expandedDimens else compactDimens,
                        LocalReducedMotion provides true
                    ) { ChangelogScreen(onBack, onStartTransactionEntry = onStartTransactionEntry) }
                }
            }
        }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
    }
}
