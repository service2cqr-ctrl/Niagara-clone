package com.niagaraclone.launcher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
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
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.getVibrantColor(palette.getDominantColor(0xFF4DD0E1.toInt()))
                if (dominantColor != 0) return dominantColor
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0xFF4DD0E1.toInt()
    }
}
