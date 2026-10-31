package com.hasta.app.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hasta.app.ui.theme.HastaSizes

@Composable
fun HastaTopBar(
    onCamera: Boolean,
    compact: Boolean,
    status: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.heightIn(min = HastaSizes.topBar),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLockup(onCamera = onCamera)
        if (compact) {
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.weight(1f))
        }
        status()
    }
}
