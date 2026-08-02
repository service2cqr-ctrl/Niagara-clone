package com.niagaraclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String
)

/**
 * Detects installed icon pack apps (Nova/ADW-style "appfilter.xml" packs)
 * and resolves per-app replacement icons from them.
 */
class IconPackRepository(private val context: Context) {

    private val pm = context.packageManager

    // packageName+activity -> drawable resource name, parsed from the pack's appfilter.xml
    private var componentToDrawable: Map<String, String> = emptyMap()
    private var currentPackPackage: String? = null

    suspend fun getAvailableIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val actions = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.anddoes.launcher.THEME")
        actions.flatMap { action ->
            pm.queryIntentActivities(Intent(action), 0)
        }.map {
            IconPackInfo(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString()
            )
        }.distinctBy { it.packageName }
    }

    suspend fun selectIconPack(packageName: String?) = withContext(Dispatchers.IO) {
        currentPackPackage = packageName
        componentToDrawable = if (packageName == null) emptyMap() else parseAppFilter(packageName)
    }

    /** Returns a themed icon for the app if the current icon pack has one, else null. */
    fun getThemedIcon(entry: AppEntry): Drawable? {
        val packPkg = currentPackPackage ?: return null
        val key = "ComponentInfo{${entry.packageName}/${entry.activityClassName}}"
        val drawableName = componentToDrawable[key] ?: return null
        val resId = pm.getResourcesForApplication(packPkg).getIdentifier(drawableName, "drawable", packPkg)
        if (resId == 0) return null
        return pm.getResourcesForApplication(packPkg).getDrawable(resId, null)
    }

    /**
     * Parses the icon pack's res/xml/appfilter.xml, which maps
     * <item component="ComponentInfo{pkg/activity}" drawable="icon_name" />
     */
    private fun parseAppFilter(packPackage: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val res = pm.getResourcesForApplication(packPackage)
            val resId = res.getIdentifier("appfilter", "xml", packPackage)
            if (resId == 0) return emptyMap()
            val parser: XmlResourceParser = res.getXml(resId)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        map[component] = drawable
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            return emptyMap()
        }
        return map
    }
}
