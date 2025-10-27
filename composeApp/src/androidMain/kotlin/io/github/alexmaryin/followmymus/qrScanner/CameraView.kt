package io.github.alexmaryin.followmymus.qrScanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { viewContext ->
            val previewView = PreviewView(viewContext)
            val preview = Preview.Builder().build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .apply {
                    setAnalyzer(ContextCompat.getMainExecutor(viewContext)) { imageProxy ->
                        processImageProxy(imageProxy, onQrScanned)
                    }
                }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalyzer)
                preview.surfaceProvider = previewView.surfaceProvider
            }, ContextCompat.getMainExecutor(viewContext))

            previewView
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(cameraProviderFuture) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }
}