package io.github.alexmaryin.followmymus.core.system

import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

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

    actual suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String? {
        val fileDialog = FileDialog(null as Frame?, "Save favorites", FileDialog.SAVE)
        fileDialog.file = suggestedName
        fileDialog.isVisible = true
        val selectedFile = fileDialog.files.firstOrNull() ?: return null
        selectedFile.writeBytes(data)
        return selectedFile.absolutePath
    }

    actual suspend fun openFile(mimeType: String): Pair<String, ByteArray>? {
        val dialog = FileDialog(null as Frame?, "Open favorites", FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".json") }
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        val selectedFile = java.io.File(directory, file)
        return Pair(selectedFile.absolutePath, selectedFile.readBytes())
    }
}