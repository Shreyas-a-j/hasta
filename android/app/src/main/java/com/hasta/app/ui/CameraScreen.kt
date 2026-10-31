package com.hasta.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasta.app.R
import com.hasta.app.data.benchmark.BenchmarkRecorder
import com.hasta.app.data.benchmark.BenchmarkState
import com.hasta.app.ui.component.HandOverlay
import com.hasta.app.ui.permission.CameraPermissionStatus
import com.hasta.app.ui.permission.PermissionStatusReader
import com.hasta.app.ui.theme.HastaCameraTheme
import com.hasta.app.ui.theme.HastaRadius
import com.hasta.app.ui.theme.HastaTheme
import kotlinx.coroutines.delay

/**
 * Hosts the camera preview or the permission recovery UI.
 *
 * The permission state is re-read from the platform on every resume, so a grant
 * made through Android Settings is picked up without restarting the app.
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    benchmark: BenchmarkRecorder? = null,
) {
    val context = LocalContext.current
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(PermissionStatusReader.read(context, hasRequested = hasRequested))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        hasRequested = true
        status = PermissionStatusReader.read(context, hasRequested = true)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = PermissionStatusReader.read(context, hasRequested = hasRequested)
    }

    val openSettings: () -> Unit = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    when (status) {
        CameraPermissionStatus.GRANTED -> HastaCameraTheme {
            SystemBarAppearance(lightIcons = false)
            CameraContent(
                modifier = modifier,
                benchmark = benchmark,
            )
        }

        CameraPermissionStatus.NOT_REQUESTED,
        CameraPermissionStatus.DENIED,
        -> {
            SystemBarAppearance(lightIcons = !HastaTheme.colors.isDark)
            PermissionScreen(
                kind = if (status == CameraPermissionStatus.NOT_REQUESTED) {
                    PermissionScreenKind.Rationale
                } else {
                    PermissionScreenKind.Denied
                },
                onAllow = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = openSettings,
                modifier = modifier,
            )
        }

        CameraPermissionStatus.PERMANENTLY_DENIED -> {
            SystemBarAppearance(lightIcons = !HastaTheme.colors.isDark)
            PermissionScreen(
                kind = PermissionScreenKind.PermanentlyDenied,
                onAllow = { launcher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = openSettings,
                modifier = modifier,
            )
        }
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
    val tracking by controller.tracking.collectAsStateWithLifecycle()
    val reducedMotion = HastaTheme.motion.reduced

    var practice by rememberSaveable(stateSaver = PracticeSaver) {
        mutableStateOf(PracticeUiState())
    }

    LaunchedEffect(practice.phase, reducedMotion) {
        if (practice.phase == PracticePhase.Running) {
            val tick = if (reducedMotion) 1_000L else 100L
            while (true) {
                delay(tick)
                practice = practice.advance(tick)
            }
        }
    }

    LifecycleStartEffect(controller) {
        controller.start()
        onStopOrDispose { controller.stop() }
    }

    val onAction: (CoachAction) -> Unit = { action ->
        when (action) {
            CoachAction.StartPractice -> practice = practice.start()
            CoachAction.StopPractice -> practice = practice.stop()
            CoachAction.PractiseAgain -> practice = practice.again()
            CoachAction.RetryTracking,
            CoachAction.RetryCamera,
            -> {
                practice = practice.stop()
                controller.restart()
            }

            CoachAction.AllowCamera,
            CoachAction.OpenSettings,
            -> Unit
        }
    }

    val onLongClick: (() -> Unit)? = if (benchmark == null) {
        null
    } else {
        ({ controller.toggleBenchmark() })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiveCoachScreen(
            state = LiveCoachUiState(tracking = tracking, practice = practice),
            onAction = onAction,
            stage = {
                CameraStage(
                    controller = controller,
                    previewView = previewView,
                )
            },
            modifier = modifier,
            debugOnLongClick = onLongClick,
        )

        if (benchmark != null) {
            val benchmarkState by benchmark.state.collectAsStateWithLifecycle()
            BenchmarkBanner(
                state = benchmarkState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}

/**
 * Owns the high-frequency frame collector. Keeping it in its own composable means
 * landmark updates recompose the preview/overlay, not the coach panel.
 */
@Composable
private fun CameraStage(
    controller: CameraController,
    previewView: PreviewView,
    modifier: Modifier = Modifier,
) {
    val frame by controller.frameState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        val stageDescription = stringResource(R.string.a11y_camera_stage)
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = stageDescription },
        )
        HandOverlay(
            hands = frame.hands,
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val PracticeSaver = listSaver<PracticeUiState, Any>(
    save = {
        listOf(it.phase.name, it.elapsedMillis, it.windowMillis)
    },
    restore = {
        PracticeUiState(
            phase = PracticePhase.valueOf(it[0] as String),
            windowMillis = it[2] as Long,
            elapsedMillis = it[1] as Long,
        )
    },
)

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
        color = HastaTheme.colors.cameraPanel,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = HastaTheme.colors.cameraText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
