package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ui.tv.RnaTriggersExecutor
import com.example.ui.tv.TvProfilePersistenceStore

/**
 * Runs on system boot (BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / QUICKBOOT_POWERON) and app update:
 * 1. Syncs persisted user profiles & wallpapers.
 * 2. Executes configured "When Device Turns On" triggers once per boot (coordinated with
 *    RnaHomeInterceptorService so Android 10+ background activity launches work reliably):
 *    - Open Live TV at Boot: opens RNA. TV to warm it, then reopens the previously opened non-launcher app (Live TV).
 *    - Open RNA TV at Boot: opens RNA. TV directly.
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
                RnaTriggersExecutor.handleDeviceBootOnce(
                    context = appCtx,
                    fromAccessibilityService = false
                )
            }
        }
    }
}


