package com.example.gpgrocery.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gpgrocery.R
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.ui.theme.Crate
import com.example.gpgrocery.ui.theme.CrateIcons
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch

/** The store's snackbar, for "Product saved" and the like, whichever screen asks. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Which screen, if any, is waiting for a barcode. Shared by the store's screens and drawn by [BarcodeScannerHost]. */
@Stable
class BarcodeScannerState {
    internal var request: ((String) -> Unit)? by mutableStateOf(null)
        private set

    fun open(onScanned: (String) -> Unit) {
        request = onScanned
    }

    internal fun close() {
        request = null
    }
}

val LocalBarcodeScanner = staticCompositionLocalOf { BarcodeScannerState() }

/** Returns what to call to scan a barcode; [onScanned] gets it, already in its usual form. */
@Composable
fun rememberBarcodeScanner(onScanned: (String) -> Unit): () -> Unit {
    val scanner = LocalBarcodeScanner.current
    val latest by rememberUpdatedState(onScanned)
    return remember(scanner) { { scanner.open { latest(it) } } }
}

/**
 * The scanner, drawn over the store's screens while one of them waits for a barcode. Barcodes are
 * read on the phone by ML Kit's bundled model, so it works offline and needs nothing downloaded
 * from Google Play. The camera permission is asked for the first time it opens.
 */
@Composable
fun BarcodeScannerHost(state: BarcodeScannerState) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val needsCamera = stringResource(R.string.scanner_permission)
    val openSettings = stringResource(R.string.scanner_settings)
    val noCamera = stringResource(R.string.scanner_unavailable)
    var justGranted by remember { mutableStateOf(false) }
    val askForCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            justGranted = true
        } else {
            state.close()
            scope.launch {
                val choice = snackbar.showSnackbar(needsCamera, actionLabel = openSettings, duration = SnackbarDuration.Long)
                if (choice == SnackbarResult.ActionPerformed) context.startActivity(appSettings(context))
            }
        }
    }

    val request = state.request ?: return
    when {
        !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> LaunchedEffect(request) {
            scope.launch { snackbar.showSnackbar(noCamera) }
            state.close()
        }
        justGranted || hasCamera(context) -> CameraScanner(
            onScanned = { code ->
                state.close()
                request(code)
            },
            onClose = state::close,
        )
        else -> LaunchedEffect(request) { askForCamera.launch(Manifest.permission.CAMERA) }
    }
}

private fun hasCamera(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun appSettings(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

@Composable
private fun CameraScanner(onScanned: (String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val latest by rememberUpdatedState(onScanned)
    val hasLight = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) }
    var lightOn by remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val executor = ContextCompat.getMainExecutor(context)
        val reader = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                )
                .build(),
        )
        var done = false
        controller.setImageAnalysisAnalyzer(
            executor,
            MlKitAnalyzer(listOf(reader), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
                val code = result.getValue(reader)?.firstNotNullOfOrNull { it.rawValue }
                // Frames keep arriving after the first read; only one barcode goes back.
                if (code != null && !done) {
                    done = true
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    latest(Barcodes.normalize(code))
                }
            },
        )
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            reader.close()
        }
    }
    LaunchedEffect(lightOn) { controller.enableTorch(lightOn) }
    BackHandler(onBack = onClose)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    this.controller = controller
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Viewfinder(Modifier.fillMaxSize())
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircleIconButton(CrateIcons.Close, stringResource(R.string.scanner_close), onClose, containerColor = Scrim, contentColor = Color.White)
            Text(
                stringResource(R.string.scanner_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (hasLight) {
                CircleIconButton(
                    CrateIcons.Flash,
                    stringResource(if (lightOn) R.string.scanner_light_off else R.string.scanner_light_on),
                    { lightOn = !lightOn },
                    containerColor = if (lightOn) Color.White else Scrim,
                    contentColor = if (lightOn) Color.Black else Color.White,
                )
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Text(
            stringResource(R.string.scanner_hint),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 56.dp)
                .background(Scrim, MaterialTheme.shapes.large)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

private val Scrim = Color.Black.copy(alpha = 0.55f)

/** Dims the camera around a barcode-shaped window, with a line sweeping across it. */
@Composable
private fun Viewfinder(modifier: Modifier) {
    val accent = Crate.colors.accent
    val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    Canvas(modifier) {
        val window = window()
        val corner = CornerRadius(24.dp.toPx())
        val dimmed = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(RoundRect(window, corner))
        }
        drawPath(dimmed, Scrim)
        drawCorners(window, 24.dp.toPx(), 34.dp.toPx())
        val y = window.top + window.height * sweep
        drawLine(accent, Offset(window.left + 20.dp.toPx(), y), Offset(window.right - 20.dp.toPx(), y), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** Where barcodes are meant to go: wide and short, a little above the middle. */
private fun DrawScope.window(): Rect {
    val width = minOf(size.width * 0.82f, 380.dp.toPx())
    val height = width * 0.56f
    val left = (size.width - width) / 2
    val top = size.height * 0.42f - height / 2
    return Rect(left, top, left + width, top + height)
}

/** White brackets at the window's corners, rounded like the window. */
private fun DrawScope.drawCorners(window: Rect, radius: Float, arm: Float) {
    val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        // Top left
        moveTo(window.left, window.top + arm)
        lineTo(window.left, window.top + radius)
        quadraticTo(window.left, window.top, window.left + radius, window.top)
        lineTo(window.left + arm, window.top)
        // Top right
        moveTo(window.right - arm, window.top)
        lineTo(window.right - radius, window.top)
        quadraticTo(window.right, window.top, window.right, window.top + radius)
        lineTo(window.right, window.top + arm)
        // Bottom right
        moveTo(window.right, window.bottom - arm)
        lineTo(window.right, window.bottom - radius)
        quadraticTo(window.right, window.bottom, window.right - radius, window.bottom)
        lineTo(window.right - arm, window.bottom)
        // Bottom left
        moveTo(window.left + arm, window.bottom)
        lineTo(window.left + radius, window.bottom)
        quadraticTo(window.left, window.bottom, window.left, window.bottom - radius)
        lineTo(window.left, window.bottom - arm)
    }
    drawPath(path, Color.White, style = stroke)
}
