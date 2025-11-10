package io.github.alexmaryin.followmymus.core.system

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class FileHandler actual constructor() {
    actual suspend fun saveQR(image: ByteArray) {
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir/$QR_FILE"
        image.toNSData().writeToFile(filePath, true)
        val fileUrl = NSURL.fileURLWithPath(filePath)
        val activityVC = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null
        )
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, true, null)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData() = memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
    }
}
