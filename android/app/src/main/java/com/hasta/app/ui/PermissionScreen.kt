package com.hasta.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hasta.app.R
import com.hasta.app.ui.component.BrandLockup
import com.hasta.app.ui.component.HastaButton
import com.hasta.app.ui.component.HastaCard
import com.hasta.app.ui.component.HastaCardVariant
import com.hasta.app.ui.component.HastaButtonVariant
import com.hasta.app.ui.component.HastaIcon
import com.hasta.app.ui.component.HastaIconName
import com.hasta.app.ui.theme.HastaSpacing
import com.hasta.app.ui.theme.HastaTheme

enum class PermissionScreenKind { Rationale, Denied, PermanentlyDenied }

@Composable
fun PermissionScreen(
    kind: PermissionScreenKind,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HastaTheme.colors
    val title = stringResource(
        when (kind) {
            PermissionScreenKind.Rationale -> R.string.permission_title
            PermissionScreenKind.Denied -> R.string.permission_denied_title
            PermissionScreenKind.PermanentlyDenied -> R.string.permission_permanently_denied_title
        },
    )
    val body = stringResource(
        when (kind) {
            PermissionScreenKind.Rationale -> R.string.permission_body
            PermissionScreenKind.Denied -> R.string.permission_denied_body
            PermissionScreenKind.PermanentlyDenied -> R.string.permission_settings_hint
        },
    )
    val primaryLabel = stringResource(
        if (kind == PermissionScreenKind.PermanentlyDenied) {
            R.string.action_open_settings
        } else {
            R.string.action_allow_camera
        },
    )
    val primaryDescription = stringResource(
        if (kind == PermissionScreenKind.PermanentlyDenied) {
            R.string.a11y_open_settings
        } else {
            R.string.a11y_allow_camera
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HastaSpacing.lg, vertical = HastaSpacing.xl),
        horizontalAlignment = Alignment.Start,
    ) {
        BrandLockup(onCamera = false, showTagline = true)

        Spacer(Modifier.height(HastaSpacing.xxl))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            modifier = Modifier.widthIn(max = 440.dp),
        )
        Spacer(Modifier.height(HastaSpacing.sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            modifier = Modifier.widthIn(max = 440.dp),
        )

        Spacer(Modifier.height(HastaSpacing.lg))
        HastaCard(
            variant = HastaCardVariant.Accent,
            modifier = Modifier.widthIn(max = 440.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                HastaIcon(
                    name = HastaIconName.Info,
                    tint = colors.brandAccent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp),
                )
                Spacer(Modifier.width(HastaSpacing.sm))
                Column {
                    Text(
                        text = stringResource(R.string.permission_privacy_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBrandContainer,
                    )
                    Text(
                        text = stringResource(R.string.permission_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBrandContainer,
                        modifier = Modifier.padding(top = HastaSpacing.xxs),
                    )
                }
            }
        }

        Spacer(Modifier.height(HastaSpacing.lg))
        HastaButton(
            text = primaryLabel,
            onClick = if (kind == PermissionScreenKind.PermanentlyDenied) onOpenSettings else onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            icon = if (kind == PermissionScreenKind.PermanentlyDenied) {
                HastaIconName.Settings
            } else {
                HastaIconName.Camera
            },
            contentDescription = primaryDescription,
        )

        if (kind == PermissionScreenKind.Denied) {
            HastaButton(
                text = stringResource(R.string.permission_open_settings),
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = HastaSpacing.xs),
                variant = HastaButtonVariant.Text,
                contentDescription = stringResource(R.string.a11y_open_settings),
            )
        }
    }
}
