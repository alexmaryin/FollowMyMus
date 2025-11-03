package io.github.alexmaryin.followmymus.screens.login.ui.parts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.zxing.BinaryBitmap
import com.google.zxing.NotFoundException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.open_qr_dialog_caption
import followmymus.composeapp.generated.resources.qr_invalid
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import javax.imageio.ImageIO

@Composable
actual fun QRCodeScanner(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    var showError by remember { mutableStateOf(false) }

    Text(
        text = stringResource(Res.string.open_qr_dialog_caption),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )

    AnimatedVisibility(showError) {
        Text(
            text = stringResource(Res.string.qr_invalid),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.error
        )
    }

    LaunchedEffect(showError) {
        if (showError) {
            delay(2000L)
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        val dialog = FileDialog(
            null as Frame?,
            getString(Res.string.open_qr_dialog_caption),
            FileDialog.LOAD
        )
        dialog.filenameFilter = FilenameFilter { _, name ->
            name.endsWith(".png") || name.endsWith(".jpg")
        }
        dialog.isVisible = true

        if (dialog.file != null) {
            val file = java.io.File(dialog.directory, dialog.file)
            try {
                val image = ImageIO.read(file)
                val luminanceSource = BufferedImageLuminanceSource(image)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
                val reader = QRCodeReader()
                val result = reader.decode(binaryBitmap)
                onQrDetected(result.text)
            } catch (_: NotFoundException) {
                showError = true
            } catch (e: Exception) {
                e.printStackTrace()
                onCancel()
            }
        } else {
            onCancel()
        }
    }
}
