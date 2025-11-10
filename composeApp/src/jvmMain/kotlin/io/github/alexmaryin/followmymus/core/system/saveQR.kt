package io.github.alexmaryin.followmymus.core.system

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame

actual class FileHandler actual constructor() {
    actual suspend fun saveQR(image: ByteArray) {
        val fileDialog = FileDialog(null as Frame?, "Save QR image", FileDialog.SAVE)
        fileDialog.file = QR_FILE
        fileDialog.isVisible = true
        val selectedFile = fileDialog.files.firstOrNull() ?: return
        selectedFile.writeBytes(image)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(selectedFile)
        }
    }
}