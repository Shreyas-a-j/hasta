package com.hasta.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hasta.app.R
import com.hasta.app.data.benchmark.BenchmarkRecorder
import com.hasta.app.data.benchmark.BenchmarkState
import com.hasta.app.ui.component.HandOverlay
import com.hasta.app.ui.component.TrackingStatusChip
import com.hasta.app.ui.permission.CameraPermissionStatus
import com.hasta.app.ui.permission.PermissionStatusReader

/**
 * Hosts the camera preview or the permission recovery UI.
 *
 * The permission state is derived from the platform on every resume rather than
 * cached in a one-shot callback, so a user returning from Settings is handled.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    benchmark: BenchmarkRecorder? = null,
) {
    val context = LocalContext.current
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(PermissionStatusReader.read(context, hasRequested = false))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        hasRequested = true
        status = PermissionStatusReader.read(context, hasRequested = true)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = PermissionStatusReader.read(context, hasRequested)
    }

    when (status) {
        CameraPermissionStatus.GRANTED -> CameraContent(
            modifier = modifier,
            benchmark = benchmark,
        )

        CameraPermissionStatus.NOT_REQUESTED,
        CameraPermissionStatus.DENIED,
        -> PermissionRequired(
            modifier = modifier,
            title = stringResource(R.string.permission_title),
            body = if (status == CameraPermissionStatus.DENIED) {
                stringResource(R.string.permission_denied_body)
            } else {
                stringResource(R.string.permission_body)
            },
            actionLabel = stringResource(R.string.permission_allow),
            onAction = { launcher.launch(android.Manifest.permission.CAMERA) },
        )

        CameraPermissionStatus.PERMANENTLY_DENIED -> PermissionRequired(
            modifier = modifier,
            title = stringResource(R.string.permission_title),
            body = stringResource(R.string.permission_settings_hint),
            actionLabel = stringResource(R.string.permission_open_settings),
            onAction = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )
    }
}

@Composable
private fun CameraContent(
    modifier: Modifier = Modifier,
    benchmark: BenchmarkRecorder? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val controller = remember(previewView, lifecycleOwner, benchmark) {
        CameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            benchmark = benchmark,
        )
    }
    val state by controller.state.collectAsStateWithLifecycle()
    val onLongClick: (() -> Unit)? = if (benchmark == null) {
        null
    } else {
        ({ controller.toggleBenchmark() })
    }

    LifecycleStartEffect(controller) {
        controller.start()
        onStopOrDispose { controller.stop() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        HandOverlay(
            hands = state.hands,
            imageWidth = state.imageWidth,
            imageHeight = state.imageHeight,
            modifier = Modifier.fillMaxSize(),
        )

        TrackingStatusChip(
            state = state,
            onLongClick = onLongClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        if (benchmark != null) {
            val benchmarkState by benchmark.state.collectAsStateWithLifecycle()
            BenchmarkBanner(
                state = benchmarkState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            )
        }
    }
}

/** Debug-only feedback. Never composed in release because no recorder exists there. */
@Composable
private fun BenchmarkBanner(
    state: BenchmarkState,
    modifier: Modifier = Modifier,
) {
    val text = when (state) {
        BenchmarkState.Idle -> return
        is BenchmarkState.Running -> stringResource(R.string.benchmark_running)
        is BenchmarkState.Completed -> stringResource(
            R.string.benchmark_saved,
            state.processedFrames,
            state.jsonFile.substringAfterLast('/'),
        )

        is BenchmarkState.Failed -> stringResource(R.string.benchmark_failed, state.message)
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFF4F1F6),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun PermissionRequired(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 360.dp),
        )
        Button(
            onClick = onAction,
            modifier = Modifier
                .padding(top = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 320.dp),
        ) {
            Text(text = actionLabel)
        }
    }
}
