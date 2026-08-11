package com.niagaraclone.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class IconPackManager(private val context: Context) {

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
                    if (component != null && component.contains(appPackageName)) {
                        drawableName = parser.getAttributeValue(null, "drawable")
                        break
                    }
                }
            }

            if (drawableName != null) {
                val iconResId = iconPackResources.getIdentifier(drawableName, "drawable", iconPackPackage)
                if (iconResId != 0) {
                    return iconPackResources.getDrawable(iconResId, null)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace Moritz
            null
        }
    }
}
