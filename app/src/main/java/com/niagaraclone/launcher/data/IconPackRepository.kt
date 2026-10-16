package com.niagaraclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
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
    private var packageToDrawable: Map<String, String> = emptyMap()
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
        if (packageName == null) {
            componentToDrawable = emptyMap()
            packageToDrawable = emptyMap()
        } else {
            val parsedMaps = parseAppFilter(packageName)
            componentToDrawable = parsedMaps.first
            packageToDrawable = parsedMaps.second
        }
    }

    /** Returns a themed icon for the app if the current icon pack has one, else null. */
    fun getThemedIcon(app: AppModel, activityClassName: String? = null): Drawable? {
        val packPkg = currentPackPackage ?: return null
        val targetActivity = activityClassName ?: ""
        val exactKey = "ComponentInfo{${app.packageName}/$targetActivity}"
        
        // 1. Try exact component match, 2. Try package-level fallback match
        val drawableName = componentToDrawable[exactKey] 
            ?: componentToDrawable[app.packageName] 
            ?: packageToDrawable[app.packageName] 
            ?: return null

        return try {
            val res = pm.getResourcesForApplication(packPkg)
            val resId = res.getIdentifier(drawableName, "drawable", packPkg)
            if (resId == 0) null else ResourcesCompat.getDrawable(res, resId, null)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses the icon pack's res/xml/appfilter.xml, which maps
     * <item component="ComponentInfo{pkg/activity}" drawable="icon_name" />
     */
    private fun parseAppFilter(packPackage: String): Pair<Map<String, String>, Map<String, String>> {
        val componentMap = mutableMapOf<String, String>()
        val packageMap = mutableMapOf<String, String>()
        try {
            val res = pm.getResourcesForApplication(packPackage)
            val resId = res.getIdentifier("appfilter", "xml", packPackage)
            if (resId == 0) return Pair(emptyMap(), emptyMap())
            
            val parser: XmlResourceParser = res.getXml(resId)
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (drawable != null) {
                        if (component != null) {
                            componentMap[component] = drawable
                            // Extract package name from component format ComponentInfo{pkg/activity}
                            if (component.startsWith("ComponentInfo{") && component.contains("/")) {
                                val pkgName = component.substringAfter("ComponentInfo{").substringBefore("/")
                                packageMap[pkgName] = drawable
                            }
                        }
                    }
                }
                parser.next()
            }
        } catch (e: Exception) {
            return Pair(emptyMap(), emptyMap())
        }
        return Pair(componentMap, packageMap)
    }
}
