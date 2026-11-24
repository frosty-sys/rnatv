package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.RnaAgentGlowAndHudOverlay
import com.example.ui.tv.RnaAutonomousAgentController
import com.example.ui.tv.TvPerformanceMode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

class RnaHomeInterceptorService : AccessibilityService() {

    companion object {
        const val PREFS_NAME = "rna_tv_launcher_prefs"
        const val KEY_INTERCEPT_HOME = "intercept_home_enabled"
        const val KEY_REPLACE_GOOGLE_ASSISTANT = "replace_google_assistant_enabled"
        const val KEY_CONTROL_USB_BOOT_AUTO_BACK = "control_usb_boot_auto_back"

        @Volatile
        var instance: RnaHomeInterceptorService? = null

        private val STOCK_TV_LAUNCHERS = setOf(
            "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher",
            "com.google.android.tungsten.setupwraith",
            "com.amazon.tv.launcher",
            "com.android.tv.launcher",
            "com.miui.tv.home",
            "com.sony.dtv.osat.launcher",
            "com.tcl.cyberui",
            "com.hisense.tv.launcher"
        )

        fun isStockTvLauncherPackage(pkgName: String?): Boolean {
            if (pkgName.isNullOrBlank()) return false
            if (pkgName.startsWith("com.example")) return false
            val lower = pkgName.lowercase(Locale.US)
            if (lower in STOCK_TV_LAUNCHERS) return true
            return lower.endsWith(".tvlauncher") ||
                lower.endsWith(".leanbacklauncher") ||
                lower == "com.google.android.tvlauncher" ||
                lower == "com.google.android.leanbacklauncher"
        }

        private val GOOGLE_ASSISTANT_PACKAGES = setOf(
            "com.google.android.katniss",
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.tv.assistant",
            "com.google.android.apps.assistant",
            "com.google.android.apps.googleassistant",
            "com.google.android.voicesearch",
            "com.google.android.tv.assistant"
        )

        fun isGoogleAssistantPackage(pkgName: String?, className: String? = null): Boolean {
            if (pkgName.isNullOrBlank()) return false
            if (pkgName.startsWith("com.example")) return false
            val lowerPkg = pkgName.lowercase(Locale.US)
            val lowerClass = className?.lowercase(Locale.US).orEmpty()
            if (lowerPkg in GOOGLE_ASSISTANT_PACKAGES) return true
            if (lowerPkg.contains("katniss") ||
                lowerPkg.contains("googlequicksearchbox") ||
                lowerPkg.contains(".assistant") ||
                lowerPkg.contains("voicesearch") ||
                lowerPkg.contains("hotword")
            ) return true
            if (lowerClass.contains("katniss") ||
                lowerClass.contains("opaactivity") ||
                lowerClass.contains("assistantactivity") ||
                lowerClass.contains("voicesearch")
            ) return true
            return false
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (instance != null) return true
            val expectedComponentName = "${context.packageName}/${RnaHomeInterceptorService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(expectedComponentName, ignoreCase = true) }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastAssistantDismissElapsedMs: Long = 0L
    private var lastStockLauncherInterceptElapsedMs: Long = 0L
    private var currentForegroundPackage: String = ""
    private var lastAppOpenTriggerPackage: String = ""
    private var lastAppOpenTriggerElapsedMs: Long = 0L
    private var lastSpatialFocusedNodeBounds: Rect? = null
    private var hasPerformedBootUsbAutoBack: Boolean = false

    // Multi-Press & Long-Press State Machine per Hardware KeyCode
    private class MultiPressKeyState(
        var tapCount: Int = 0,
        var longPressTriggered: Boolean = false,
        var longPressRunnable: Runnable? = null,
        var multiTapFinalizeRunnable: Runnable? = null
    )
    private val keyGestureStates = mutableMapOf<Int, MultiPressKeyState>()

    private val screenOnTriggerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                com.example.ui.tv.RnaTriggersExecutor.executeTriggersForType(
                    this@RnaHomeInterceptorService,
                    com.example.ui.tv.RnaTriggerType.SCREEN_ON
                )
            }
        }
    }

    private val usbBootReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoBackEnabled = prefs.getBoolean(KEY_CONTROL_USB_BOOT_AUTO_BACK, false)
            if (!autoBackEnabled || hasPerformedBootUsbAutoBack) return
            // Only trigger if a USB drive is detected within the first 1 minute of device boot
            if (SystemClock.elapsedRealtime() <= 60_000L) {
                hasPerformedBootUsbAutoBack = true
                mainHandler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }, 350L)
            }
        }
    }

    // System-wide Unified Jetpack Compose AI Agent Overlay Window (exact same HUD as RNA. TV)
    private var windowManager: WindowManager? = null
    private var agentOverlayComposeView: ComposeView? = null
    private var agentOverlayLifecycleOwner: ServiceComposeLifecycleOwner? = null
    private var agentOverlayLayoutParams: WindowManager.LayoutParams? = null
    private var isOverlayKeyboardInputVisible: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        wireAutonomousAgentHooks()
        syncSystemAgentOverlayWindow()
        registerUsbBootAutoBackReceiver()
        registerScreenOnTriggerReceiver()
    }

    private fun registerScreenOnTriggerReceiver() {
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenOnTriggerReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(screenOnTriggerReceiver, filter)
            }
        } catch (_: Exception) {
        }
    }

    private fun registerUsbBootAutoBackReceiver() {
        try {
            val mediaFilter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addDataScheme("file")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(usbBootReceiver, mediaFilter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(usbBootReceiver, mediaFilter)
            }
        } catch (_: Exception) {
        }
        try {
            val usbFilter = android.content.IntentFilter(android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(usbBootReceiver, usbFilter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(usbBootReceiver, usbFilter)
            }
        } catch (_: Exception) {
        }
    }

    private fun wireAutonomousAgentHooks() {
        RnaAutonomousAgentController.onPerformGlobalKeyOrAction = { normalizedCmd ->
            performAgentHardwareCommandOverAnyApp(normalizedCmd)
        }
        RnaAutonomousAgentController.onPerformAccessibilityType = { textToType ->
            performAgentTypeOverAnyApp(textToType)
        }
        RnaAutonomousAgentController.onTakeAccessibilityScreenshot = {
            captureAccessibilityScreenshot()
        }
        RnaAutonomousAgentController.onSyncSystemOverlayCallback = {
            mainHandler.post {
                syncSystemAgentOverlayWindow()
            }
        }
    }

    /**
     * Unified System Screen Overlay (`TYPE_APPLICATION_OVERLAY` when overlay permission is granted,
     * falling back to `TYPE_ACCESSIBILITY_OVERLAY`) hosting the EXACT same Jetpack Compose
     * [RnaAgentGlowAndHudOverlay] used inside RNA. TV on top of ANY external app.
     */
    private fun syncSystemAgentOverlayWindow() {
        val isAgentActive = RnaAutonomousAgentController.isAgentModeActive.value
        val isExternalAppForeground = currentForegroundPackage.isNotBlank() && currentForegroundPackage != packageName

        if (!isAgentActive || !isExternalAppForeground) {
            removeSystemAgentOverlayWindow()
            return
        }

        if (agentOverlayComposeView != null) {
            agentOverlayLifecycleOwner?.onResume()
            return
        }

        val wm = windowManager ?: (getSystemService(Context.WINDOW_SERVICE) as? WindowManager).also { windowManager = it } ?: return

        val lifecycleOwner = ServiceComposeLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            lifecycleOwner.attachToView(this)
            setContent {
                MyApplicationTheme {
                    RnaAgentGlowAndHudOverlay(
                        performanceMode = TvPerformanceMode.isEnabled,
                        isServiceOverlay = true,
                        onKeyboardInputExpandedChanged = { expanded ->
                            setOverlayKeyboardInputExpanded(expanded)
                        },
                        onServiceFallbackMicLaunch = {
                            setOverlayKeyboardInputExpanded(false)
                            launchRnaAssistantOverlay(null)
                        },
                        onDismissServiceOverlay = {
                            setOverlayKeyboardInputExpanded(false)
                            removeSystemAgentOverlayWindow()
                        }
                    )
                }
            }
        }

        val preferredWindowType = if (Settings.canDrawOverlays(this)) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            preferredWindowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val added = try {
            wm.addView(composeView, params)
            true
        } catch (_: Exception) {
            try {
                params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                wm.addView(composeView, params)
                true
            } catch (_: Exception) {
                false
            }
        }

        if (added) {
            agentOverlayComposeView = composeView
            agentOverlayLifecycleOwner = lifecycleOwner
            agentOverlayLayoutParams = params
        } else {
            lifecycleOwner.onDestroy()
        }
    }

    private fun setOverlayKeyboardInputExpanded(expanded: Boolean) {
        if (isOverlayKeyboardInputVisible == expanded) return
        isOverlayKeyboardInputVisible = expanded
        val view = agentOverlayComposeView ?: return
        val params = agentOverlayLayoutParams ?: return
        val wm = windowManager ?: return
        try {
            if (expanded) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
            }
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun removeSystemAgentOverlayWindow() {
        val view = agentOverlayComposeView ?: return
        agentOverlayLifecycleOwner?.onPause()
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        agentOverlayLifecycleOwner?.onDestroy()
        agentOverlayComposeView = null
        agentOverlayLifecycleOwner = null
        agentOverlayLayoutParams = null
        isOverlayKeyboardInputVisible = false
    }

    private fun performAgentHardwareCommandOverAnyApp(normalizedCmd: String): Boolean {
        val isHold = normalizedCmd.contains("[hold]")
        val isDouble = normalizedCmd.contains("[double]")
        val baseKey = normalizedCmd
            .replace("[hold]", "")
            .replace("[double]", "")
            .replace("d-pad.ok.left", "d-pad.left")

        val keyCode = when (baseKey) {
            "volume.up", "d-pad.volume_up", "volume_up" -> KeyEvent.KEYCODE_VOLUME_UP
            "volume.down", "d-pad.volume_down", "volume_down" -> KeyEvent.KEYCODE_VOLUME_DOWN
            "volume.mute", "mute" -> KeyEvent.KEYCODE_VOLUME_MUTE
            "back", "d-pad.back", "button_b", "b" -> KeyEvent.KEYCODE_BACK
            "home", "d-pad.home", "button_mode", "guide" -> KeyEvent.KEYCODE_HOME
            "d-pad.ok", "ok", "enter", "center", "button_a", "a" -> KeyEvent.KEYCODE_DPAD_CENTER
            "d-pad.up", "up" -> KeyEvent.KEYCODE_DPAD_UP
            "d-pad.down", "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "d-pad.left", "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "d-pad.right", "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "button_x", "x" -> KeyEvent.KEYCODE_BUTTON_X
            "button_y", "y" -> KeyEvent.KEYCODE_BUTTON_Y
            "button_l1", "lb" -> KeyEvent.KEYCODE_BUTTON_L1
            "button_r1", "rb" -> KeyEvent.KEYCODE_BUTTON_R1
            "button_l2", "lt" -> KeyEvent.KEYCODE_BUTTON_L2
            "button_r2", "rt" -> KeyEvent.KEYCODE_BUTTON_R2
            "button_thumbl", "l3" -> KeyEvent.KEYCODE_BUTTON_THUMBL
            "button_thumbr", "r3" -> KeyEvent.KEYCODE_BUTTON_THUMBR
            "button_start", "start" -> KeyEvent.KEYCODE_BUTTON_START
            "button_select", "select" -> KeyEvent.KEYCODE_BUTTON_SELECT
            else -> return false
        }
        return dispatchPhysicalKeyOrGamepadActionOverExternalApp(
            keyCode = keyCode,
            isHold = isHold,
            isDouble = isDouble
        )
    }

    /**
     * Remaps Phone Remote & Xbox Wireless Controller inputs (including BUTTON_1..16, Xbox buttons,
     * D-Pad, and Analog Sticks) into real system-wide Android actions, focus traversal, clicks,
     * media key dispatches, and continuous gestures across ALL external apps using AccessibilityService.
     */
    fun dispatchPhysicalKeyOrGamepadActionOverExternalApp(
        keyCode: Int,
        isHold: Boolean = false,
        isDouble: Boolean = false
    ): Boolean {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                true
            }
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_ESCAPE -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BUTTON_MODE -> {
                launchRnaTvHome()
                true
            }
            KeyEvent.KEYCODE_APP_SWITCH -> {
                launchRecentAppsOverlayFromService()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_BUTTON_13 -> {
                performRobustDirectionalNavigation(View.FOCUS_UP)
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_BUTTON_14 -> {
                performRobustDirectionalNavigation(View.FOCUS_DOWN)
            }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BUTTON_15 -> {
                performRobustDirectionalNavigation(View.FOCUS_LEFT)
            }
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_16 -> {
                performRobustDirectionalNavigation(View.FOCUS_RIGHT)
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_12 -> {
                performRobustCenterClick(isHold = isHold, isDouble = isDouble)
            }
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_3 -> {
                performRobustCenterClick(isHold = isHold, isDouble = false)
            }
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_4 -> {
                performRobustCenterClick(isHold = false, isDouble = false)
            }
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_5 -> {
                if (!performScrollInDirection(View.FOCUS_LEFT)) {
                    performRobustDirectionalNavigation(View.FOCUS_LEFT)
                } else {
                    true
                }
            }
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_6 -> {
                if (!performScrollInDirection(View.FOCUS_RIGHT)) {
                    performRobustDirectionalNavigation(View.FOCUS_RIGHT)
                } else {
                    true
                }
            }
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_7 -> {
                if (!performScrollInDirection(View.FOCUS_UP)) {
                    val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND)
                    val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND)
                    audioManager?.dispatchMediaKeyEvent(down)
                    audioManager?.dispatchMediaKeyEvent(up)
                }
                true
            }
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_8 -> {
                if (!performScrollInDirection(View.FOCUS_DOWN)) {
                    val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                    val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
                    audioManager?.dispatchMediaKeyEvent(down)
                    audioManager?.dispatchMediaKeyEvent(up)
                }
                true
            }
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                audioManager?.dispatchMediaKeyEvent(down)
                audioManager?.dispatchMediaKeyEvent(up)
                performRobustCenterClick(isHold = false, isDouble = false)
                true
            }
            else -> {
                // Any custom mapped keycode fallback: perform primary focus click
                performRobustCenterClick(isHold = isHold, isDouble = isDouble)
            }
        }
    }

    /**
     * Allows the Right Analog Stick (or Left Stick in non-DPAD surfaces) to perform smooth spatial
     * scroll/drag gestures over external apps via AccessibilityService `dispatchGesture`.
     */
    fun dispatchAnalogStickOverExternalApp(lx: Float, ly: Float, rx: Float, ry: Float): Boolean {
        if (abs(rx) <= 0.35f && abs(ry) <= 0.35f) return false
        val dm = resources.displayMetrics
        val cx = dm.widthPixels * 0.5f
        val cy = dm.heightPixels * 0.5f
        val dx = (-rx * 180f).coerceIn(-280f, 280f)
        val dy = (-ry * 180f).coerceIn(-280f, 280f)
        return try {
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo((cx + dx).coerceIn(10f, dm.widthPixels - 10f), (cy + dy).coerceIn(10f, dm.heightPixels - 10f))
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 95L))
                .build()
            dispatchGesture(gesture, null, null)
        } catch (_: Exception) {
            false
        }
    }

    private fun performRobustDirectionalNavigation(direction: Int): Boolean {
        val root = rootInActiveWindow ?: return performDirectionalSwipeFallback(direction)
        val currentFocused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)

        // 1. Try Android's native focusSearch first
        val nativeNext = (currentFocused ?: root).focusSearch(direction)
        if (nativeNext != null && nativeNext != currentFocused) {
            if (focusAndSelectNode(nativeNext)) {
                return true
            }
        }

        // 2. Spatial geometric focus search across all visible interactive nodes in the active window
        val spatialNext = findBestSpatialNodeInDirection(root, currentFocused, direction)
        if (spatialNext != null) {
            if (focusAndSelectNode(spatialNext)) {
                return true
            }
        }

        // 3. If at the edge of the screen or inside a scrollable list, scroll the container
        if (performScrollInDirection(direction)) {
            return true
        }

        // 4. Fallback to directional gesture swipe
        return performDirectionalSwipeFallback(direction)
    }

    private fun focusAndSelectNode(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty) {
            lastSpatialFocusedNodeBounds = Rect(rect)
        }
        var ok = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val accOk = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        if (!ok && !accOk) {
            // Walk up to the nearest focusable parent if the leaf node refused focus
            var parent = node.parent
            var hops = 0
            while (parent != null && hops < 4) {
                if (parent.isFocusable) {
                    ok = parent.performAction(AccessibilityNodeInfo.ACTION_FOCUS) ||
                        parent.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
                    if (ok) break
                }
                parent = parent.parent
                hops++
            }
        }
        return ok || accOk || !rect.isEmpty
    }

    private fun findBestSpatialNodeInDirection(
        root: AccessibilityNodeInfo,
        currentFocused: AccessibilityNodeInfo?,
        direction: Int
    ): AccessibilityNodeInfo? {
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Rect>>()
        collectInteractiveNodes(root, candidates, 0)
        if (candidates.isEmpty()) return null

        val originRect = Rect()
        val hasOrigin = when {
            currentFocused != null -> {
                currentFocused.getBoundsInScreen(originRect)
                !originRect.isEmpty
            }
            lastSpatialFocusedNodeBounds != null && !lastSpatialFocusedNodeBounds!!.isEmpty -> {
                originRect.set(lastSpatialFocusedNodeBounds!!)
                true
            }
            else -> false
        }

        if (!hasOrigin) {
            return candidates.firstOrNull()?.first
        }

        val cx = originRect.centerX()
        val cy = originRect.centerY()

        var bestNode: AccessibilityNodeInfo? = null
        var bestScore = Double.MAX_VALUE

        for ((node, rect) in candidates) {
            if (node == currentFocused) continue
            val nx = rect.centerX()
            val ny = rect.centerY()
            if (abs(nx - cx) < 6 && abs(ny - cy) < 6) continue

            val dx = (nx - cx).toDouble()
            val dy = (ny - cy).toDouble()

            val isValidDirection = when (direction) {
                View.FOCUS_UP -> dy < -8.0
                View.FOCUS_DOWN -> dy > 8.0
                View.FOCUS_LEFT -> dx < -8.0
                View.FOCUS_RIGHT -> dx > 8.0
                else -> false
            }
            if (!isValidDirection) continue

            val primaryDist = when (direction) {
                View.FOCUS_UP, View.FOCUS_DOWN -> abs(dy)
                else -> abs(dx)
            }
            val crossDist = when (direction) {
                View.FOCUS_UP, View.FOCUS_DOWN -> abs(dx)
                else -> abs(dy)
            }

            // Favor nodes aligned on the same row/column while allowing diagonal jumps when needed
            val score = primaryDist + (crossDist * 2.25)
            if (score < bestScore) {
                bestScore = score
                bestNode = node
            }
        }
        return bestNode
    }

    private fun collectInteractiveNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<Pair<AccessibilityNodeInfo, Rect>>,
        depth: Int
    ) {
        if (node == null || depth > 28 || out.size >= 180) return
        if (node.isVisibleToUser && node.isEnabled) {
            val isInteractive = node.isFocusable || node.isClickable || node.isEditable || node.isCheckable
            if (isInteractive) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (rect.width() > 8 && rect.height() > 8) {
                    out.add(node to rect)
                }
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            collectInteractiveNodes(node.getChild(i), out, depth + 1)
        }
    }

    private fun performRobustCenterClick(isHold: Boolean = false, isDouble: Boolean = false): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: findNodeMatchingSavedBounds(root)

            if (focused != null) {
                var clickTarget: AccessibilityNodeInfo? = focused
                var hops = 0
                while (clickTarget != null &&
                    !clickTarget.isClickable &&
                    !clickTarget.isLongClickable &&
                    !clickTarget.isEditable &&
                    !clickTarget.isCheckable &&
                    hops < 5
                ) {
                    clickTarget = clickTarget.parent
                    hops++
                }
                val resolvedTarget = clickTarget ?: focused

                val action = if (isHold) {
                    AccessibilityNodeInfo.ACTION_LONG_CLICK
                } else {
                    AccessibilityNodeInfo.ACTION_CLICK
                }

                val clicked = resolvedTarget.performAction(action) || focused.performAction(action)
                if (isDouble) {
                    mainHandler.postDelayed({
                        resolvedTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }, 90L)
                }
                if (clicked) return true

                val rect = Rect()
                resolvedTarget.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    return dispatchTapGesture(
                        x = rect.exactCenterX(),
                        y = rect.exactCenterY(),
                        durationMs = if (isHold) 450L else 55L
                    )
                }
            }
        }

        val fallbackBounds = lastSpatialFocusedNodeBounds
        if (fallbackBounds != null && !fallbackBounds.isEmpty) {
            return dispatchTapGesture(
                x = fallbackBounds.exactCenterX(),
                y = fallbackBounds.exactCenterY(),
                durationMs = if (isHold) 450L else 55L
            )
        }
        return false
    }

    private fun findNodeMatchingSavedBounds(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val saved = lastSpatialFocusedNodeBounds ?: return null
        if (saved.isEmpty) return null
        val candidates = mutableListOf<Pair<AccessibilityNodeInfo, Rect>>()
        collectInteractiveNodes(root, candidates, 0)
        return candidates.minByOrNull { (_, r) ->
            abs(r.centerX() - saved.centerX()) + abs(r.centerY() - saved.centerY())
        }?.first
    }

    private fun performScrollInDirection(direction: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollables = mutableListOf<AccessibilityNodeInfo>()
        collectScrollableNodes(root, scrollables, 0)
        for (scrollable in scrollables) {
            val action = when (direction) {
                View.FOCUS_DOWN, View.FOCUS_RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (scrollable.performAction(action)) return true
        }
        return false
    }

    private fun collectScrollableNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int
    ) {
        if (node == null || depth > 25 || out.size >= 25) return
        if (node.isVisibleToUser && node.isScrollable) {
            out.add(node)
        }
        val count = node.childCount
        for (i in 0 until count) {
            collectScrollableNodes(node.getChild(i), out, depth + 1)
        }
    }

    private fun performDirectionalSwipeFallback(direction: Int): Boolean {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(320).toFloat()
        val h = dm.heightPixels.coerceAtLeast(240).toFloat()
        val cx = w * 0.5f
        val cy = h * 0.5f
        val (sx, sy, ex, ey) = when (direction) {
            View.FOCUS_UP -> listOf(cx, h * 0.35f, cx, h * 0.72f)
            View.FOCUS_DOWN -> listOf(cx, h * 0.72f, cx, h * 0.35f)
            View.FOCUS_LEFT -> listOf(w * 0.32f, cy, w * 0.72f, cy)
            else -> listOf(w * 0.72f, cy, w * 0.32f, cy)
        }
        return try {
            val path = Path().apply {
                moveTo(sx, sy)
                lineTo(ex, ey)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, 140L))
                .build()
            dispatchGesture(gesture, null, null)
        } catch (_: Exception) {
            false
        }
    }

    private fun dispatchTapGesture(x: Float, y: Float, durationMs: Long = 55L): Boolean {
        return try {
            val path = Path().apply {
                moveTo(x.coerceAtLeast(1f), y.coerceAtLeast(1f))
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build()
            dispatchGesture(gesture, null, null)
        } catch (_: Exception) {
            false
        }
    }

    private var lastExternalEditableNodeBounds: Rect? = null
    private var lastExternalEditablePackage: String = ""
    private var lastExternalKeyboardLaunchMs: Long = 0L

    private fun performAgentTypeOverAnyApp(text: String): Boolean {
        return injectTextIntoExternalApp(text, submitAfter = false)
    }

    /**
     * Injects text from the standalone bottom-center [RnaTvKeyboardActivity] into the active editable
     * field of any external Android TV app (YouTube, Netflix, Settings, Play Store, etc.).
     */
    fun injectTextIntoExternalApp(text: String, submitAfter: Boolean): Boolean {
        val targetNode = findExternalEditableTargetNode() ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val setOk = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (submitAfter) {
            mainHandler.postDelayed({
                val freshTarget = findExternalEditableTargetNode() ?: targetNode
                var submitted = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    submitted = freshTarget.performAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id
                    )
                }
                if (!submitted) {
                    freshTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }, 60L)
        }
        return setOk
    }

    private fun findExternalEditableTargetNode(): AccessibilityNodeInfo? {
        val rootList = mutableListOf<AccessibilityNodeInfo>()
        try {
            for (win in windows) {
                val root = win.root ?: continue
                val pkg = root.packageName?.toString().orEmpty()
                if (pkg.isNotBlank() && pkg != packageName) {
                    rootList.add(root)
                }
            }
        } catch (_: Exception) {
        }
        rootInActiveWindow?.let { activeRoot ->
            val pkg = activeRoot.packageName?.toString().orEmpty()
            if (pkg != packageName && activeRoot !in rootList) {
                rootList.add(0, activeRoot)
            }
        }

        // 1. Check focused input node in any external window
        for (root in rootList) {
            val focusedInput = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focusedInput != null && (focusedInput.isEditable || isEditableClassName(focusedInput.className))) {
                return focusedInput
            }
        }

        // 2. Collect all visible editable nodes in external windows and match closest to saved bounds
        val editables = mutableListOf<Pair<AccessibilityNodeInfo, Rect>>()
        for (root in rootList) {
            collectEditableNodes(root, editables, 0)
        }
        if (editables.isEmpty()) return null
        val savedRect = lastExternalEditableNodeBounds
        if (savedRect != null && !savedRect.isEmpty) {
            return editables.minByOrNull { (_, r) ->
                abs(r.centerX() - savedRect.centerX()) + abs(r.centerY() - savedRect.centerY())
            }?.first
        }
        return editables.first().first
    }

    private fun isEditableClassName(className: CharSequence?): Boolean {
        val name = className?.toString()?.lowercase(Locale.US).orEmpty()
        return name.contains("edittext") ||
            name.contains("autocompletetextview") ||
            name.contains("searchedit") ||
            name.contains("textinput")
    }

    private fun collectEditableNodes(
        node: AccessibilityNodeInfo?,
        out: MutableList<Pair<AccessibilityNodeInfo, Rect>>,
        depth: Int
    ) {
        if (node == null || depth > 28 || out.size >= 40) return
        if (node.isVisibleToUser && node.isEnabled && (node.isEditable || isEditableClassName(node.className))) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                out.add(node to rect)
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            collectEditableNodes(node.getChild(i), out, depth + 1)
        }
    }

    private suspend fun captureAccessibilityScreenshot(): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { cont ->
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val hwBitmap = runCatching {
                                Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                            }.getOrNull()
                            runCatching { screenshot.hardwareBuffer.close() }
                            if (cont.isActive) cont.resume(hwBitmap)
                        }

                        override fun onFailure(errorCode: Int) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val replaceAssistantEnabled = prefs.getBoolean(KEY_REPLACE_GOOGLE_ASSISTANT, false)
        val interceptHomeEnabled = prefs.getBoolean(KEY_INTERCEPT_HOME, true)

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val pkgName = (event.packageName ?: event.source?.packageName ?: rootInActiveWindow?.packageName)?.toString().orEmpty()
            val className = event.className?.toString().orEmpty()
            if (pkgName.isNotBlank() && pkgName != "android" && pkgName != "com.android.systemui") {
                val previousPkg = currentForegroundPackage
                currentForegroundPackage = pkgName
                syncSystemAgentOverlayWindow()

                // Fire custom App-Open Triggers whenever a new foreground app opens
                if (pkgName != previousPkg && pkgName != packageName) {
                    val nowMs = SystemClock.elapsedRealtime()
                    if (pkgName != lastAppOpenTriggerPackage || nowMs - lastAppOpenTriggerElapsedMs > 1800L) {
                        lastAppOpenTriggerPackage = pkgName
                        lastAppOpenTriggerElapsedMs = nowMs
                        com.example.ui.tv.RnaTriggersExecutor.executeTriggersForAppOpened(this, pkgName)
                    }
                }
            }

            // Automatically check for the Android TV stock launcher app whenever "Set RNA. TV as Default Launcher"
            // (KEY_INTERCEPT_HOME) is enabled, and if it ever opens just in case, immediately open RNA. TV without delay!
            if (interceptHomeEnabled && isStockTvLauncherPackage(pkgName)) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastStockLauncherInterceptElapsedMs > 220L) {
                    lastStockLauncherInterceptElapsedMs = now
                    launchRnaTvHome()
                }
                return
            }

            // Replace Google Assistant (invoked by system "Hey Google" or remote Google Assistant button):
            // Press GLOBAL_ACTION_BACK to dismiss Google Assistant so it doesn't stay on screen,
            // and immediately open RnaAssistantOverlayActivity.
            if (replaceAssistantEnabled && isGoogleAssistantPackage(pkgName, className)) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastAssistantDismissElapsedMs > 450L) {
                    lastAssistantDismissElapsedMs = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.postDelayed({
                        launchRnaAssistantOverlay(null)
                    }, 60L)
                }
                return
            }
        }

        // Track focused editable node bounds in external apps and ensure RnaTvInputMethodService
        // shows its native IME window when RNA Keyboard is the active system default IME.
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) {
            val pkgName = event.packageName?.toString().orEmpty()
            if (pkgName.isNotBlank() &&
                pkgName != packageName &&
                pkgName != "com.android.systemui"
            ) {
                val src = event.source
                val isEditableField = src != null && (src.isEditable || isEditableClassName(src.className))
                if (isEditableField && src != null) {
                    val rect = Rect()
                    src.getBoundsInScreen(rect)
                    if (!rect.isEmpty) {
                        lastExternalEditableNodeBounds = Rect(rect)
                        lastExternalEditablePackage = pkgName
                    }
                    if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED &&
                        RnaTvInputMethodService.isDefaultSystemIme(this) &&
                        !(RnaAutonomousAgentController.isAgentActivelyControlling() && RnaAutonomousAgentController.isWorking.value)
                    ) {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastExternalKeyboardLaunchMs > 400L) {
                            lastExternalKeyboardLaunchMs = now
                            mainHandler.postDelayed({
                                if (RnaTvInputMethodService.isDefaultSystemIme(this@RnaHomeInterceptorService) &&
                                    !(RnaAutonomousAgentController.isAgentActivelyControlling() && RnaAutonomousAgentController.isWorking.value)
                                ) {
                                    RnaTvInputMethodService.instance?.requestShowFromAccessibility()
                                }
                            }, 80L)
                        }
                    }
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Live Key Capture Mode for TV Key Remapper ("Press any button on your remote")
        if (com.example.ui.tv.KeyRemapperBridge.isCapturingKey) {
            // Allow Back to cancel capture dialog if user presses Back, unless they specifically want to capture another key
            if (event.keyCode != KeyEvent.KEYCODE_BACK &&
                event.keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
                event.keyCode != KeyEvent.KEYCODE_ENTER
            ) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    val capturedCode = event.keyCode
                    mainHandler.post {
                        com.example.ui.tv.KeyRemapperBridge.notifyKeyCaptured(capturedCode)
                    }
                }
                return true
            }
        }

        // 2. Intercept Assistant / Voice / Search hardware buttons anywhere on the TV when Replace Google Assistant is ON
        if (event.keyCode == KeyEvent.KEYCODE_ASSIST ||
            event.keyCode == KeyEvent.KEYCODE_VOICE_ASSIST ||
            event.keyCode == KeyEvent.KEYCODE_SEARCH
        ) {
            val replaceAssistantEnabled = prefs.getBoolean(KEY_REPLACE_GOOGLE_ASSISTANT, false)
            if (replaceAssistantEnabled) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    lastAssistantDismissElapsedMs = SystemClock.elapsedRealtime()
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.postDelayed({
                        launchRnaAssistantOverlay(null)
                    }, 50L)
                }
                return true
            }
        }

        // 3. Check user-configured TV Key Remapper bindings (Single / Double / Triple / Long Press)
        val bindings = com.example.ui.tv.loadRemappedKeyBindings(prefs)
        val matchedBinding = bindings.firstOrNull {
            it.enabled && it.keyCode == event.keyCode && it.hasAnyAssignedAction()
        }
        if (matchedBinding != null) {
            // Fast zero-latency path: only Single Press is configured (no Double/Triple/Long Press)
            if (!matchedBinding.hasMultiGestureConfigured()) {
                val singleAction = matchedBinding.singlePress
                if (!singleAction.isAssigned) return false
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val allowRepeat = singleAction.actionType in setOf(
                        com.example.ui.tv.RemappedKeyActionType.DPAD_UP,
                        com.example.ui.tv.RemappedKeyActionType.DPAD_DOWN,
                        com.example.ui.tv.RemappedKeyActionType.DPAD_LEFT,
                        com.example.ui.tv.RemappedKeyActionType.DPAD_RIGHT,
                        com.example.ui.tv.RemappedKeyActionType.VOLUME_UP,
                        com.example.ui.tv.RemappedKeyActionType.VOLUME_DOWN
                    )
                    if (event.repeatCount == 0 || allowRepeat) {
                        executeKeyGestureAction(singleAction, matchedBinding.keyCode, prefs)
                    }
                }
                return true
            }

            // Multi-gesture state machine (supports Single, Double, Triple, and Long Press cleanly)
            val state = keyGestureStates.getOrPut(event.keyCode) { MultiPressKeyState() }
            val hasLong = matchedBinding.longPress.isAssigned
            val hasDouble = matchedBinding.doublePress.isAssigned
            val hasTriple = matchedBinding.triplePress.isAssigned
            val maxTapTarget = when {
                hasTriple -> 3
                hasDouble -> 2
                else -> 1
            }

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        state.longPressTriggered = false
                        state.multiTapFinalizeRunnable?.let { mainHandler.removeCallbacks(it) }
                        state.multiTapFinalizeRunnable = null

                        if (hasLong) {
                            state.longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                            val lpRunnable = Runnable {
                                state.longPressTriggered = true
                                state.tapCount = 0
                                state.longPressRunnable = null
                                executeKeyGestureAction(matchedBinding.longPress, matchedBinding.keyCode, prefs)
                            }
                            state.longPressRunnable = lpRunnable
                            mainHandler.postDelayed(lpRunnable, 480L)
                        }
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    state.longPressRunnable?.let { mainHandler.removeCallbacks(it) }
                    state.longPressRunnable = null
                    if (state.longPressTriggered) {
                        state.longPressTriggered = false
                        return true
                    }

                    state.tapCount += 1
                    val countNow = state.tapCount
                    if (countNow >= maxTapTarget) {
                        state.multiTapFinalizeRunnable?.let { mainHandler.removeCallbacks(it) }
                        state.multiTapFinalizeRunnable = null
                        state.tapCount = 0
                        val chosen = when (countNow) {
                            3 -> matchedBinding.triplePress
                            2 -> matchedBinding.doublePress
                            else -> matchedBinding.singlePress
                        }
                        executeKeyGestureAction(chosen, matchedBinding.keyCode, prefs)
                    } else {
                        state.multiTapFinalizeRunnable?.let { mainHandler.removeCallbacks(it) }
                        val finalize = Runnable {
                            val finalCount = state.tapCount
                            state.tapCount = 0
                            state.multiTapFinalizeRunnable = null
                            val chosen = when (finalCount) {
                                3 -> matchedBinding.triplePress
                                2 -> matchedBinding.doublePress
                                else -> matchedBinding.singlePress
                            }
                            executeKeyGestureAction(chosen, matchedBinding.keyCode, prefs)
                        }
                        state.multiTapFinalizeRunnable = finalize
                        mainHandler.postDelayed(finalize, 300L)
                    }
                    return true
                }
            }
            return true
        }

        // 4. Fallback Home button interception if enabled in Control Panel
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            val interceptEnabled = prefs.getBoolean(KEY_INTERCEPT_HOME, true)
            if (!interceptEnabled) return false
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                launchRnaTvHome()
            }
            return true
        }

        return false
    }

    private fun executeKeyGestureAction(
        gestureAction: com.example.ui.tv.KeyGestureAction,
        originalKeyCode: Int,
        prefs: android.content.SharedPreferences
    ) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        when (gestureAction.actionType) {
            com.example.ui.tv.RemappedKeyActionType.NONE_SYSTEM_DEFAULT -> {
                // Perform best-effort system default for common hardware buttons when single-press was unmapped
                when (originalKeyCode) {
                    KeyEvent.KEYCODE_HOME -> {
                        if (prefs.getBoolean(KEY_INTERCEPT_HOME, true)) launchRnaTvHome()
                        else performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                    KeyEvent.KEYCODE_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
                    KeyEvent.KEYCODE_VOLUME_UP -> audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    KeyEvent.KEYCODE_VOLUME_DOWN -> audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    KeyEvent.KEYCODE_VOLUME_MUTE -> audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                        audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    }
                    else -> dispatchPhysicalKeyOrGamepadActionOverExternalApp(originalKeyCode)
                }
            }
            com.example.ui.tv.RemappedKeyActionType.RNA_HOME -> {
                launchRnaTvHome()
            }
            com.example.ui.tv.RemappedKeyActionType.RECENT_APPS_DRAWER -> {
                launchRecentAppsOverlayFromService()
            }
            com.example.ui.tv.RemappedKeyActionType.QUICK_APPS_TASKBAR -> {
                launchTaskbarOverlayFromService()
            }
            com.example.ui.tv.RemappedKeyActionType.OPEN_APP -> {
                launchAppPackageFromService(gestureAction.targetPackage)
            }
            com.example.ui.tv.RemappedKeyActionType.DPAD_UP -> {
                performDirectionalFocusAction(View.FOCUS_UP, "d-pad.up")
            }
            com.example.ui.tv.RemappedKeyActionType.DPAD_DOWN -> {
                performDirectionalFocusAction(View.FOCUS_DOWN, "d-pad.down")
            }
            com.example.ui.tv.RemappedKeyActionType.DPAD_LEFT -> {
                performDirectionalFocusAction(View.FOCUS_LEFT, "d-pad.left")
            }
            com.example.ui.tv.RemappedKeyActionType.DPAD_RIGHT -> {
                performDirectionalFocusAction(View.FOCUS_RIGHT, "d-pad.right")
            }
            com.example.ui.tv.RemappedKeyActionType.DPAD_OK -> {
                performRobustCenterClick(isHold = false, isDouble = false)
            }
            com.example.ui.tv.RemappedKeyActionType.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            com.example.ui.tv.RemappedKeyActionType.VOLUME_UP -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            com.example.ui.tv.RemappedKeyActionType.VOLUME_DOWN -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            com.example.ui.tv.RemappedKeyActionType.VOLUME_MUTE -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            }
            com.example.ui.tv.RemappedKeyActionType.MEDIA_PLAY_PAUSE -> {
                val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                audioManager?.dispatchMediaKeyEvent(down)
                audioManager?.dispatchMediaKeyEvent(up)
            }
            com.example.ui.tv.RemappedKeyActionType.LIVE_VOICE_AI -> {
                com.example.ui.tv.KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(this, prefs)
                launchRnaAssistantOverlay(null)
            }
            com.example.ui.tv.RemappedKeyActionType.AUTONOMOUS_AGENT -> {
                com.example.ui.tv.KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(this, prefs)
                RnaAutonomousAgentController.enterAgentMode()
                syncSystemAgentOverlayWindow()
            }
            com.example.ui.tv.RemappedKeyActionType.RNA_AI_APP -> {
                val aiIntent = Intent(this, RnaAiActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                runCatching { startActivity(aiIntent) }
            }
            com.example.ui.tv.RemappedKeyActionType.RNA_BROWSER -> {
                val browserIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
                    putExtra(MainActivity.EXTRA_OPEN_TV_BROWSER_URL, "https://www.google.com")
                }
                runCatching { startActivity(browserIntent) }
            }
        }
    }

    fun launchRnaAssistantOverlayFromTrigger() {
        launchRnaAssistantOverlay(null)
    }

    private fun performDirectionalFocusAction(direction: Int, cmdName: String) {
        if (!performRobustDirectionalNavigation(direction)) {
            performAgentHardwareCommandOverAnyApp(cmdName)
        }
    }

    fun launchRnaTvHomeFromService() {
        launchRnaTvHome()
    }

    fun launchRecentAppsOverlayFromService() {
        if (!com.example.ui.tv.KeyRemapperBridge.hasUsageStatsPermission(this)) {
            com.example.ui.tv.KeyRemapperBridge.requestUsageStatsPermission(this)
        }
        val intent = Intent(this, RnaQuickOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(RnaQuickOverlayActivity.EXTRA_OVERLAY_MODE, RnaQuickOverlayActivity.MODE_RECENT_APPS)
        }
        runCatching { startActivity(intent) }
    }

    fun launchTaskbarOverlayFromService() {
        val intent = Intent(this, RnaQuickOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(RnaQuickOverlayActivity.EXTRA_OVERLAY_MODE, RnaQuickOverlayActivity.MODE_QUICK_TASKBAR)
        }
        runCatching { startActivity(intent) }
    }

    fun launchAiBarOverlayFromService() {
        launchRnaAssistantOverlay(null)
    }

    fun launchAppPackageFromService(targetPkg: String): Boolean {
        if (targetPkg.isBlank()) return false
        return try {
            when (targetPkg) {
                com.example.ui.tv.RNAAI_PACKAGE_NAME -> {
                    val aiIntent = Intent(this, RnaAiActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(aiIntent)
                    true
                }
                packageName -> {
                    launchRnaTvHome()
                    true
                }
                else -> {
                    val pm = packageManager
                    val launchIntent = pm.getLeanbackLaunchIntentForPackage(targetPkg)
                        ?: pm.getLaunchIntentForPackage(targetPkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun launchRnaAssistantOverlay(initialCommand: String?) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        com.example.ui.tv.KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(this, prefs)
        val intent = Intent(this, RnaAssistantOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            if (!initialCommand.isNullOrBlank()) {
                putExtra(RnaAssistantOverlayActivity.EXTRA_INITIAL_WAKE_QUERY, initialCommand)
            }
        }
        runCatching { startActivity(intent) }
    }

    private fun launchRnaTvHome() {
        mainHandler.post {
            com.example.ui.tv.KeyRemapperBridge.notifyNavigateToHomeTabInMainActivity()
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_RESET_TO_HOME_TAB, true)
        }
        runCatching { startActivity(intent) }
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        runCatching { unregisterReceiver(screenOnTriggerReceiver) }
        runCatching { unregisterReceiver(usbBootReceiver) }
        keyGestureStates.clear()
        removeSystemAgentOverlayWindow()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onInterrupt() {}
}
