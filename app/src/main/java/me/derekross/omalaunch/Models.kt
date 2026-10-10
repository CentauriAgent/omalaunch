package me.derekross.omalaunch

import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.min

data class AppModel(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val key: String get() = "$packageName/$activityName"
}

/**
 * Convert any drawable (adaptive icons included) to an ImageBitmap,
 * capped at [maxPx] per side.
 */
fun Drawable.toImageBitmap(maxPx: Int = 144): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        val b = bitmap
        if (b.width in 1..maxPx && b.height in 1..maxPx) return b.asImageBitmap()
    }
    val w = if (intrinsicWidth <= 0) maxPx else min(intrinsicWidth, maxPx)
    val h = if (intrinsicHeight <= 0) maxPx else min(intrinsicHeight, maxPx)
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bmp.asImageBitmap()
}
