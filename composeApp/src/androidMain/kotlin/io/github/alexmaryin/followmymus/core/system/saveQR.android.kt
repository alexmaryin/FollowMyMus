package io.github.alexmaryin.followmymus.core.system

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.qr_share_title
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual class FileHandler actual constructor() : KoinComponent {

    private val context: Context by inject()

    actual suspend fun saveQR(image: ByteArray) {
        val file = File(context.cacheDir, QR_FILE)
        file.writeBytes(image)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = QR_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(
            shareIntent,
            getString(Res.string.qr_share_title)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        context.startActivity(chooser)
    }
}