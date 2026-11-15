package com.yasin.playora.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

/**
 * Basic photo edits, applied on a Bitmap in memory.
 * Nothing here writes to disk — the caller decides when to save.
 *
 * TODO (Phase 3): filters (grayscale, sepia, warm/cool), free crop,
 * text stickers, and saving the edited copy to the gallery.
 */
object PhotoEditor {

    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun flipHorizontal(source: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun cropCenterSquare(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        return Bitmap.createBitmap(source, x, y, size, size)
    }

    /** brightness: -1f (dark) .. 1f (bright), 0f = unchanged. */
    fun adjustBrightness(source: Bitmap, brightness: Float): Bitmap {
        val value = brightness.coerceIn(-1f, 1f) * 255f
        val matrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, value,
                0f, 1f, 0f, 0f, value,
                0f, 0f, 1f, 0f, value,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyMatrix(source, matrix)
    }

    /** contrast: 0f (grey) .. 2f (strong), 1f = unchanged. */
    fun adjustContrast(source: Bitmap, contrast: Float): Bitmap {
        val c = contrast.coerceIn(0f, 2f)
        val t = (1f - c) * 128f
        val matrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyMatrix(source, matrix)
    }

    private fun applyMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(
            source.width, source.height,
            source.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(result)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
