package com.niagaraclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A single installed app entry as shown in the vertical app list.
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val activityClassName: String,
    val isSystemApp: Boolean
) {
    val firstLetter: Char
        get() = label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
}

/**
 * Wraps PackageManager to list launchable apps and resolve their icons,
 * optionally substituting icons from a user-selected icon pack.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getLaunchableApps(): List<AppEntry> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                val appInfo: ApplicationInfo = resolveInfo.activityInfo.applicationInfo
                AppEntry(
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityClassName = resolveInfo.activityInfo.name,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = android.content.ComponentName(entry.packageName, entry.activityClassName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppInfo(entry: AppEntry) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${entry.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstall(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.parse("package:${entry.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Loads the default (un-themed) icon for an app. */
    fun loadDefaultIcon(entry: AppEntry): Drawable? = try {
        packageManager.getActivityIcon(
            android.content.ComponentName(entry.packageName, entry.activityClassName)
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
