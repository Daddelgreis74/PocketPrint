package com.example.openthermalprint.ui.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.openthermalprint.protocol.DitherMode
import com.example.openthermalprint.protocol.EscPosEncoder

enum class CropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1 Quadrat", 1.0f),
    PHOTO_4_3("4:3 Foto", 4f / 3f),
    WIDE_16_9("16:9 Quer", 16f / 9f),
    BANNER_1_2("1:2 Bon", 1f / 2f)
}

enum class PhotoFrame(val label: String) {
    NONE("Ohne"),
    POLAROID("📷 Polaroid"),
    CLASSIC("🖼️ Klassisch"),
    STAMP("✉️ Briefmarke"),
    HEARTS("❤️ Deko-Herzen")
}

enum class CaptionPosition(val label: String) {
    BOTTOM("Unten"),
    TOP("Oben"),
    FOOTER("Polaroid-Fuß")
}

enum class StickerPosition(val label: String) {
    NONE("Ohne"),
    BOTTOM_RIGHT("Unten Rechts"),
    BOTTOM_LEFT("Unten Links"),
    TOP_RIGHT("Oben Rechts"),
    TOP_LEFT("Oben Links"),
    CENTER("Mitte")
}

object PhotoEditorHelper {

    /**
     * Rotiert ein Bitmap und spiegelt es optional horizontal.
     */
    fun transformBitmap(source: Bitmap, rotationDegrees: Int, flipHorizontal: Boolean): Bitmap {
        if (rotationDegrees == 0 && !flipHorizontal) return source

        val matrix = Matrix().apply {
            if (rotationDegrees != 0) {
                postRotate(rotationDegrees.toFloat())
            }
            if (flipHorizontal) {
                postScale(-1f, 1f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Schneidet das Bitmap mittig auf das gewünschte Seitenverhältnis zu.
     */
    fun cropToAspect(source: Bitmap, aspect: CropAspect): Bitmap {
        val targetRatio = aspect.ratio ?: return source
        val currentRatio = source.width.toFloat() / source.height.toFloat()

        return if (currentRatio > targetRatio) {
            // Bild ist breiter als gewünscht -> links/rechts abschneiden
            val newWidth = (source.height * targetRatio).toInt().coerceIn(1, source.width)
            val xOffset = (source.width - newWidth) / 2
            Bitmap.createBitmap(source, xOffset, 0, newWidth, source.height)
        } else if (currentRatio < targetRatio) {
            // Bild ist höher als gewünscht -> oben/unten abschneiden
            val newHeight = (source.width / targetRatio).toInt().coerceIn(1, source.height)
            val yOffset = (source.height - newHeight) / 2
            Bitmap.createBitmap(source, 0, yOffset, source.width, newHeight)
        } else {
            source
        }
    }

    /**
     * Wendet Rahmen, Bildunterschrift und Sticker/Emoji auf das Bitmap an.
     */
    fun applyFrameAndDecorations(
        source: Bitmap,
        frame: PhotoFrame,
        captionText: String,
        captionPos: CaptionPosition,
        stickerEmoji: String,
        stickerPos: StickerPosition
    ): Bitmap {
        val baseWidth = source.width
        val baseHeight = source.height

        val (paddingLeft, paddingTop, paddingRight, paddingBottom) = when (frame) {
            PhotoFrame.NONE -> listOf(0, 0, 0, 0)
            PhotoFrame.POLAROID -> {
                val padSide = (baseWidth * 0.08f).toInt().coerceAtLeast(16)
                val padBottom = (baseWidth * 0.28f).toInt().coerceAtLeast(64)
                listOf(padSide, padSide, padSide, padBottom)
            }
            PhotoFrame.CLASSIC -> {
                val pad = (baseWidth * 0.06f).toInt().coerceAtLeast(14)
                listOf(pad, pad, pad, pad)
            }
            PhotoFrame.STAMP -> {
                val pad = (baseWidth * 0.07f).toInt().coerceAtLeast(16)
                listOf(pad, pad, pad, pad)
            }
            PhotoFrame.HEARTS -> {
                val pad = (baseWidth * 0.06f).toInt().coerceAtLeast(14)
                listOf(pad, pad, pad, pad)
            }
        }

        val totalWidth = baseWidth + paddingLeft + paddingRight
        val totalHeight = baseHeight + paddingTop + paddingBottom

        val result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Hintergrund füllen
        canvas.drawColor(Color.WHITE)

        // Bild einzeichnen
        canvas.drawBitmap(source, paddingLeft.toFloat(), paddingTop.toFloat(), null)

        val imageRect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (paddingLeft + baseWidth).toFloat(),
            (paddingTop + baseHeight).toFloat()
        )

        // Spezifische Rahmenelemente zeichnen
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        when (frame) {
            PhotoFrame.NONE -> { /* kein Rahmen */ }
            PhotoFrame.POLAROID -> {
                // Feine Umrandung um das Foto selbst
                borderPaint.strokeWidth = 2f
                canvas.drawRect(imageRect, borderPaint)
                // Äußere Kante
                borderPaint.strokeWidth = 3f
                canvas.drawRect(1f, 1f, totalWidth - 1f, totalHeight - 1f, borderPaint)
            }
            PhotoFrame.CLASSIC -> {
                // Äußere Linie
                borderPaint.strokeWidth = 4f
                canvas.drawRect(4f, 4f, totalWidth - 4f, totalHeight - 4f, borderPaint)
                // Innere dünne Linie
                borderPaint.strokeWidth = 1.5f
                val innerGap = (paddingLeft * 0.45f)
                canvas.drawRect(
                    innerGap, innerGap,
                    totalWidth - innerGap, totalHeight - innerGap,
                    borderPaint
                )
            }
            PhotoFrame.STAMP -> {
                // Gezackter Briefmarken-Rand
                val notchPaint = Paint().apply {
                    color = Color.WHITE
                    isAntiAlias = true
                    style = Paint.Style.FILL
                }
                borderPaint.strokeWidth = 2.5f
                canvas.drawRect(8f, 8f, totalWidth - 8f, totalHeight - 8f, borderPaint)

                // Briefmarken-Lochung
                val radius = (totalWidth * 0.022f).coerceIn(6f, 18f)
                val step = radius * 2.8f
                // Horizontale Lochung oben und unten
                var x = step / 2
                while (x < totalWidth) {
                    canvas.drawCircle(x, 0f, radius, notchPaint)
                    canvas.drawCircle(x, totalHeight.toFloat(), radius, notchPaint)
                    x += step
                }
                // Vertikale Lochung links und rechts
                var y = step / 2
                while (y < totalHeight) {
                    canvas.drawCircle(0f, y, radius, notchPaint)
                    canvas.drawCircle(totalWidth.toFloat(), y, radius, notchPaint)
                    y += step
                }
            }
            PhotoFrame.HEARTS -> {
                borderPaint.strokeWidth = 2.5f
                canvas.drawRect(6f, 6f, totalWidth - 6f, totalHeight - 6f, borderPaint)

                val heartPaint = Paint().apply {
                    textSize = (totalWidth * 0.055f).coerceIn(16f, 32f)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                // Deko-Symbole in den 4 Ecken
                val hOffset = heartPaint.textSize * 0.8f
                canvas.drawText("♥", hOffset, hOffset, heartPaint)
                canvas.drawText("♥", totalWidth - hOffset, hOffset, heartPaint)
                canvas.drawText("♥", hOffset, totalHeight - hOffset * 0.4f, heartPaint)
                canvas.drawText("♥", totalWidth - hOffset, totalHeight - hOffset * 0.4f, heartPaint)
            }
        }

        // Caption / Text zeichnen
        if (captionText.isNotBlank()) {
            drawCaption(canvas, totalWidth, totalHeight, imageRect, captionText, captionPos, frame == PhotoFrame.POLAROID)
        }

        // Sticker / Emoji zeichnen
        if (stickerEmoji.isNotBlank() && stickerPos != StickerPosition.NONE) {
            drawSticker(canvas, totalWidth, totalHeight, imageRect, stickerEmoji, stickerPos)
        }

        return result
    }

    private fun drawCaption(
        canvas: Canvas,
        totalWidth: Int,
        totalHeight: Int,
        imageRect: RectF,
        text: String,
        position: CaptionPosition,
        isPolaroid: Boolean
    ) {
        val paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = (totalWidth * 0.052f).coerceIn(18f, 34f)
            typeface = if (isPolaroid && position == CaptionPosition.FOOTER) {
                Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            } else {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }

        when {
            isPolaroid && position == CaptionPosition.FOOTER -> {
                // Im breiten Polaroid-Fuß zentriert platzieren
                val footerCenterY = imageRect.bottom + (totalHeight - imageRect.bottom) / 2f + (paint.textSize / 3f)
                canvas.drawText(text, totalWidth / 2f, footerCenterY, paint)
            }
            position == CaptionPosition.TOP -> {
                // Oben auf dem Bild mit halbtransparentem Banner-Hintergrund
                val bannerHeight = paint.textSize * 1.6f
                val bannerRect = RectF(imageRect.left, imageRect.top, imageRect.right, imageRect.top + bannerHeight)
                val bgPaint = Paint().apply {
                    color = Color.argb(190, 255, 255, 255)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(bannerRect, bgPaint)
                canvas.drawText(text, totalWidth / 2f, imageRect.top + bannerHeight * 0.72f, paint)
            }
            else -> {
                // Unten auf dem Bild
                val bannerHeight = paint.textSize * 1.6f
                val bannerRect = RectF(imageRect.left, imageRect.bottom - bannerHeight, imageRect.right, imageRect.bottom)
                val bgPaint = Paint().apply {
                    color = Color.argb(190, 255, 255, 255)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(bannerRect, bgPaint)
                canvas.drawText(text, totalWidth / 2f, imageRect.bottom - bannerHeight * 0.28f, paint)
            }
        }
    }

    private fun drawSticker(
        canvas: Canvas,
        totalWidth: Int,
        totalHeight: Int,
        imageRect: RectF,
        emoji: String,
        position: StickerPosition
    ) {
        val stickerSize = (totalWidth * 0.14f).coerceIn(32f, 64f)
        val paint = Paint().apply {
            textSize = stickerSize
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val margin = stickerSize * 0.7f
        val (x, y) = when (position) {
            StickerPosition.NONE -> return
            StickerPosition.TOP_LEFT -> Pair(imageRect.left + margin, imageRect.top + margin)
            StickerPosition.TOP_RIGHT -> Pair(imageRect.right - margin, imageRect.top + margin)
            StickerPosition.BOTTOM_LEFT -> Pair(imageRect.left + margin, imageRect.bottom - margin * 0.3f)
            StickerPosition.BOTTOM_RIGHT -> Pair(imageRect.right - margin, imageRect.bottom - margin * 0.3f)
            StickerPosition.CENTER -> Pair(totalWidth / 2f, totalHeight / 2f + stickerSize * 0.35f)
        }

        canvas.drawText(emoji, x, y, paint)
    }

    /**
     * Erzeugt eine 1:1 Live-Druckvorschau (Monochrom Thermopapier-Optik)
     * basierend auf dem gewählten Rastermodus und Helligkeit/Kontrast.
     */
    fun createDitherPreview(
        source: Bitmap,
        targetWidth: Int = 384,
        ditherMode: DitherMode = DitherMode.FLOYD_STEINBERG,
        brightness: Float = 0f,
        contrast: Float = 1.0f,
        invert: Boolean = false
    ): Bitmap {
        val adjusted = EscPosEncoder.adjustBrightnessAndContrast(source, brightness, contrast)
        val aspect = adjusted.height.toFloat() / adjusted.width.toFloat()
        val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(adjusted, targetWidth, targetHeight, true)
        val width = scaled.width
        val height = scaled.height

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

        val blackMatrix = Array(width) { BooleanArray(height) }
        val bayerMatrix = arrayOf(
            intArrayOf(0, 128, 32, 160),
            intArrayOf(192, 64, 224, 96),
            intArrayOf(48, 176, 16, 144),
            intArrayOf(240, 112, 208, 80)
        )

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
                        val threshold = bayerMatrix[x % 4][y % 4]
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

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var isDot = blackMatrix[x][y]
                if (invert) isDot = !isDot
                // Thermopapier-Look: Schwarz auf Weiß
                pixels[y * width + x] = if (isDot) Color.BLACK else Color.WHITE
            }
        }

        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
