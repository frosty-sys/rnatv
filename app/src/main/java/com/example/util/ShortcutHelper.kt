package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.WebContainerActivity
import com.example.data.model.RnaApp

object ShortcutHelper {

    fun updateActivityAlias(context: Context, slot: Int, enabled: Boolean) {
        if (slot < 1 || slot > 20) return
        val componentName = ComponentName(context, "com.example.Slot${slot}Alias")
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        try {
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pinAppShortcut(context: Context, app: RnaApp): Boolean {
        return try {
            if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                return false
            }

            val launchIntent = Intent(context, WebContainerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(WebContainerActivity.EXTRA_APP_ID, app.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }

            val iconCompat = try {
                val iconStr = app.iconBase64?.trim().orEmpty()
                val isBase64Data = iconStr.startsWith("data:image", ignoreCase = true) ||
                    (iconStr.length > 64 && !iconStr.contains("://"))
                if (isBase64Data) {
                    val raw = if (iconStr.contains(",")) iconStr.substringAfter(",") else iconStr
                    val bytes = Base64.decode(raw, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) IconCompat.createWithAdaptiveBitmap(bmp)
                    else IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher_round)
                } else {
                    IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher_round)
                }
            } catch (_: Exception) {
                IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher_round)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "shortcut_${app.id}")
                .setShortLabel(app.title.ifBlank { "Web App" })
                .setLongLabel("rna.: ${app.title.ifBlank { "Web App" }}")
                .setIcon(iconCompat)
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        } catch (_: Exception) {
            false
        }
    }
}
