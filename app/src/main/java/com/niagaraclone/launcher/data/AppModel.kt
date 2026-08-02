package com.niagaraclone.launcher.data

import android.graphics.drawable.Drawable

/**
 * Represents an installed application on the user's device.
 */
data class AppModel(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val notificationText: String? = null
)
