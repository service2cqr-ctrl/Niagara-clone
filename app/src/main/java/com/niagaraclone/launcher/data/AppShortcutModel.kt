package com.niagaraclone.launcher.data

import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable

data class AppShortcutModel(
    val id: String,
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val shortcutInfo: ShortcutInfo
)
