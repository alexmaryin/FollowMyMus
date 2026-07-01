package io.github.alexmaryin.followmymus.core.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.qr_share_title
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.resume
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.coroutines.resume

actual class FileHandler actual constructor() : KoinComponent {

    private val context: Context by inject()

    companion object {
        private var currentActivity: Activity? = null

        fun bindActivity(activity: Activity) {
            currentActivity = activity
        }
    }

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

    actual suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String? {
        val activity = currentActivity ?: return null
        return suspendCancellableCoroutine { cont ->
            val launcher = activity.activityResultRegistry.register(
                "save_file_${System.nanoTime()}",
                ActivityResultContracts.CreateDocument(mimeType)
            ) { uri: Uri? ->
                if (uri != null) {
                    activity.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(data)
                    }
                    cont.resume(uri.toString())
                } else {
                    cont.resume(null)
                }
            }
            activity.runOnUiThread { launcher.launch(suggestedName) }
        }
    }

    actual suspend fun openFile(mimeType: String): Pair<String, ByteArray>? {
        val activity = currentActivity ?: return null
        return suspendCancellableCoroutine { cont ->
            val launcher = activity.activityResultRegistry.register(
                "open_file_${System.nanoTime()}",
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    val bytes = activity.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes()
                    }
                    cont.resume(bytes?.let { Pair(uri.toString(), it) })
                } else {
                    cont.resume(null)
                }
            }
            activity.runOnUiThread { launcher.launch(arrayOf(mimeType)) }
        }
    }
}