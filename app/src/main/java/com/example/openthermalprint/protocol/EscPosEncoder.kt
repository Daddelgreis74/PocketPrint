package com.example.openthermalprint.protocol

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

enum class DitherMode {
    FLOYD_STEINBERG, // Smooth error diffusion for realistic photos
    BAYER_ORDERED,   // Retro structured halftoning for comics & graphics
    THRESHOLD        // Hard black & white cutoff for logos, text & barcodes
}

object EscPosEncoder {

    // Standard ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @ (Initialize printer)
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)

    enum class TextAlign {
        LEFT, CENTER, RIGHT
    }

    // Standard Bayer 4x4 matrix scaled for 0..255 threshold
    private val BAYER_4X4 = arrayOf(
        intArrayOf(0, 128, 32, 160),
        intArrayOf(192, 64, 224, 96),
        intArrayOf(48, 176, 16, 144),
        intArrayOf(240, 112, 208, 80)
    )

    /**
     * Set print density (WalkPrint proprietary ESC #N command)
     * density: 0 (light = 12), 1 (medium = 18), 2 (dark = 25)
     */
    fun setDensity(density: Int): ByteArray {
        val densityVal = when (density) {
            0 -> 12.toByte()
            1 -> 18.toByte()
            else -> 25.toByte()
        }
        return byteArrayOf(0x1B, 0x23, 0x4E, densityVal)
    }

    /**
     * Line feed paper by N lines (ESC d n)
     */
    fun feedPaper(lines: Int = 8): ByteArray {
        val count = lines.coerceIn(1, 100).toByte()
        return byteArrayOf(0x1B, 0x64, count)
    }

    /**
     * Adjust brightness (-100..+100) and contrast (0.5..2.0) on a Bitmap
     */
    fun adjustBrightnessAndContrast(source: Bitmap, brightness: Float = 0f, contrast: Float = 1.0f): Bitmap {
        if (brightness == 0f && contrast == 1.0f) return source

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * Renders arbitrary Unicode text to a crisp 384-dot monochrome bitmap.
     */
    fun renderTextToBitmap(
        text: String,
        targetWidth: Int = 384,
        textSizeSp: Float = 24f,
        align: TextAlign = TextAlign.LEFT,
        bold: Boolean = false,
        doubleSize: Boolean = false
    ): Bitmap {
        val effectiveSize = if (doubleSize) textSizeSp * 1.5f else textSizeSp
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = effectiveSize
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        val textPaint = TextPaint(paint)
        val padding = 12
        val contentWidth = (targetWidth - (padding * 2)).coerceAtLeast(100)

        val textAlignment = when (align) {
            TextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
            TextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
                .setAlignment(textAlignment)
                .setLineSpacing(6f, 1.15f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textPaint, contentWidth, textAlignment, 1.15f, 6f, true)
        }

        val height = (staticLayout.height + padding * 2).coerceAtLeast(32)
        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        canvas.save()
        canvas.translate(padding.toFloat(), padding.toFloat())
        staticLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    /**
     * Format raw text into ESC/POS printable bytes (legacy text mode fallback)
     */
    fun formatText(
        text: String,
        align: TextAlign = TextAlign.LEFT,
        bold: Boolean = false,
        doubleSize: Boolean = false,
        feedAfter: Int = 8
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        when (align) {
            TextAlign.LEFT -> stream.write(CMD_ALIGN_LEFT)
            TextAlign.CENTER -> stream.write(CMD_ALIGN_CENTER)
            TextAlign.RIGHT -> stream.write(CMD_ALIGN_RIGHT)
        }

        if (bold) stream.write(CMD_BOLD_ON) else stream.write(CMD_BOLD_OFF)

        if (doubleSize) {
            stream.write(byteArrayOf(0x1D, 0x21, 0x11))
        } else {
            stream.write(byteArrayOf(0x1D, 0x21, 0x00))
        }

        val textBytes = try {
            text.toByteArray(Charset.forName("GB2312"))
        } catch (_: Exception) {
            text.toByteArray(Charsets.UTF_8)
        }
        stream.write(textBytes)
        stream.write("\n".toByteArray())

        if (feedAfter > 0) {
            stream.write(feedPaper(feedAfter))
        }

        return stream.toByteArray()
    }

    /**
     * Convert an Android Bitmap into standard ESC/POS GS v 0 raster bytes
     * Supports Floyd-Steinberg, Bayer Matrix, and Threshold dithering modes.
     */
    fun bitmapToEscPosRaster(
        source: Bitmap,
        targetWidth: Int = 384,
        ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
        brightness: Float = 0f,
        contrast: Float = 1.0f,
        feedLines: Int = 8
    ): ByteArray {
        // Apply brightness / contrast if modified
        val preAdjusted = adjustBrightnessAndContrast(source, brightness, contrast)

        val aspect = preAdjusted.height.toFloat() / preAdjusted.width.toFloat()
        val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(preAdjusted, targetWidth, targetHeight, true)
        val width = scaled.width
        val height = scaled.height
        val widthBytes = width / 8

        val gray = Array(width) { IntArray(height) }
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                gray[x][y] = ((r * 38) + (g * 75) + (b * 15)) shr 7
            }
        }

        // Output matrix (true = black dot, false = white)
        val blackMatrix = Array(width) { BooleanArray(height) }

        when (ditherMode) {
            DitherMode.FLOYD_STEINBERG -> {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val oldPixel = gray[x][y]
                        val isBlack = oldPixel < 128
                        blackMatrix[x][y] = isBlack
                        val newPixel = if (isBlack) 0 else 255
                        val quantError = oldPixel - newPixel

                        if (y + 1 < height) {
                            gray[x][y + 1] = (gray[x][y + 1] + (quantError * 7) / 16).coerceIn(0, 255)
                        }
                        if (x + 1 < width && y - 1 >= 0) {
                            gray[x + 1][y - 1] = (gray[x + 1][y - 1] + (quantError * 3) / 16).coerceIn(0, 255)
                        }
                        if (x + 1 < width) {
                            gray[x + 1][y] = (gray[x + 1][y] + (quantError * 5) / 16).coerceIn(0, 255)
                        }
                        if (x + 1 < width && y + 1 < height) {
                            gray[x + 1][y + 1] = (gray[x + 1][y + 1] + (quantError * 1) / 16).coerceIn(0, 255)
                        }
                    }
                }
            }
            DitherMode.BAYER_ORDERED -> {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val threshold = BAYER_4X4[x % 4][y % 4]
                        blackMatrix[x][y] = gray[x][y] < threshold
                    }
                }
            }
            DitherMode.THRESHOLD -> {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        blackMatrix[x][y] = gray[x][y] < 128
                    }
                }
            }
        }

        val stream = ByteArrayOutputStream()

        stream.write(CMD_INIT)
        stream.write(CMD_ALIGN_CENTER)

        val xL = (widthBytes % 256).toByte()
        val xH = (widthBytes / 256).toByte()
        val yL = (height % 256).toByte()
        val yH = (height / 256).toByte()

        stream.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

        val rowBuffer = ByteArray(widthBytes)
        for (y in 0 until height) {
            for (byteIdx in 0 until widthBytes) {
                val startX = byteIdx * 8
                var rowByte = 0
                for (bit in 0..7) {
                    val px = startX + bit
                    if (blackMatrix[px][y]) {
                        rowByte = rowByte or (1 shl (7 - bit))
                    }
                }
                rowBuffer[byteIdx] = rowByte.toByte()
            }
            stream.write(rowBuffer)
        }

        if (feedLines > 0) {
            stream.write(feedPaper(feedLines))
        }

        return stream.toByteArray()
    }
}
