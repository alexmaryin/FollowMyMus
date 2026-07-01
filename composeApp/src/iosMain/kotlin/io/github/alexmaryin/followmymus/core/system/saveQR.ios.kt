package io.github.alexmaryin.followmymus.core.system

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import platform.objc.objc_setAssociatedObject
import platform.posix.memcpy
import kotlin.coroutines.resume

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

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String? {
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir/$suggestedName"
        data.toNSData().writeToFile(filePath, true)
        val fileUrl = NSURL.fileURLWithPath(filePath)

        return suspendCancellableCoroutine { cont ->
            val picker = UIDocumentPickerViewController(
                forExportingURLs = listOf(fileUrl)
            )
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    cont.resume(url?.path)
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    cont.resume(null)
                }
            }
            picker.delegate = delegate
            objc_setAssociatedObject(picker, "delegate", delegate, 1u)
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(picker, true, null)
        }
    }

    actual suspend fun openFile(mimeType: String): Pair<String, ByteArray>? {
        return suspendCancellableCoroutine { cont ->
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeJSON)
            )
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    val path = url?.path
                    if (path != null) {
                        val data = NSData.dataWithContentsOfFile(path)
                        val bytes = data?.toByteArray()
                        cont.resume(bytes?.let { Pair(path, it) })
                    } else {
                        cont.resume(null)
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    cont.resume(null)
                }
            }
            picker.delegate = delegate
            objc_setAssociatedObject(picker, "delegate", delegate, 1u)
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootVC?.presentViewController(picker, true, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData() = memScoped {
        NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
            }
        }
        return bytes
    }
}
