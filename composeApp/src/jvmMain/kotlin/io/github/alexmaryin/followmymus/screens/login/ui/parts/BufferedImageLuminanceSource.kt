package io.github.alexmaryin.followmymus.screens.login.ui.parts

import com.google.zxing.LuminanceSource
import java.awt.image.BufferedImage

class BufferedImageLuminanceSource(image: BufferedImage) :
    LuminanceSource(image.width, image.height) {

    private val luminances: ByteArray

    init {
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)

        luminances = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                // standard luminance formula
                luminances[y * width + x] = ((r + g + g + b) shr 2).toByte()
            }
        }
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val start = y * width
        return (row ?: ByteArray(width)).apply {
            System.arraycopy(luminances, start, this, 0, width)
        }
    }

    override fun getMatrix(): ByteArray = luminances
}
