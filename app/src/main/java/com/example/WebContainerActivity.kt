package com.example

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.example.data.model.RnaApp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VioletSecondary
import com.example.ui.tv.RnaCursorDesign
import com.example.ui.tv.RnaScrollWheelDesign
import com.example.ui.tv.RnaWebInputOverlay
import com.example.ui.tv.advanceInputModeOnHoldOk
import com.example.ui.tv.stepBackInputModeOnBack
import com.example.ui.wizard.buildFaviconUrlForWebsite
import com.example.ui.wizard.resolveWebsiteLogoToPersistentDataUri
import com.example.util.configureAppCheck
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class WebContainerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_ID = "extra_rna_app_id"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_DIRECT_URL = "extra_direct_url"
        const val EXTRA_DIRECT_TITLE = "extra_direct_title"
        const val EXTRA_BROWSER_MODE = "extra_browser_mode"

        const val MODE_DPAD = 0
        const val MODE_MOUSE = 1
        const val MODE_SCROLL = 2

        const val PREFS_WEB_CONTROLS = "rna_web_controls_prefs"
        const val KEY_CURSOR_DESIGN = "cursor_design"
        const val KEY_SCROLL_DESIGN = "scroll_design"
        const val KEY_BROWSER_DEFAULT_MODE = "browser_default_mode" // "DPAD" or "MOUSE"
        const val KEY_BROWSER_MOUSE_SPEED = "browser_mouse_speed"
        const val KEY_BROWSER_FAST_PERFORMANCE_MODE = "browser_fast_performance_mode"

        const val JS_FAST_BROWSER_OPTIMIZER = """
            javascript:(function() {
                try {
                    if (!document.getElementById('__rna_fast_browser_css__')) {
                        var st = document.createElement('style');
                        st.id = '__rna_fast_browser_css__';
                        st.innerHTML = '*, *::before, *::after { animation: none !important; animation-duration: 0s !important; transition: none !important; transition-duration: 0s !important; scroll-behavior: auto !important; } section, article, main, footer, aside { content-visibility: auto; contain-intrinsic-size: auto 320px; }';
                        (document.head || document.documentElement).appendChild(st);
                    }
                    var imgs = document.querySelectorAll('img');
                    for (var i = 0; i < imgs.length; i++) {
                        if (!imgs[i].getAttribute('loading')) imgs[i].setAttribute('loading', 'lazy');
                        if (!imgs[i].getAttribute('decoding')) imgs[i].setAttribute('decoding', 'async');
                    }
                    var iframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < iframes.length; j++) {
                        if (!iframes[j].getAttribute('loading')) iframes[j].setAttribute('loading', 'lazy');
                    }
                    var media = document.querySelectorAll('video, audio');
                    for (var k = 0; k < media.length; k++) {
                        media[k].setAttribute('preload', 'none');
                        if (!media[k].dataset.rnaUserPlayed && !media[k].paused) {
                            try { media[k].pause(); } catch(e) {}
                        }
                    }
                    if (!window.__rnaRafThrottled) {
                        window.__rnaRafThrottled = true;
                        var origRaf = window.requestAnimationFrame;
                        var lastFrameTime = 0;
                        window.requestAnimationFrame = function(cb) {
                            var now = performance.now();
                            var minInterval = document.hidden ? 1000 : 42;
                            var wait = Math.max(0, minInterval - (now - lastFrameTime));
                            if (wait <= 2 && origRaf) {
                                lastFrameTime = now;
                                return origRaf(cb);
                            }
                            return window.setTimeout(function() {
                                lastFrameTime = performance.now();
                                cb(lastFrameTime);
                            }, wait);
                        };
                        var origSetInterval = window.setInterval;
                        window.setInterval = function(fn, delay) {
                            var clamped = (typeof delay === 'number' && delay < 50) ? 50 : delay;
                            return origSetInterval.apply(this, [fn, clamped].concat(Array.prototype.slice.call(arguments, 2)));
                        };
                    }
                } catch(e) {}
            })()
        """

        const val CSS_FORCE_DARK = """
            javascript:(function() {
                var el = document.getElementById('__rna_dark_style__');
                if (!el) {
                    var style = document.createElement('style');
                    style.id = '__rna_dark_style__';
                    style.innerHTML = 'html { filter: invert(90%) hue-rotate(180deg) !important; background: #101218 !important; } img, video, canvas, iframe, svg { filter: invert(100%) hue-rotate(180deg) !important; }';
                    document.head.appendChild(style);
                }
            })()
        """

        const val CSS_REMOVE_DARK = """
            javascript:(function() {
                var el = document.getElementById('__rna_dark_style__');
                if (el) el.remove();
            })()
        """

        // Injected Spatial D-Pad Navigation & Focus Highlighter for real websites
        const val JS_INIT_SPATIAL_NAV = """
            javascript:(function() {
                if (window.__rnaSpatialInitialized) return;
                window.__rnaSpatialInitialized = true;
                var style = document.createElement('style');
                style.id = '__rna_spatial_style__';
                style.innerHTML = '.__rna_dpad_focus__ { outline: 3px solid #BAC3FF !important; outline-offset: 3px !important; box-shadow: 0 0 18px rgba(186,195,255,0.75) !important; border-radius: 4px !important; transition: outline 0.12s ease !important; }';
                document.head.appendChild(style);

                window.__rnaFocusedEl = null;

                window.__rnaGetFocusable = function() {
                    var selector = 'a[href], button, input:not([type="hidden"]), select, textarea, summary, [role="button"], [role="link"], [role="tab"], [role="menuitem"], [tabindex]:not([tabindex="-1"]), [onclick]';
                    var nodes = Array.prototype.slice.call(document.querySelectorAll(selector));
                    return nodes.filter(function(el) {
                        var rect = el.getBoundingClientRect();
                        var st = window.getComputedStyle(el);
                        return rect.width > 8 && rect.height > 8 && st.visibility !== 'hidden' && st.display !== 'none' && st.opacity !== '0';
                    });
                };

                window.__rnaClearFocus = function() {
                    if (window.__rnaFocusedEl) {
                        window.__rnaFocusedEl.classList.remove('__rna_dpad_focus__');
                    }
                };

                window.__rnaMoveDpad = function(dir) {
                    var keyMap = { 'UP': 'ArrowUp', 'DOWN': 'ArrowDown', 'LEFT': 'ArrowLeft', 'RIGHT': 'ArrowRight' };
                    var codeMap = { 'UP': 38, 'DOWN': 40, 'LEFT': 37, 'RIGHT': 39 };
                    window.dispatchEvent(new KeyboardEvent('keydown', { key: keyMap[dir], code: keyMap[dir], keyCode: codeMap[dir], bubbles: true }));

                    var items = window.__rnaGetFocusable();
                    if (!items.length) {
                        if (dir === 'DOWN') window.scrollBy({top: 160, behavior: 'smooth'});
                        if (dir === 'UP') window.scrollBy({top: -160, behavior: 'smooth'});
                        if (dir === 'RIGHT') window.scrollBy({left: 160, behavior: 'smooth'});
                        if (dir === 'LEFT') window.scrollBy({left: -160, behavior: 'smooth'});
                        return;
                    }

                    if (!window.__rnaFocusedEl || !document.body.contains(window.__rnaFocusedEl)) {
                        var visibleFirst = items.find(function(el) {
                            var r = el.getBoundingClientRect();
                            return r.top >= 0 && r.bottom <= window.innerHeight;
                        }) || items[0];
                        window.__rnaFocusedEl = visibleFirst;
                        window.__rnaFocusedEl.classList.add('__rna_dpad_focus__');
                        window.__rnaFocusedEl.focus({preventScroll: true});
                        window.__rnaFocusedEl.scrollIntoView({behavior: 'smooth', block: 'center', inline: 'nearest'});
                        return;
                    }

                    var curRect = window.__rnaFocusedEl.getBoundingClientRect();
                    var cx = curRect.left + curRect.width / 2;
                    var cy = curRect.top + curRect.height / 2;

                    var best = null;
                    var bestScore = Infinity;

                    items.forEach(function(el) {
                        if (el === window.__rnaFocusedEl) return;
                        var r = el.getBoundingClientRect();
                        var ex = r.left + r.width / 2;
                        var ey = r.top + r.height / 2;
                        var dx = ex - cx;
                        var dy = ey - cy;

                        var valid = false;
                        var primary = 0;
                        var secondary = 0;
                        if (dir === 'RIGHT' && dx > 6) { valid = true; primary = dx; secondary = Math.abs(dy); }
                        else if (dir === 'LEFT' && dx < -6) { valid = true; primary = -dx; secondary = Math.abs(dy); }
                        else if (dir === 'DOWN' && dy > 6) { valid = true; primary = dy; secondary = Math.abs(dx); }
                        else if (dir === 'UP' && dy < -6) { valid = true; primary = -dy; secondary = Math.abs(dx); }

                        if (valid) {
                            var score = primary + secondary * 2.2;
                            if (score < bestScore) {
                                bestScore = score;
                                best = el;
                            }
                        }
                    });

                    if (best) {
                        window.__rnaFocusedEl.classList.remove('__rna_dpad_focus__');
                        window.__rnaFocusedEl = best;
                        best.classList.add('__rna_dpad_focus__');
                        best.focus({preventScroll: true});
                        best.scrollIntoView({behavior: 'smooth', block: 'center', inline: 'nearest'});
                    } else {
                        if (dir === 'DOWN') window.scrollBy({top: 200, behavior: 'smooth'});
                        if (dir === 'UP') window.scrollBy({top: -200, behavior: 'smooth'});
                        if (dir === 'RIGHT') window.scrollBy({left: 160, behavior: 'smooth'});
                        if (dir === 'LEFT') window.scrollBy({left: -160, behavior: 'smooth'});
                    }
                };

                window.__rnaActivateFocused = function() {
                    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, bubbles: true }));
                    var el = window.__rnaFocusedEl || document.activeElement;
                    if (el) {
                        el.focus();
                        el.click();
                        var tag = (el.tagName || '').toUpperCase();
                        return (tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable) ? 'INPUT' : 'CLICKED';
                    }
                    return 'NONE';
                };
            })();
        """
    }

    private var webView: WebView? = null
    private var currentApp by mutableStateOf<RnaApp?>(null)
    private var isBrowserMode by mutableStateOf(false)
    private var currentUrlState by mutableStateOf("")
    private var currentTitleState by mutableStateOf("")
    private var topBarHeightPx by mutableIntStateOf(0)

    private var defaultBaseMode by mutableIntStateOf(MODE_DPAD)
    private var inputMode by mutableIntStateOf(MODE_DPAD)
    private var runtimeMouseSpeed by mutableFloatStateOf(1.2f)
    private var cursorDesign by mutableStateOf(RnaCursorDesign.CLASSIC_ARROW)
    private var scrollWheelDesign by mutableStateOf(RnaScrollWheelDesign.GLASS_PILL_WHEEL)

    private var cursorX by mutableFloatStateOf(400f)
    private var cursorY by mutableFloatStateOf(360f)
    private var isForcedDark by mutableStateOf(false)
    private var isCursorPressed by mutableStateOf(false)
    private var pageLoadProgress by mutableIntStateOf(0)
    private var modeBannerVisible by mutableStateOf(true)
    private var bannerAutoHideJob: Job? = null

    private var activeDirectionKey by mutableIntStateOf(0)
    private var movementJob: Job? = null

    private var okKeyDownTime: Long = 0L
    private var isLongPressTriggered = false
    private var isOkKeyDown = false
    private var holdOkJob: Job? = null
    private var outOfMemoryMessage by mutableStateOf<String?>(null)
    private var appLoadError by mutableStateOf<String?>(null)
    private var webViewReloadEpoch by mutableIntStateOf(0)
    private var customFullscreenView by mutableStateOf<View?>(null)
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var capturedSiteFavicon: Bitmap? = null
    private var isConvertingDirectSite by mutableStateOf(false)

    private fun killWebContainerForLowMemory(reason: String) {
        if (outOfMemoryMessage != null) return
        outOfMemoryMessage = reason
        val wv = webView
        webView = null
        com.example.ui.tv.RnaControllerNetworkBridge.activeWebViewRef = null
        runCatching {
            wv?.stopLoading()
            wv?.loadUrl("about:blank")
            wv?.clearHistory()
            wv?.clearCache(true)
            (wv?.parent as? ViewGroup)?.removeView(wv)
            wv?.removeAllViews()
            wv?.destroy()
        }
        runCatching { Runtime.getRuntime().gc() }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            runOnUiThread {
                killWebContainerForLowMemory("Device ran out of memory — webpage closed safely to keep RNA. TV running.")
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        runOnUiThread {
            killWebContainerForLowMemory("Device ran out of memory — webpage closed safely to keep RNA. TV running.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val dm = resources.displayMetrics
        cursorX = dm.widthPixels / 2f
        cursorY = dm.heightPixels / 2f

        val controlPrefs = getSharedPreferences(PREFS_WEB_CONTROLS, Context.MODE_PRIVATE)
        cursorDesign = RnaCursorDesign.fromId(controlPrefs.getString(KEY_CURSOR_DESIGN, null))
        scrollWheelDesign = RnaScrollWheelDesign.fromId(controlPrefs.getString(KEY_SCROLL_DESIGN, null))

        loadIntentTarget(intent)

        setContent {
            MyApplicationTheme {
                ContainerScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadIntentTarget(intent)
    }

    private fun loadIntentTarget(targetIntent: Intent) {
        val controlPrefs = getSharedPreferences(PREFS_WEB_CONTROLS, Context.MODE_PRIVATE)
        val savedBrowserDefault = controlPrefs.getString(KEY_BROWSER_DEFAULT_MODE, "DPAD") ?: "DPAD"

        val directUrl = targetIntent.getStringExtra(EXTRA_DIRECT_URL)
        val directTitle = targetIntent.getStringExtra(EXTRA_DIRECT_TITLE) ?: "RNA. TV Browser"
        val browserModeExtra = targetIntent.getBooleanExtra(EXTRA_BROWSER_MODE, directUrl != null)
        isBrowserMode = browserModeExtra
        appLoadError = null

        val appIdExtra = targetIntent.getStringExtra(EXTRA_APP_ID)
        val componentName = targetIntent.component?.className ?: ""

        val slotNumber = if (componentName.contains("Slot")) {
            val match = Regex("Slot(\\d+)Alias").find(componentName)
            match?.groupValues?.get(1)?.toIntOrNull()
        } else {
            targetIntent.getIntExtra(EXTRA_SLOT_INDEX, -1).takeIf { it > 0 }
        }

        val repo = (application as RnaApplication).repository
        lifecycleScope.launch {
            val app = when {
                directUrl != null -> {
                    RnaApp(
                        id = "direct_search",
                        packageName = "com.rna.browser",
                        title = directTitle,
                        sourceType = "URL",
                        sourceUriOrContent = directUrl,
                        isTvEnabled = true,
                        defaultInputMode = savedBrowserDefault,
                        mouseSpeed = controlPrefs.getFloat(KEY_BROWSER_MOUSE_SPEED, 1.2f),
                        forceDarkMode = false,
                        showLoadingBar = true,
                        loadingBarColor = "#BAC3FF"
                    )
                }
                appIdExtra != null -> repo.getAppById(appIdExtra)
                slotNumber != null -> repo.getAppBySlot(slotNumber)
                else -> null
            }

            if (app != null) {
                val previousId = currentApp?.id
                currentApp = app
                currentUrlState = app.sourceUriOrContent
                currentTitleState = app.title
                isForcedDark = app.forceDarkMode
                runtimeMouseSpeed = app.mouseSpeed.coerceIn(0.5f, 3.0f)
                val base = if (app.defaultInputMode == "MOUSE") MODE_MOUSE else MODE_DPAD
                defaultBaseMode = base
                inputMode = base
                if (app.id != "direct_search") {
                    repo.markAppLaunched(app.id)
                }
                if (previousId != null && previousId != app.id) {
                    webView?.let { loadAppContent(it, app) }
                }
                triggerModeToast()
            } else {
                appLoadError = "Could not find this RNA. web app in storage. It may have been uninstalled."
            }
        }
    }

    private fun convertCurrentDirectWebsiteToApp() {
        if (isConvertingDirectSite) return
        val liveUrl = webView?.url?.takeIf { it.isNotBlank() && !it.startsWith("about:") }
            ?: currentUrlState.takeIf { it.isNotBlank() }
            ?: return
        val normalizedUrl = if (liveUrl.startsWith("http://", true) || liveUrl.startsWith("https://", true)) {
            liveUrl
        } else {
            "https://$liveUrl"
        }
        val title = (webView?.title?.takeIf { it.isNotBlank() } ?: currentTitleState)
            .take(30)
            .ifBlank { "Web App" }
        isConvertingDirectSite = true
        Toast.makeText(this, "Converting '$title' to RNA. App...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val persistentIcon = resolveWebsiteLogoToPersistentDataUri(
                context = this@WebContainerActivity,
                candidateUri = buildFaviconUrlForWebsite(normalizedUrl),
                websiteUrl = normalizedUrl,
                fallbackTitle = title,
                capturedWebViewFavicon = capturedSiteFavicon
            )
            val appId = UUID.randomUUID().toString()
            val slug = title.lowercase().replace("[^a-z0-9]".toRegex(), "").take(24).ifBlank { "webapp" }
            val newApp = RnaApp(
                id = appId,
                packageName = "com.rna.$slug.${appId.take(4)}",
                title = title,
                version = "1.0.0",
                sourceType = "URL",
                sourceUriOrContent = normalizedUrl,
                iconBase64 = persistentIcon,
                bannerBase64 = persistentIcon,
                isTvEnabled = true,
                isTvFavorite = true,
                defaultInputMode = if (defaultBaseMode == MODE_MOUSE) "MOUSE" else "DPAD",
                mouseSpeed = runtimeMouseSpeed,
                forceDarkMode = isForcedDark,
                showLoadingBar = true,
                loadingBarColor = "#BAC3FF"
            )
            val repo = (application as RnaApplication).repository
            repo.installApp(newApp, useSlotAlias = true, pinShortcut = true)
            isConvertingDirectSite = false
            Toast.makeText(
                this@WebContainerActivity,
                "Installed '$title' to RNA. TV Home & Apps!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        movementJob?.cancel()
        bannerAutoHideJob?.cancel()
        // Strictly destroy WebView and release all DOM/JS memory when container closes
        webView?.let { wv ->
            try {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            } catch (_: Exception) {
            }
        }
        webView = null
        super.onDestroy()
    }

    private fun triggerModeToast() {
        modeBannerVisible = true
        bannerAutoHideJob?.cancel()
        bannerAutoHideJob = lifecycleScope.launch {
            delay(3000)
            modeBannerVisible = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun ContainerScreen() {
        val app = currentApp
        var showAddressDialog by remember { mutableStateOf(false) }
        var showControlsCustomizeDialog by remember { mutableStateOf(false) }

        // Apply Dark Mode CSS only when isForcedDark actually changes (never on every recomposition)
        LaunchedEffect(isForcedDark) {
            val wv = webView ?: return@LaunchedEffect
            if (isForcedDark) {
                wv.loadUrl(CSS_FORCE_DARK)
            } else {
                wv.loadUrl(CSS_REMOVE_DARK)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0E1017))
        ) {
            if (appLoadError != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF161B2A),
                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Web App Unavailable",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            Text(
                                text = appLoadError.orEmpty(),
                                color = Color(0xFFA0AFC6),
                                fontSize = 13.5.sp
                            )
                            Button(
                                onClick = { finish() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF0A0E17)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Return Home", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (app == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Initializing RNA. Container...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Browser Top Bar (Visible when launched in Browser Mode)
                    if (isBrowserMode) {
                        RnaBrowserTopBar(
                            currentTitle = currentTitleState.ifBlank { app.title },
                            currentUrl = currentUrlState.ifBlank { app.sourceUriOrContent },
                            inputMode = inputMode,
                            mouseSpeed = runtimeMouseSpeed,
                            isForcedDark = isForcedDark,
                            isConvertingToApp = isConvertingDirectSite,
                            onHomeClick = { finish() },
                            onBackClick = {
                                if (webView?.canGoBack() == true) webView?.goBack() else finish()
                            },
                            onForwardClick = {
                                if (webView?.canGoForward() == true) webView?.goForward()
                            },
                            onReloadClick = { webView?.reload() },
                            onEditAddressClick = { showAddressDialog = true },
                            onConvertToAppClick = { convertCurrentDirectWebsiteToApp() },
                            onCycleInputMode = {
                                val next = advanceInputModeOnHoldOk(inputMode, defaultBaseMode)
                                if (next != MODE_DPAD) {
                                    webView?.evaluateJavascript("if(window.__rnaClearFocus) window.__rnaClearFocus();", null)
                                }
                                inputMode = next
                                triggerModeToast()
                            },
                            onOpenControlsSettings = { showControlsCustomizeDialog = true },
                            onToggleDark = { isForcedDark = !isForcedDark },
                            modifier = Modifier.onGloballyPositioned { coords ->
                                topBarHeightPx = coords.size.height
                            }
                        )
                    }

                    // 2. WebView Container
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (outOfMemoryMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0A0D16)),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(26.dp),
                                    color = Color(0xFF141929),
                                    border = BorderStroke(2.dp, Color(0xFFF59E0B)),
                                    modifier = Modifier.padding(28.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Device Ran Out of Memory",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 21.sp
                                        )
                                        Text(
                                            text = outOfMemoryMessage.orEmpty(),
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = {
                                                    outOfMemoryMessage = null
                                                    webViewReloadEpoch += 1
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyanPrimary,
                                                    contentColor = Color(0xFF090D16)
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Reload Page", fontWeight = FontWeight.Black)
                                            }
                                            OutlinedButton(
                                                onClick = { finish() },
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Return Home", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            androidx.compose.runtime.key(webViewReloadEpoch) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            webView = this
                                            com.example.ui.tv.RnaControllerNetworkBridge.activeWebViewRef = java.lang.ref.WeakReference(this)
                                            val fastBrowserOn = ctx.getSharedPreferences(PREFS_WEB_CONTROLS, Context.MODE_PRIVATE)
                                                .getBoolean(KEY_BROWSER_FAST_PERFORMANCE_MODE, true)
                                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                                            }
                                            isFocusable = true
                                            isFocusableInTouchMode = true
                                            CookieManager.getInstance().setAcceptCookie(true)
                                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                loadWithOverviewMode = true
                                                useWideViewPort = true
                                                allowFileAccess = true
                                                allowContentAccess = true
                                                mediaPlaybackRequiresUserGesture = fastBrowserOn
                                                cacheMode = WebSettings.LOAD_DEFAULT
                                                setSupportZoom(true)
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                                setSupportMultipleWindows(false)
                                                userAgentString = settings.userAgentString.replace("; wv", "")
                                            }

                                            webViewClient = object : WebViewClient() {
                                                override fun onRenderProcessGone(
                                                    view: WebView?,
                                                    detail: RenderProcessGoneDetail?
                                                ): Boolean {
                                                    runOnUiThread {
                                                        killWebContainerForLowMemory("Device ran out of memory — webpage closed safely to keep RNA. TV running.")
                                                    }
                                                    return true
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    if (!url.isNullOrBlank()) {
                                                        currentUrlState = url
                                                    }
                                                    if (!view?.title.isNullOrBlank()) {
                                                        currentTitleState = view?.title ?: currentTitleState
                                                    }
                                                    view?.loadUrl(JS_INIT_SPATIAL_NAV)
                                                    if (fastBrowserOn) {
                                                        view?.loadUrl(JS_FAST_BROWSER_OPTIMIZER)
                                                    }
                                                    view?.evaluateJavascript(com.example.ui.tv.RnaControllerNetworkBridge.buildXboxGamepadSpoofJs(), null)
                                                    if (isForcedDark) {
                                                        view?.loadUrl(CSS_FORCE_DARK)
                                                    }
                                                }

                                                override fun shouldOverrideUrlLoading(
                                                    view: WebView?,
                                                    request: WebResourceRequest?
                                                ): Boolean {
                                                    val target = request?.url?.toString() ?: return false
                                                    val lower = target.lowercase()
                                                    if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) {
                                                        currentUrlState = target
                                                        return false
                                                    }
                                                    // Handle intent:// fallback URLs cleanly instead of showing ERR_UNKNOWN_URL_SCHEME
                                                    if (lower.startsWith("intent://")) {
                                                        runCatching {
                                                            val parsed = Intent.parseUri(target, Intent.URI_INTENT_SCHEME)
                                                            val fallback = parsed.getStringExtra("browser_fallback_url")
                                                            if (!fallback.isNullOrBlank()) {
                                                                currentUrlState = fallback
                                                                view?.loadUrl(fallback)
                                                                return true
                                                            }
                                                        }
                                                        return true
                                                    }
                                                    if (lower.startsWith("mailto:") || lower.startsWith("tel:") || lower.startsWith("market://")) {
                                                        runCatching {
                                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                                                        }
                                                        return true
                                                    }
                                                    return true
                                                }
                                            }

                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                    super.onProgressChanged(view, newProgress)
                                                    pageLoadProgress = newProgress
                                                }

                                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                                    super.onReceivedTitle(view, title)
                                                    if (!title.isNullOrBlank()) {
                                                        currentTitleState = title
                                                    }
                                                }

                                                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                                    super.onReceivedIcon(view, icon)
                                                    if (icon != null && !icon.isRecycled && icon.width >= 16 && icon.height >= 16) {
                                                        capturedSiteFavicon = icon.copy(Bitmap.Config.ARGB_8888, false)
                                                    }
                                                }

                                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                                    if (customFullscreenView != null) {
                                                        callback?.onCustomViewHidden()
                                                        return
                                                    }
                                                    customFullscreenView = view
                                                    customViewCallback = callback
                                                }

                                                override fun onHideCustomView() {
                                                    customFullscreenView = null
                                                    runCatching { customViewCallback?.onCustomViewHidden() }
                                                    customViewCallback = null
                                                }

                                                override fun onPermissionRequest(request: PermissionRequest?) {
                                                    request?.grant(request.resources)
                                                }

                                                override fun onGeolocationPermissionsShowPrompt(
                                                    origin: String?,
                                                    callback: GeolocationPermissions.Callback?
                                                ) {
                                                    callback?.invoke(origin, true, false)
                                                }
                                            }

                                            loadAppContent(this, app)
                                        }
                                    }
                                )
                            }
                        }

                        // Fullscreen HTML5 Video Container Overlay
                        customFullscreenView?.let { fsView ->
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                                        addView(
                                            fsView,
                                            FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        // Top Loading Progress Bar
                        if (app.showLoadingBar && pageLoadProgress in 1..99) {
                            val barColor = parseHexColor(app.loadingBarColor, CyanPrimary)
                            LinearProgressIndicator(
                                progress = { pageLoadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (!isBrowserMode) Modifier.statusBarsPadding() else Modifier)
                                    .height(3.dp)
                                    .align(Alignment.TopCenter)
                                    .testTag("container_top_loading_bar"),
                                color = barColor,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }

                // 3. Unified 3-Stage Overlay: Virtual Mouse Cursor (in MODE_MOUSE) & Scroll Wheel HUD (in MODE_SCROLL)
                RnaWebInputOverlay(
                    inputMode = inputMode,
                    defaultBaseMode = defaultBaseMode,
                    cursorX = cursorX,
                    cursorY = cursorY,
                    isCursorPressed = isCursorPressed,
                    cursorDesign = cursorDesign,
                    scrollWheelDesign = scrollWheelDesign,
                    mouseSpeed = runtimeMouseSpeed,
                    modeBannerVisible = modeBannerVisible,
                    activeScrollDirection = activeDirectionKey,
                    onScrollStepClick = { dx, dy ->
                        webView?.scrollBy(dx, dy)
                        webView?.evaluateJavascript("window.scrollBy({left: $dx, top: $dy, behavior: 'smooth'});", null)
                    }
                )

                // 4. Compact Bottom-Right Controls & Dark Mode Floating Pills (when not in Browser Mode)
                if (!isBrowserMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xAA141824),
                            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { showControlsCustomizeDialog = true }
                                .testTag("container_controls_settings_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Customize D-Pad, Mouse & Scroll",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isForcedDark) VioletSecondary.copy(alpha = 0.85f) else Color(0x88141824),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { isForcedDark = !isForcedDark }
                                .testTag("container_dark_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isForcedDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Force Dark Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Controls & Designs Customization Dialog (Default Mode, Speed, Cursor & Scroll Wheel Designs)
                if (showControlsCustomizeDialog) {
                    val prefs = remember { getSharedPreferences(PREFS_WEB_CONTROLS, Context.MODE_PRIVATE) }
                    Dialog(onDismissRequest = { showControlsCustomizeDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202B)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "D-Pad, Mouse & Scroll Wheel Settings",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )

                                Text(
                                    text = "Default Input Mode (Hold OK to cycle • Back to step down):",
                                    color = Color(0xFFB5B6C6),
                                    fontSize = 11.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            defaultBaseMode = MODE_DPAD
                                            inputMode = MODE_DPAD
                                            prefs.edit().putString(KEY_BROWSER_DEFAULT_MODE, "DPAD").apply()
                                            triggerModeToast()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (defaultBaseMode == MODE_DPAD) CyanPrimary else Color(0xFF282A37),
                                            contentColor = if (defaultBaseMode == MODE_DPAD) Color(0xFF12131A) else Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("D-Pad Default", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            defaultBaseMode = MODE_MOUSE
                                            inputMode = MODE_MOUSE
                                            prefs.edit().putString(KEY_BROWSER_DEFAULT_MODE, "MOUSE").apply()
                                            triggerModeToast()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (defaultBaseMode == MODE_MOUSE) CyanPrimary else Color(0xFF282A37),
                                            contentColor = if (defaultBaseMode == MODE_MOUSE) Color(0xFF12131A) else Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Mouse Default", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Text("Mouse Cursor Design:", color = Color(0xFFB5B6C6), fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RnaCursorDesign.entries.forEach { design ->
                                        val selected = cursorDesign == design
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) CyanPrimary.copy(alpha = 0.22f) else Color(0xFF282A37),
                                            border = BorderStroke(1.dp, if (selected) CyanPrimary else Color.Transparent),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    cursorDesign = design
                                                    prefs.edit().putString(KEY_CURSOR_DESIGN, design.id).apply()
                                                }
                                        ) {
                                            Text(
                                                text = design.label.substringBefore(" "),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Text("Scroll Wheel Design:", color = Color(0xFFB5B6C6), fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RnaScrollWheelDesign.entries.forEach { design ->
                                        val selected = scrollWheelDesign == design
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) VioletSecondary.copy(alpha = 0.22f) else Color(0xFF282A37),
                                            border = BorderStroke(1.dp, if (selected) VioletSecondary else Color.Transparent),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    scrollWheelDesign = design
                                                    prefs.edit().putString(KEY_SCROLL_DESIGN, design.id).apply()
                                                }
                                        ) {
                                            Text(
                                                text = design.label.substringBefore(" "),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { showControlsCustomizeDialog = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanPrimary,
                                            contentColor = Color(0xFF12131A)
                                        )
                                    ) {
                                        Text("Done", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Browser URL / Search Input Dialog
                if (showAddressDialog) {
                    var inputUrl by remember { mutableStateOf(currentUrlState) }
                    Dialog(onDismissRequest = { showAddressDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202B)),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Navigate or Search Web",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    label = { Text("Enter URL or Search Query") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyanPrimary,
                                        unfocusedBorderColor = Color(0xFF454653),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { showAddressDialog = false }) {
                                        Text("Cancel", color = Color(0xFFC6C5D0))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val trimmed = inputUrl.trim()
                                            if (trimmed.isNotEmpty()) {
                                                val target = when {
                                                    trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                                                    trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
                                                    else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
                                                }
                                                currentUrlState = target
                                                webView?.loadUrl(target)
                                            }
                                            showAddressDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanPrimary,
                                            contentColor = Color(0xFF172778)
                                        )
                                    ) {
                                        Text("Go", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RnaBrowserTopBar(
        currentTitle: String,
        currentUrl: String,
        inputMode: Int,
        mouseSpeed: Float,
        isForcedDark: Boolean,
        isConvertingToApp: Boolean = false,
        onHomeClick: () -> Unit,
        onBackClick: () -> Unit,
        onForwardClick: () -> Unit,
        onReloadClick: () -> Unit,
        onEditAddressClick: () -> Unit,
        onConvertToAppClick: () -> Unit,
        onCycleInputMode: () -> Unit,
        onOpenControlsSettings: () -> Unit,
        onToggleDark: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            color = Color(0xFF161822),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .testTag("rna_browser_top_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CyanPrimary,
                    modifier = Modifier
                        .clickable { onHomeClick() }
                        .testTag("browser_home_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "RNA. TV Home",
                            tint = Color(0xFF172778),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Home",
                            color = Color(0xFF172778),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onBackClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFE3E1EC),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onForwardClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = Color(0xFFE3E1EC),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onReloadClick, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color(0xFFE3E1EC),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF222534),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEditAddressClick() }
                        .testTag("browser_address_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTitle,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                color = Color(0xFF90909E),
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Convert current website to standalone RNA. App
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldSuccess.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, EmeraldSuccess),
                    modifier = Modifier
                        .clickable(enabled = !isConvertingToApp) { onConvertToAppClick() }
                        .testTag("browser_convert_to_app_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = "Convert to App",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isConvertingToApp) "Saving..." else "Convert to App",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Mouse / D-Pad / Scroll Mode Switcher Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (inputMode == MODE_MOUSE) Color(0xFF303F9F) else Color(0xFF282A37),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .clickable { onCycleInputMode() }
                        .testTag("browser_mouse_mode_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = when (inputMode) {
                                MODE_MOUSE -> Icons.Default.Mouse
                                MODE_SCROLL -> Icons.Default.SwapVert
                                else -> Icons.Default.Tv
                            },
                            contentDescription = "Switch Input Mode",
                            tint = CyanPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when (inputMode) {
                                MODE_MOUSE -> "Mouse"
                                MODE_SCROLL -> "Scroll"
                                else -> "D-Pad"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Controls Settings & Speed Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF282A37),
                    modifier = Modifier
                        .clickable { onOpenControlsSettings() }
                        .testTag("browser_mouse_speed_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Controls & Speed",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", mouseSpeed)}x",
                            color = Color(0xFFE3E1EC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onToggleDark,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("container_dark_toggle")
                ) {
                    Icon(
                        imageVector = if (isForcedDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Dark Mode",
                        tint = if (isForcedDark) VioletSecondary else Color(0xFFE3E1EC),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    private fun parseHexColor(hex: String, fallback: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }

    private fun loadAppContent(wv: WebView, app: RnaApp) {
        when (app.sourceType.uppercase()) {
            "URL" -> {
                val trimmed = app.sourceUriOrContent.trim()
                val url = if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.startsWith("file://", ignoreCase = true)
                ) {
                    trimmed
                } else {
                    "https://$trimmed"
                }
                wv.loadUrl(url)
            }
            "CODE", "HTML" -> {
                wv.loadDataWithBaseURL("https://rna.local/", app.sourceUriOrContent, "text/html", "UTF-8", null)
            }
            "BUNDLE" -> {
                wv.loadUrl(app.sourceUriOrContent)
            }
        }
    }

    // ── Smooth 60fps 3-Stage Input Engine: D-Pad -> Hold OK for Mouse -> Hold OK for Scroll -> Back Steps Down ─────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Let Compose handle D-Pad navigation when showing OOM or error UI instead of hijacking keys
        if (outOfMemoryMessage != null || appLoadError != null || currentApp == null) {
            return super.onKeyDown(keyCode, event)
        }
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!isOkKeyDown) {
                    isOkKeyDown = true
                    okKeyDownTime = SystemClock.uptimeMillis()
                    isLongPressTriggered = false
                    holdOkJob?.cancel()
                    holdOkJob = lifecycleScope.launch {
                        delay(420L)
                        if (isOkKeyDown && !isLongPressTriggered) {
                            isLongPressTriggered = true
                            val nextMode = advanceInputModeOnHoldOk(inputMode, defaultBaseMode)
                            if (nextMode != MODE_DPAD) {
                                webView?.evaluateJavascript("if(window.__rnaClearFocus) window.__rnaClearFocus();", null)
                            }
                            inputMode = nextMode
                            triggerModeToast()
                        }
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (inputMode == MODE_DPAD) {
                    if (event?.repeatCount == 0 || (event?.repeatCount ?: 0) % 2 == 0) {
                        val dir = when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_UP -> "UP"
                            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
                            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
                            else -> "RIGHT"
                        }
                        webView?.evaluateJavascript(
                            "if(window.__rnaMoveDpad) window.__rnaMoveDpad('$dir');",
                            null
                        )
                    }
                    return true
                } else {
                    if (activeDirectionKey != keyCode) {
                        activeDirectionKey = keyCode
                        startContinuousMovement(keyCode)
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startContinuousMovement(keyCode: Int) {
        movementJob?.cancel()
        movementJob = lifecycleScope.launch {
            var ticks = 0
            val speedMultiplier = runtimeMouseSpeed.coerceIn(0.5f, 3.0f)
            val screenW = resources.displayMetrics.widthPixels.toFloat()
            val screenH = resources.displayMetrics.heightPixels.toFloat()
            val edgeMarginY = screenH * 0.12f
            val edgeMarginX = screenW * 0.08f

            while (isActive && activeDirectionKey == keyCode) {
                ticks++
                val accel = (0.85f + (ticks.coerceAtMost(22) * 0.08f)) * speedMultiplier

                if (inputMode == MODE_MOUSE) {
                    val delta = 8.5f * accel
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            cursorX = (cursorX - delta).coerceAtLeast(6f)
                            if (cursorX < edgeMarginX) {
                                webView?.scrollBy((-10 * speedMultiplier).toInt(), 0)
                                webView?.evaluateJavascript("window.scrollBy(-12, 0);", null)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            cursorX = (cursorX + delta).coerceAtMost(screenW - 6f)
                            if (cursorX > screenW - edgeMarginX) {
                                webView?.scrollBy((10 * speedMultiplier).toInt(), 0)
                                webView?.evaluateJavascript("window.scrollBy(12, 0);", null)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            cursorY = (cursorY - delta).coerceAtLeast(6f)
                            if (cursorY < edgeMarginY + topBarHeightPx) {
                                val scrollAmt = (-14 * accel).toInt()
                                webView?.scrollBy(0, scrollAmt)
                                webView?.evaluateJavascript("window.scrollBy(0, $scrollAmt);", null)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            cursorY = (cursorY + delta).coerceAtMost(screenH - 6f)
                            if (cursorY > screenH - edgeMarginY) {
                                val scrollAmt = (14 * accel).toInt()
                                webView?.scrollBy(0, scrollAmt)
                                webView?.evaluateJavascript("window.scrollBy(0, $scrollAmt);", null)
                            }
                        }
                    }
                } else if (inputMode == MODE_SCROLL) {
                    val scrollStep = (24f * accel).toInt()
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            webView?.scrollBy(0, -scrollStep)
                            webView?.evaluateJavascript("window.scrollBy(0, -$scrollStep);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            webView?.scrollBy(0, scrollStep)
                            webView?.evaluateJavascript("window.scrollBy(0, $scrollStep);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            webView?.scrollBy(-scrollStep, 0)
                            webView?.evaluateJavascript("window.scrollBy(-$scrollStep, 0);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            webView?.scrollBy(scrollStep, 0)
                            webView?.evaluateJavascript("window.scrollBy($scrollStep, 0);", null)
                        }
                    }
                }
                delay(16L)
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (outOfMemoryMessage != null || appLoadError != null || currentApp == null) {
            return super.onKeyUp(keyCode, event)
        }
        if (keyCode == activeDirectionKey) {
            activeDirectionKey = 0
            movementJob?.cancel()
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            isOkKeyDown = false
            holdOkJob?.cancel()
            if (!isLongPressTriggered) {
                when (inputMode) {
                    MODE_DPAD -> {
                        webView?.evaluateJavascript(
                            "if(window.__rnaActivateFocused) window.__rnaActivateFocused(); else 'NONE';"
                        ) { result ->
                            if (result?.contains("INPUT") == true) {
                                webView?.requestFocus()
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                webView?.let { imm?.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
                            }
                        }
                    }
                    MODE_MOUSE -> {
                        simulateClick(cursorX, cursorY)
                    }
                    MODE_SCROLL -> {
                        // Short tap in Scroll Mode steps back to Mouse Mode
                        inputMode = MODE_MOUSE
                        triggerModeToast()
                    }
                }
            }
            isLongPressTriggered = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun simulateClick(x: Float, y: Float) {
        isCursorPressed = true
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        if (isBrowserMode && y <= topBarHeightPx && topBarHeightPx > 0) {
            val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val upEvent = MotionEvent.obtain(downTime, eventTime + 45, MotionEvent.ACTION_UP, x, y, 0)
            window.decorView.dispatchTouchEvent(downEvent)
            window.decorView.postDelayed({
                window.decorView.dispatchTouchEvent(upEvent)
                downEvent.recycle()
                upEvent.recycle()
                isCursorPressed = false
            }, 50)
            return
        }

        val wv = webView ?: return
        val webY = (y - (if (isBrowserMode) topBarHeightPx else 0)).coerceAtLeast(1f)
        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, webY, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 45, MotionEvent.ACTION_UP, x, webY, 0)

        wv.dispatchTouchEvent(downEvent)
        wv.postDelayed({
            wv.dispatchTouchEvent(upEvent)
            downEvent.recycle()
            upEvent.recycle()
            isCursorPressed = false
        }, 50)

        val density = resources.displayMetrics.density
        val cssX = (x / density).toInt()
        val cssY = (webY / density).toInt()
        wv.evaluateJavascript(
            """
            (function() {
                var el = document.elementFromPoint($cssX, $cssY);
                if (el) {
                    el.focus();
                    var tag = (el.tagName || '').toUpperCase();
                    if (tag === 'INPUT' || tag === 'TEXTAREA' || el.isContentEditable) return 'INPUT';
                }
                return 'OK';
            })();
            """.trimIndent()
        ) { res ->
            if (res?.contains("INPUT") == true) {
                wv.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(wv, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (customFullscreenView != null) {
            customFullscreenView = null
            runCatching { customViewCallback?.onCustomViewHidden() }
            customViewCallback = null
            return
        }
        val previousMode = stepBackInputModeOnBack(inputMode, defaultBaseMode)
        if (previousMode != null) {
            inputMode = previousMode
            triggerModeToast()
            return
        }
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
