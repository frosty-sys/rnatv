package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.RNAAI_PACKAGE_NAME
import com.example.ui.tv.RNAAI_UNIQUE_KEY
import com.example.ui.tv.TvAppRamCache
import com.example.ui.tv.TvQuickAppsTaskbarOverlay
import com.example.ui.tv.TvRecentAppsDrawerOverlay
import com.example.ui.tv.UnifiedLauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Translucent standalone singleInstance Activity (`taskAffinity=""`) that renders either:
 * 1. Recent Apps Drawer (`MODE_RECENT_APPS`)
 * 2. Small All-Apps Taskbar in Dark Mode (`MODE_QUICK_TASKBAR`)
 * directly on top of any external app when triggered by a remapped remote button!
 */
class RnaQuickOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OVERLAY_MODE = "extra_overlay_mode"
        const val MODE_RECENT_APPS = "RECENT_APPS"
        const val MODE_QUICK_TASKBAR = "QUICK_TASKBAR"
    }

    private var overlayModeState by mutableStateOf(MODE_QUICK_TASKBAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.45f)

        overlayModeState = intent?.getStringExtra(EXTRA_OVERLAY_MODE) ?: MODE_QUICK_TASKBAR
        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            var allApps by remember { mutableStateOf<List<UnifiedLauncherApp>>(emptyList()) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val pm = packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                    val resolved = (pm.queryIntentActivities(mainIntent, 0) +
                        pm.queryIntentActivities(leanbackIntent, 0))
                        .distinctBy { it.activityInfo.packageName }
                        .filter { it.activityInfo.packageName != packageName }

                    val nativeList = resolved.mapNotNull { info ->
                        try {
                            val pkg = info.activityInfo.packageName
                            val label = info.loadLabel(pm).toString()
                            val icon = info.loadIcon(pm)
                            val bmp = TvAppRamCache.iconBitmaps[pkg] ?: runCatching {
                                icon.toBitmap(96, 96).asImageBitmap().also {
                                    TvAppRamCache.iconBitmaps[pkg] = it
                                }
                            }.getOrNull()
                            UnifiedLauncherApp(
                                uniqueKey = "pkg_$pkg",
                                title = label,
                                packageName = pkg,
                                isRnaWebApp = false,
                                rnaApp = null,
                                customLogoUri = null,
                                nativeIcon = icon,
                                cachedBitmap = bmp,
                                isFavorite = false,
                                favoriteOrder = 0,
                                allAppsOrder = 0,
                                lastUsedTimestamp = 0L
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val rnaIconDrawable = ContextCompat.getDrawable(this@RnaQuickOverlayActivity, R.mipmap.ic_launcher_round)
                    val rnaIconBmp = runCatching { rnaIconDrawable?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                    val rnaAiApp = UnifiedLauncherApp(
                        uniqueKey = RNAAI_UNIQUE_KEY,
                        title = "RNA AI",
                        packageName = RNAAI_PACKAGE_NAME,
                        isRnaWebApp = false,
                        rnaApp = null,
                        customLogoUri = null,
                        nativeIcon = rnaIconDrawable,
                        cachedBitmap = rnaIconBmp,
                        isFavorite = true,
                        favoriteOrder = 0,
                        allAppsOrder = 0,
                        lastUsedTimestamp = System.currentTimeMillis()
                    )

                    allApps = listOf(rnaAiApp) + nativeList.sortedBy { it.title.lowercase() }
                }
            }

            MyApplicationTheme(darkTheme = true) {
                if (overlayModeState == MODE_RECENT_APPS) {
                    TvRecentAppsDrawerOverlay(
                        allApps = allApps,
                        onLaunchApp = { app ->
                            launchUnifiedApp(app)
                            closeSilently()
                        },
                        onDismiss = {
                            closeSilently()
                        }
                    )
                } else {
                    TvQuickAppsTaskbarOverlay(
                        allApps = allApps,
                        onLaunchApp = { app ->
                            launchUnifiedApp(app)
                            closeSilently()
                        },
                        onDismiss = {
                            closeSilently()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        overlayModeState = intent.getStringExtra(EXTRA_OVERLAY_MODE) ?: MODE_QUICK_TASKBAR
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            closeSilently()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun launchUnifiedApp(app: UnifiedLauncherApp) {
        if (app.uniqueKey == RNAAI_UNIQUE_KEY || app.packageName == RNAAI_PACKAGE_NAME) {
            val aiIntent = Intent(this, RnaAiActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(aiIntent)
            return
        }
        val pm: PackageManager = packageManager
        val launchIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: pm.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    private fun closeSilently() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
