package com.niagaraclone.launcher.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import java.util.Random

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class IconPackManager(private val context: Context) {

    data class IconMaskConfig(
        val iconBacks: List<Bitmap> = emptyList(),
        val iconMask: Bitmap? = null,
        val iconUpon: Bitmap? = null,
        val scale: Float = 1.0f
    )

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val iconPacks = mutableListOf<IconPackInfo>()
        val categories = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.dlto.atom.launcher.THEME"
        )
        val packages = mutableSetOf<String>()

        for (category in categories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (!packages.contains(pkgName)) {
                    packages.add(pkgName)
                    try {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        iconPacks.add(
                            IconPackInfo(
                                packageName = pkgName,
                                label = pm.getApplicationLabel(appInfo).toString(),
                                icon = pm.getApplicationIcon(appInfo)
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return iconPacks
    }

    fun loadIconFromPack(iconPackPackage: String, appPackageName: String): Drawable? {
        val pm = context.packageManager
        return try {
            val iconPackResources = pm.getResourcesForApplication(iconPackPackage)
            val appFilterId = iconPackResources.getIdentifier("appfilter", "xml", iconPackPackage)

            if (appFilterId == 0) return null

            val parser: XmlPullParser = iconPackResources.getXml(appFilterId)
            var drawableName: String? = null

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    if (component != null && (component.contains(appPackageName) || component.startsWith("ComponentInfo{$appPackageName/"))) {
                        drawableName = parser.getAttributeValue(null, "drawable")
                        break
                    }
                }
            }

            if (drawableName != null) {
                val iconResId = iconPackResources.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (iconResId != 0) {
                    val resDrawable = iconPackResources.getDrawable(iconResId, null)
                    if (resDrawable != null) return resDrawable
                }
            }

            // Fallback: Apply Icon Masking if no direct icon mapping is defined
            val originalIcon = pm.getApplicationIcon(appPackageName)
            val maskConfig = loadMaskConfig(iconPackPackage)
            if (maskConfig.iconBacks.isNotEmpty() || maskConfig.iconMask != null) {
                return applyMask(originalIcon, maskConfig)
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadMaskConfig(iconPackPackage: String): IconMaskConfig {
        val pm = context.packageManager
        val backs = mutableListOf<Bitmap>()
        var mask: Bitmap? = null
        var upon: Bitmap? = null
        var scale = 1.0f

        try {
            val res = pm.getResourcesForApplication(iconPackPackage)
            val appFilterId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (appFilterId != 0) {
                val parser = res.getXml(appFilterId)
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "iconback" -> {
                                for (i in 0 until parser.attributeCount) {
                                    val attrName = parser.getAttributeName(i)
                                    if (attrName.startsWith("img")) {
                                        val resName = parser.getAttributeValue(i)
                                        val id = res.getIdentifier(resName, "drawable", iconPackPackage)
                                        if (id != 0) {
                                            res.getDrawable(id, null)?.let { backs.add(drawableToBitmap(it)) }
                                        }
                                    }
                                }
                            }
                            "iconmask" -> {
                                val resName = parser.getAttributeValue(null, "img1")
                                if (resName != null) {
                                    val id = res.getIdentifier(resName, "drawable", iconPackPackage)
                                    if (id != 0) {
                                        mask = res.getDrawable(id, null)?.let { drawableToBitmap(it) }
                                    }
                                }
                            }
                            "iconupon" -> {
                                val resName = parser.getAttributeValue(null, "img1")
                                if (resName != null) {
                                    val id = res.getIdentifier(resName, "drawable", iconPackPackage)
                                    if (id != 0) {
                                        upon = res.getDrawable(id, null)?.let { drawableToBitmap(it) }
                                    }
                                }
                            }
                            "scale" -> {
                                val scaleStr = parser.getAttributeValue(null, "factor")
                                scaleStr?.toFloatOrNull()?.let { scale = it }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return IconMaskConfig(backs, mask, upon, scale)
    }

    private fun applyMask(original: Drawable, config: IconMaskConfig): Drawable {
        val width = 192
        val height = 192

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw Icon Background
        if (config.iconBacks.isNotEmpty()) {
            val randomBack = config.iconBacks[Random().nextInt(config.iconBacks.size)]
            canvas.drawBitmap(Bitmap.createScaledBitmap(randomBack, width, height, true), 0f, 0f, paint)
        }

        // 2. Scale & Draw Original App Icon
        val scaledWidth = (width * config.scale).toInt()
        val scaledHeight = (height * config.scale).toInt()
        val origBitmap = drawableToBitmap(original)

        val tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f
        tempCanvas.drawBitmap(Bitmap.createScaledBitmap(origBitmap, scaledWidth, scaledHeight, true), left, top, paint)

        // 3. Apply Icon Mask
        config.iconMask?.let { maskBitmap ->
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            tempCanvas.drawBitmap(Bitmap.createScaledBitmap(maskBitmap, width, height, true), 0f, 0f, paint)
            paint.xfermode = null
        }

        canvas.drawBitmap(tempBitmap, 0f, 0f, paint)

        // 4. Overlay Icon Upon
        config.iconUpon?.let { uponBitmap ->
            canvas.drawBitmap(Bitmap.createScaledBitmap(uponBitmap, width, height, true), 0f, 0f, paint)
        }

        return BitmapDrawable(context.resources, resultBitmap)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 192
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 192
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
