package io.github.alexmaryin.followmymus.core.system

expect class FileHandler() {
    suspend fun saveQR(image: ByteArray)
    suspend fun saveFile(suggestedName: String, mimeType: String, data: ByteArray): String?
    suspend fun openFile(mimeType: String): Pair<String, ByteArray>?
}

const val QR_FILE = "qrcode_followmymus.png"
const val QR_MIME_TYPE = "image/png"