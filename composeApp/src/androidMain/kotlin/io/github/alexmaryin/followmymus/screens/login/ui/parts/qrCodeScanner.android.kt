package io.github.alexmaryin.followmymus.screens.login.ui.parts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.alexmaryin.followmymus.screens.login.ui.parts.qrScanner.CameraPermissionRequestScreen
import io.github.alexmaryin.followmymus.screens.login.ui.parts.qrScanner.CameraWithScanner

@Composable
actual fun QRCodeScanner(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraWithScanner(modifier, onQrDetected, onCancel)
    } else {
        CameraPermissionRequestScreen(
            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel
        )
    }
}