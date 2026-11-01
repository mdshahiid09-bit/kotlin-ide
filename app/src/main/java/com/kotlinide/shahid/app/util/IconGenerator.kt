package com.kotlinide.shahid.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

object IconGenerator {

    data class DensityInfo(val folder: String, val size: Int)

    val DENSITIES = listOf(
        DensityInfo("mipmap-mdpi", 48),
        DensityInfo("mipmap-hdpi", 72),
        DensityInfo("mipmap-xhdpi", 96),
        DensityInfo("mipmap-xxhdpi", 144),
        DensityInfo("mipmap-xxxhdpi", 192)
    )

    fun generateMipmapIcons(sourceBitmap: Bitmap?): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()

        for (density in DENSITIES) {
            val squareBitmap = if (sourceBitmap != null) {
                Bitmap.createScaledBitmap(sourceBitmap, density.size, density.size, true)
            } else {
                createDefaultSquareBitmap(density.size)
            }

            val roundBitmap = if (sourceBitmap != null) {
                createCircularBitmap(squareBitmap)
            } else {
                createDefaultRoundBitmap(density.size)
            }

            result["app/src/main/res/${density.folder}/ic_launcher.png"] = toPngByteArray(squareBitmap)
            result["app/src/main/res/${density.folder}/ic_launcher_round.png"] = toPngByteArray(roundBitmap)
        }

        return result
    }

    fun createCircularBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)
        return output
    }

    private fun createDefaultSquareBitmap(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark background (#121212)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121212")
            style = Paint.Style.FILL
        }
        val cornerRadius = size * 0.22f
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), cornerRadius, cornerRadius, bgPaint)

        // Neon green border (#00FF66)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00FF66")
            style = Paint.Style.STROKE
            strokeWidth = (size * 0.04f).coerceAtLeast(1.5f)
        }
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRoundRect(RectF(inset, inset, size.toFloat() - inset, size.toFloat() - inset), cornerRadius, cornerRadius, borderPaint)

        // Code brackets text </>
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00FF66")
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val text = "</>"
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, size / 2f, textY, textPaint)

        return bitmap
    }

    private fun createDefaultRoundBitmap(size: Int): Bitmap {
        val square = createDefaultSquareBitmap(size)
        return createCircularBitmap(square)
    }

    private fun toPngByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
