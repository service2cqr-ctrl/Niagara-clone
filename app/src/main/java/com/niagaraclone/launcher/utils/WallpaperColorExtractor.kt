package com.niagaraclone.launcher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.palette.graphics.Palette

object WallpaperColorExtractor {

    fun getDynamicAccentColor(context: Context): Int {
        val wallpaperManager = WallpaperManager.getInstance(context)

        // 1. Android 12+ (API 31+) Native Material You Wallpaper Color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val primaryColor = wallpaperColors?.primaryColor?.toArgb()
                if (primaryColor != null && primaryColor != 0) {
                    return primaryColor
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Android 8.0+ Fallback via Palette API from Wallpaper Bitmap
        try {
            val drawable = wallpaperManager.drawable
            if (drawable != null) {
                val bitmap = drawableToBitmap(drawable)
                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.getVibrantColor(palette.getDominantColor(0xFF4DD0E1.toInt()))
                if (dominantColor != 0) return dominantColor
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0xFF4DD0E1.toInt()
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
