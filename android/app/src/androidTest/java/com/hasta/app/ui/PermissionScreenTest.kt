package com.hasta.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hasta.app.ui.theme.HastaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(
        kind: PermissionScreenKind,
        dark: Boolean = false,
        onAllow: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        compose.setContent {
            HastaTheme(darkTheme = dark, reducedMotion = false) {
                PermissionScreen(
                    kind = kind,
                    onAllow = onAllow,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }

    @Test
    fun rationaleExplainsOnDeviceProcessing() {
        show(PermissionScreenKind.Rationale)

        compose.onNodeWithText("A camera for your hands").assertIsDisplayed()
        compose.onNodeWithText("On-device only").assertIsDisplayed()
        compose.onNodeWithText("Allow camera").assertIsDisplayed()
    }

    @Test
    fun denyingKeepsRecoveryAvailable() {
        show(PermissionScreenKind.Denied)

        compose.onNodeWithText("Camera access is needed").assertIsDisplayed()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
    }

    @Test
    fun permanentDenialSendsTheLearnerToSettings() {
        var settingsClicked = false
        show(
            kind = PermissionScreenKind.PermanentlyDenied,
            onOpenSettings = { settingsClicked = true },
        )

        compose.onNodeWithText("Camera access is off").assertIsDisplayed()
        compose.onNodeWithText("Open settings").performClick()
        assertTrue(settingsClicked)
    }

    @Test
    fun permissionScreenRendersInDarkTheme() {
        show(PermissionScreenKind.Rationale, dark = true)

        compose.onNodeWithText("A camera for your hands").assertIsDisplayed()
    }

    @Test
    fun allowButtonReportsTheClick() {
        var allowed = false
        show(kind = PermissionScreenKind.Rationale, onAllow = { allowed = true })

        compose.onNodeWithText("Allow camera").performClick()
        assertTrue(allowed)
    }
}
