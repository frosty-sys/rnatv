package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ui.tv.KEY_OPEN_RNA_AT_BOOT
import com.example.ui.tv.RnaTriggerType
import com.example.ui.tv.RnaTriggersExecutor
import com.example.ui.tv.TvProfilePersistenceStore

/**
 * Runs on system boot (BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / QUICKBOOT_POWERON) and app update:
 * 1. Syncs persisted user profiles & wallpapers.
 * 2. Executes configured "When Device Turns On" triggers (defaults to silent background pre-warm
 *    so Live TV is never disturbed, while RNA. TV opens instantaneously with zero lag when Home is pressed).
 * 3. If the user explicitly enabled "Open RNA TV at Boot" in Control Panel, straight-up opens RNA. TV foreground.
 */
class RnaBootAndUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appCtx = context.applicationContext
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            TvProfilePersistenceStore.ensureProfilesSyncedOnBootOrUpdate(appCtx)

            val isBootEvent = action != Intent.ACTION_MY_PACKAGE_REPLACED
            if (isBootEvent) {
                // Execute user/default "When Device Turns On" triggers (default: silent background pre-warm)
                RnaTriggersExecutor.executeTriggersForType(appCtx, RnaTriggerType.DEVICE_BOOT)

                // If user enabled "Open RNA TV at Boot" in Control Panel, launch RNA. TV straight to foreground
                val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
                val openRnaAtBoot = prefs.getBoolean(KEY_OPEN_RNA_AT_BOOT, false)
                if (openRnaAtBoot) {
                    RnaTriggersExecutor.launchRnaTvForeground(appCtx)
                }
            }
        }
    }
}

