package io.github.alexmaryin.followmymus.core.system

expect class FileHandler() {
    suspend fun saveQR(image: ByteArray)
}

const val QR_FILE = "qrcode_followmymus.png"
const val QR_MIME_TYPE = "image/png"