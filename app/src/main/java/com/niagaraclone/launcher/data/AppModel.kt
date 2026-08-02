package com.niagaraclone.launcher.data

import android.graphics.drawable.Drawable

/**
 * Represents an installed application on the user's device.
 *
 * @property packageName The unique package identifier (e.g., "com.whatsapp").
 * @property label The human-readable display name of the app.
 * @property icon The app's drawable icon retrieved from PackageManager, or null if unavailable.
 */
data class AppModel(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)
