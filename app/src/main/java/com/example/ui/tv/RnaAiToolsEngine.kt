package com.example.ui.tv

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.RnaFileExplorerActivity
import com.example.RnaHomeInterceptorService
import com.example.WebContainerActivity
import com.example.data.RnaAppEntity
import com.example.data.RnaDatabase
import com.example.data.model.RnaApp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.wizard.buildFaviconUrlForWebsite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.UUID

data class RnaAppHoverPreview(
    val appId: String,
    val appName: String,
    val displaySubtitle: String, // Shows URL or "Interactive HTML5 TV App" (never raw HTML)
    val logoUri: String,
    val createdApp: RnaApp
)

data class RnaToolsExecutionReport(
    val cleanedText: String,
    val executedSummaries: List<String>,
    val createdOrUpdatedApp: RnaApp? = null
)

/**
 * Shared engine for `$start [command] rna-tools;` and `$start ... app-tools;` across:
 * 1. Normal AI Voice/Type Assistant (`TvVoiceAssistantOverlay`)
 * 2. Full-Screen RNAAI App (`RnaAiTvAppScreen`)
 * 3. Autonomous Screen Agent (`RnaAutonomousAgent`)
 */
object RnaAiToolsEngine {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _hoverAppPreview = MutableStateFlow<RnaAppHoverPreview?>(null)
    val hoverAppPreview: StateFlow<RnaAppHoverPreview?> = _hoverAppPreview.asStateFlow()

    // Live callbacks wired by GoogleTvLauncherScreen so changes reflect immediately on screen
    var onApplyWallpaperCallback: ((wallpaperAssetOrUri: String, targetProfileName: String?) -> Unit)? = null
    var onSwitchProfileByNameCallback: ((profileName: String) -> Unit)? = null
    var onApplyControlPanelSettingCallback: ((settingKey: String, settingValue: String) -> Unit)? = null
    var onLaunchAppCallback: ((packageOrName: String) -> Unit)? = null
    var onAppCreatedOrUpdatedCallback: ((RnaApp) -> Unit)? = null

    // Staged builder state when multiple app-tools commands arrive in one or across turns
    private var stagedAppName: String = "RNA Custom App"
    private var stagedAppLogoUri: String = ""
    private var stagedAppUrl: String = ""
    private var stagedAppHtml: String = ""

    fun dismissHoverPreview() {
        _hoverAppPreview.value = null
    }

    /**
     * Generates a data:image/svg+xml;base64 URI icon with vibrant TV styling for `$start app-name.[name].img-gen app-tools;`.
     */
    fun generateStyledSvgLogoDataUri(appName: String): String {
        val clean = appName.trim().ifBlank { "App" }
        val initials = clean.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { clean.take(2).uppercase(Locale.US) }
        val hash = clean.hashCode()
        val palettes = listOf(
            "#06B6D4" to "#3B82F6",
            "#8B5CF6" to "#EC4899",
            "#10B981" to "#059669",
            "#F59E0B" to "#EF4444",
            "#6366F1" to "#A855F7",
            "#14B8A6" to "#0284C7"
        )
        val (c1, c2) = palettes[(hash and 0x7FFFFFFF) % palettes.size]
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 256 256">
              <defs>
                <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="$c1"/>
                  <stop offset="100%" stop-color="$c2"/>
                </linearGradient>
              </defs>
              <rect width="256" height="256" rx="56" fill="url(#g)"/>
              <circle cx="200" cy="56" r="46" fill="#FFFFFF" fill-opacity="0.14"/>
              <text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle"
                    fill="#FFFFFF" font-family="sans-serif" font-weight="900" font-size="96">$initials</text>
            </svg>
        """.trimIndent()
        val b64 = Base64.encodeToString(svg.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "data:image/svg+xml;base64,$b64"
    }

    /**
     * Builds the complete system prompt section describing all installed apps, wallpapers,
     * profiles, Control Panel settings, `rna-tools` syntax, and `app-tools` syntax.
     */
    fun buildRnaToolsAndAppToolsSystemPrompt(
        context: Context,
        installedApps: List<UnifiedLauncherApp>
    ): String {
        val prefs = context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val profiles = runCatching { loadTvProfiles(prefs) }.getOrDefault(emptyList())
        val profilesListStr = profiles.joinToString(", ") { "\"${it.name}\" (id=${it.id}, wallpaper=${it.wallpaperUri})" }
            .ifBlank { "\"Main\"" }

        // Gather all installed Android TV packages from PackageManager + UnifiedLauncherApp list
        val pm = context.packageManager
        val systemPackages = runCatching {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            val leanback = pm.queryIntentActivities(mainIntent, 0)
            val stdIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val standard = pm.queryIntentActivities(stdIntent, 0)
            (leanback + standard)
                .mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                    if (label.isNotEmpty()) "$label -> $pkg" else null
                }
                .distinct()
        }.getOrDefault(emptyList())

        val combinedAppsText = buildString {
            installedApps.forEach { app ->
                appendLine("  - \"${app.title}\" -> ${app.packageName}")
            }
            systemPackages.take(80).forEach { entry ->
                appendLine("  - $entry")
            }
            appendLine("  - \"RNA File Explorer\" -> com.rna.fileexplorer")
            appendLine("  - \"YouTube\" -> com.google.android.youtube.tv")
            appendLine("  - \"Netflix\" -> com.netflix.mediaclient")
            appendLine("  - \"Prime Video\" -> com.amazon.amazonvideo.livingroom")
            appendLine("  - \"Disney+\" -> com.disney.disneyplus")
        }

        val userLiveVideos = runCatching {
            File(context.filesDir, "live_wallpapers").listFiles()?.map { it.name }.orEmpty()
        }.getOrDefault(emptyList())

        val currentShape = prefs.getString("control_app_icon_shape", "Circle") ?: "Circle"
        val currentIdleMin = prefs.getInt("control_ambient_idle_minutes", 0)
        val currentLiveRes = prefs.getString("control_live_wallpaper_res", "720p") ?: "720p"
        val currentPerf = prefs.getBoolean("control_performance_mode", false)
        val currentSplash = prefs.getBoolean("splash_screen_enabled", true)
        val currentUsbBack = prefs.getBoolean("control_usb_boot_auto_back", false)

        return """
            ═══════════════════════════════════════════════════════════════════════
            RNA-TOOLS SYSTEM CONTROL & APP LAUNCHER (`rna-tools`)
            ═══════════════════════════════════════════════════════════════════════
            You have full control over RNA. TV settings, wallpapers, profiles, and installed apps using `${'$'}start [command] rna-tools;`:
            1. LAUNCH ANY APP ON TV:
               - `${'$'}start [package_name] rna-tools;` (e.g., `${'$'}start com.google.android.youtube.tv rna-tools;` or `${'$'}start com.rna.fileexplorer rna-tools;`)
            2. CHANGE WALLPAPER (STATIC OR LIVE) FOR CURRENT OR SPECIFIC PROFILE:
               - Static Wallpapers available: `wp_01.jpg` through `wp_43.jpg`
               - Live Video Wallpapers available: `live_wp_01_cyber_nebula.mp4`, `live_wp_02_aurora_waves.mp4`, `live_wp_03_neon_horizon.mp4`${if (userLiveVideos.isNotEmpty()) ", " + userLiveVideos.joinToString(", ") else ""}
               - For active profile: `${'$'}start wallpaper.[wallpaper_name] rna-tools;` (e.g. `${'$'}start wallpaper.wp_12.jpg rna-tools;` or `${'$'}start wallpaper.live_wp_01_cyber_nebula.mp4 rna-tools;`)
               - For a specific profile: `${'$'}start [profile_name].[wallpaper_name] rna-tools;` (e.g. `${'$'}start Main.wp_15.jpg rna-tools;`)
            3. SWITCH ACTIVE PROFILE:
               - Available Profiles: $profilesListStr
               - Command: `${'$'}start profile.[profile_name] rna-tools;` (e.g. `${'$'}start profile.Main rna-tools;`)
            4. CHANGE ANY CONTROL PANEL SETTING:
               - App Icon Shape (currently: $currentShape; 19 allowed shapes: `Rectangle`, `Leaf`, `Square`, `Squircle`, `Circle`, `Rounded Square`, `Hexagon`, `Octagon`, `Diamond`, `Pentagon`, `Shield`, `Teardrop`, `Pebble`, `Clover`, `Flower`, `Starburst`, `Capsule`, `Cut Corner`, `Scalloped Badge`):
                 `${'$'}start icon_shape.[shape_name] rna-tools;`
               - 4K Ambient Mode Auto-Idle Timer (currently: ${if (currentIdleMin == 0) "Off" else "${currentIdleMin}m"}; allowed: `0`, `1`, `3`, `5`, `10`, `15`, `20`, `25`, `30`):
                 `${'$'}start ambient_idle.[minutes] rna-tools;`
               - Live Wallpaper Resolution (currently: $currentLiveRes; allowed: `360p`, `480p`, `720p`, `1080p`):
                 `${'$'}start live_res.[360p|480p|720p|1080p] rna-tools;`
               - Performance Mode (currently: $currentPerf):
                 `${'$'}start performance_mode.[on|off] rna-tools;`
               - Splash Screen on Startup (currently: $currentSplash):
                 `${'$'}start splash_screen.[on|off] rna-tools;`
               - Auto-Back on USB Boot Detection (currently: $currentUsbBack):
                 `${'$'}start usb_boot_auto_back.[on|off] rna-tools;`
               - System Taskbar Overlay:
                 `${'$'}start taskbar.[on|off] rna-tools;`
               - RNA AI App Visibility on Home/Apps:
                 `${'$'}start rna_ai.[on|off] rna-tools;`
               - Open RNA File Explorer:
                 `${'$'}start com.rna.fileexplorer rna-tools;`

            ═══════════════════════════════════════════════════════════════════════
            TRIGGERS & MACROS AUTOMATION (`rna-tools`)
            ═══════════════════════════════════════════════════════════════════════
            You can create, modify, toggle, or clear TV automation triggers (Default triggers: `device_boot`, `screen_on`, OR any installed app using `app:[package_name]` or `app:[AppName]`):
            - Add an action to a trigger (auto-creates the app trigger if needed):
              `${'$'}start trigger.add.[target].[action_spec] rna-tools;`
              Targets: `device_boot`, `screen_on`, `app:com.netflix.mediaclient`, `app:Netflix`, `app:YouTube`, etc.
              Action Specs:
                * `volume:[0-100]` (sets exact TV volume %, e.g. `volume:21`)
                * `open_app:[package_or_name]` (opens another app)
                * `press:[back|home|ok|up|down|left|right|play_pause|mute|vol_up|vol_down|menu]`
                * `boot_warm` (open RNA TV on boot, then immediately return to previous non-launcher app when Open RNA TV at Boot is off)
                * `previous_app` (reopen the previously opened app if not Android TV launcher)
                * `rna_home` | `taskbar` | `recent_apps` | `voice_ai` | `agent` | `rna_ai` | `browser`
                * `delay:[ms]` (wait before next action, e.g. `delay:1500`)
              Examples:
                * "Set volume to 21% when Netflix opens" -> `${'$'}start trigger.add.app:com.netflix.mediaclient.volume:21 rna-tools;`
                * "When screen turns on, open YouTube" -> `${'$'}start trigger.add.screen_on.open_app:com.google.android.youtube.tv rna-tools;`
                * "When TV turns on, warm RNA TV and return to Live TV" -> `${'$'}start trigger.add.device_boot.boot_warm rna-tools;`
            - Remove an action (1-based index) or clear all actions on a trigger:
              `${'$'}start trigger.remove.[target].[1|2|all] rna-tools;`
            - Enable or disable a trigger:
              `${'$'}start trigger.toggle.[target].[on|off] rna-tools;`
            - Open RNA TV at boot toggle (off by default):
              `${'$'}start open_at_boot.[on|off] rna-tools;`

            CURRENT CONFIGURED TRIGGERS:
            ${RnaTriggersStore.buildPromptSummary(prefs)}

            ═══════════════════════════════════════════════════════════════════════
            APP-TOOLS INSTANT RNA APP CREATOR (`app-tools`)
            ═══════════════════════════════════════════════════════════════════════
            When the user asks you to create, build, or package an app (either a Website App or a custom HTML/CSS/JS TV App/Game), emit these commands at the end of your reply:
            1. Set the App Name:
               `${'$'}start app-name.[App Name] app-tools;`
            2. Generate a custom AI Icon for the App:
               `${'$'}start app-name.[App Name].img-gen app-tools;`
            3. Provide EITHER a Website URL OR full HTML5 code:
               - Website App: `${'$'}start app-url.[https://example.com] app-tools;`
               - Custom HTML5 App/Game: `${'$'}start app-html.{<!DOCTYPE html><html>...</html>} app-tools;`
            IMPORTANT: Never write raw HTML inside your conversational text — only place the HTML inside `${'$'}start app-html.{...} app-tools;` so the user sees a clean floating App Preview Card on screen!

            ALL INSTALLED APPS & PACKAGES ON THIS TV:
            $combinedAppsText
        """.trimIndent()
    }

    /**
     * Parses and executes any `$start ... rna-tools;` and `$start ... app-tools;` commands in [rawResponse],
     * and returns the cleaned display text with all raw command strings and `{html}` blocks removed.
     */
    fun processAndExecuteTools(
        context: Context,
        rawResponse: String
    ): RnaToolsExecutionReport {
        var workingText = rawResponse
        val summaries = mutableListOf<String>()
        var touchedAppTools = false

        // 1. Parse `$start app-html.{...} app-tools;` first because `{...}` can span multiple lines and contain semicolons!
        val htmlRegex = Regex(
            pattern = """\${'$'}start\s+app-html\.\{([\s\S]*?)\}\s+app-tools\s*;?""",
            option = RegexOption.IGNORE_CASE
        )
        htmlRegex.findAll(workingText).forEach { match ->
            val htmlContent = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (htmlContent.isNotEmpty()) {
                stagedAppHtml = htmlContent
                stagedAppUrl = ""
                touchedAppTools = true
                summaries.add("Generated HTML5 TV App code")
            }
        }
        workingText = workingText.replace(htmlRegex, "")

        // 2. Parse `$start [payload] app-tools;` (app-name.[name].img-gen, app-name.[name], app-url.[url])
        val appToolsRegex = Regex(
            pattern = """\${'$'}start\s+([^\n;]+?)\s+app-tools\s*;?""",
            option = RegexOption.IGNORE_CASE
        )
        appToolsRegex.findAll(workingText).forEach { match ->
            val directive = match.groupValues.getOrNull(1)?.trim().orEmpty()
            when {
                directive.startsWith("app-name.", ignoreCase = true) &&
                    directive.endsWith(".img-gen", ignoreCase = true) -> {
                    val extractedName = directive
                        .substringAfter("app-name.", "")
                        .replace(Regex("""\.img-gen$""", RegexOption.IGNORE_CASE), "")
                        .trim()
                        .removeSurrounding("[", "]")
                    if (extractedName.isNotBlank()) {
                        stagedAppName = extractedName
                    }
                    stagedAppLogoUri = generateStyledSvgLogoDataUri(stagedAppName)
                    touchedAppTools = true
                    summaries.add("Generated App Icon for \"$stagedAppName\"")
                }

                directive.startsWith("app-name.", ignoreCase = true) -> {
                    val extractedName = directive
                        .substringAfter("app-name.")
                        .trim()
                        .removeSurrounding("[", "]")
                    if (extractedName.isNotBlank()) {
                        stagedAppName = extractedName
                        if (stagedAppLogoUri.isBlank()) {
                            stagedAppLogoUri = generateStyledSvgLogoDataUri(stagedAppName)
                        }
                        touchedAppTools = true
                        summaries.add("Named App \"$stagedAppName\"")
                    }
                }

                directive.startsWith("app-url.", ignoreCase = true) -> {
                    val rawUrl = directive
                        .substringAfter("app-url.")
                        .trim()
                        .removeSurrounding("[", "]")
                    val normalizedUrl = when {
                        rawUrl.startsWith("http://", ignoreCase = true) ||
                            rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
                        rawUrl.isNotBlank() -> "https://$rawUrl"
                        else -> ""
                    }
                    if (normalizedUrl.isNotBlank()) {
                        stagedAppUrl = normalizedUrl
                        stagedAppHtml = ""
                        if (stagedAppLogoUri.isBlank()) {
                            stagedAppLogoUri = buildFaviconUrlForWebsite(normalizedUrl)
                                ?: generateStyledSvgLogoDataUri(stagedAppName)
                        }
                        touchedAppTools = true
                        summaries.add("Linked URL $normalizedUrl")
                    }
                }
            }
        }
        workingText = workingText.replace(appToolsRegex, "")

        var newlyCreatedApp: RnaApp? = null
        if (touchedAppTools && (stagedAppUrl.isNotBlank() || stagedAppHtml.isNotBlank())) {
            val finalTitle = stagedAppName.ifBlank { "RNA Custom App" }
            val slug = finalTitle.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "").ifBlank { "customapp" }
            val finalLogo = stagedAppLogoUri.ifBlank {
                if (stagedAppUrl.isNotBlank()) {
                    buildFaviconUrlForWebsite(stagedAppUrl) ?: generateStyledSvgLogoDataUri(finalTitle)
                } else {
                    generateStyledSvgLogoDataUri(finalTitle)
                }
            }
            val isHtml = stagedAppHtml.isNotBlank()
            val appObj = RnaApp(
                id = UUID.randomUUID().toString(),
                packageName = "com.rna.ai.$slug",
                title = finalTitle,
                version = "1.0.0",
                sourceType = if (isHtml) "HTML" else "URL",
                sourceUriOrContent = if (isHtml) stagedAppHtml else stagedAppUrl,
                iconBase64 = finalLogo,
                bannerBase64 = finalLogo,
                isTvEnabled = true,
                defaultInputMode = "DPAD",
                mouseSpeed = 1.2f,
                forceDarkMode = false,
                showLoadingBar = true,
                loadingBarColor = "#38BDF8"
            )
            newlyCreatedApp = appObj

            // Save to Room database & notify launcher
            ioScope.launch {
                runCatching {
                    RnaDatabase.getDatabase(context).rnaAppDao().insertOrUpdateApp(RnaAppEntity.fromDomain(appObj))
                }
            }
            onAppCreatedOrUpdatedCallback?.invoke(appObj)

            _hoverAppPreview.value = RnaAppHoverPreview(
                appId = appObj.id,
                appName = appObj.title,
                displaySubtitle = if (isHtml) "Interactive HTML5 TV App • Ready to Launch" else stagedAppUrl,
                logoUri = finalLogo,
                createdApp = appObj
            )

            // Reset staged URL/HTML so subsequent app creations start fresh
            stagedAppUrl = ""
            stagedAppHtml = ""
        }

        // 3. Parse `$start [command] rna-tools;`
        val rnaToolsRegex = Regex(
            pattern = """\${'$'}start\s+([^\n;]+?)\s+rna-tools\s*;?""",
            option = RegexOption.IGNORE_CASE
        )
        rnaToolsRegex.findAll(workingText).forEach { match ->
            val rawCmd = match.groupValues.getOrNull(1)?.trim()?.removeSurrounding("[", "]").orEmpty()
            if (rawCmd.isNotEmpty()) {
                val summary = executeSingleRnaToolCommand(context, rawCmd)
                if (!summary.isNullOrBlank()) {
                    summaries.add(summary)
                }
            }
        }
        workingText = workingText.replace(rnaToolsRegex, "")

        val cleaned = workingText
            .replace(Regex("""\s+"""), " ")
            .trim()

        return RnaToolsExecutionReport(
            cleanedText = cleaned,
            executedSummaries = summaries,
            createdOrUpdatedApp = newlyCreatedApp
        )
    }

    private fun executeSingleRnaToolCommand(context: Context, cmd: String): String? {
        val prefs = context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val lower = cmd.lowercase(Locale.US).trim()

        // 1. Wallpaper change: `wallpaper.wp_05.jpg` or `wallpaper.live_wp_01_cyber_nebula.mp4` or direct `wp_05.jpg` / `live_wp_...`
        if (lower.startsWith("wallpaper.")) {
            val wpName = cmd.substringAfter(".").trim()
            onApplyWallpaperCallback?.invoke(wpName, null)
            persistWallpaperSelectionDirectly(context, prefs, wpName, null)
            return "Wallpaper set to $wpName"
        }
        if (lower.matches(Regex("""wp_\d+\.(jpg|png|jpeg)""")) ||
            lower.startsWith("live_wp_") ||
            lower.endsWith(".mp4")
        ) {
            onApplyWallpaperCallback?.invoke(cmd, null)
            persistWallpaperSelectionDirectly(context, prefs, cmd, null)
            return "Wallpaper set to $cmd"
        }

        // 2. Profile switch: `profile.Main`
        if (lower.startsWith("profile.")) {
            val rest = cmd.substringAfter(".")
            if (rest.contains(".wallpaper.")) {
                val profName = rest.substringBefore(".wallpaper.").trim()
                val wpName = rest.substringAfter(".wallpaper.").trim()
                onApplyWallpaperCallback?.invoke(wpName, profName)
                persistWallpaperSelectionDirectly(context, prefs, wpName, profName)
                return "Wallpaper for $profName set to $wpName"
            } else {
                val profName = rest.trim()
                onSwitchProfileByNameCallback?.invoke(profName)
                return "Switched profile to $profName"
            }
        }

        // Check `[profile_name].[wp_XX.jpg or live_wp_...]`
        if (lower.contains(".wp_") || lower.contains(".live_wp_") || lower.endsWith(".mp4") || lower.endsWith(".jpg")) {
            val profName = cmd.substringBefore(".").trim()
            val wpName = cmd.substringAfter(".").trim()
            if (profName.isNotEmpty() && wpName.isNotEmpty()) {
                onApplyWallpaperCallback?.invoke(wpName, profName)
                persistWallpaperSelectionDirectly(context, prefs, wpName, profName)
                return "Wallpaper for $profName set to $wpName"
            }
        }

        // 2.5 Triggers & Macros Automation commands:
        //    `trigger.add.[target].[action_spec]`
        //    `trigger.remove.[target].[index|all]`
        //    `trigger.clear.[target]`
        //    `trigger.toggle.[target].[on|off]`
        if (lower.startsWith("trigger.")) {
            return executeTriggerRnaToolCommand(context, prefs, cmd)
        }

        // 3. Control Panel settings:
        //    icon_shape.[shape], ambient_idle.[min], live_res.[res], performance_mode.[on|off],
        //    splash_screen.[on|off], usb_boot_auto_back.[on|off], taskbar.[on|off], rna_ai.[on|off], open_at_boot.[on|off]
        val settingPrefixes = listOf(
            "setting.",
            "icon_shape.",
            "app_icon_shape.",
            "ambient_idle.",
            "live_res.",
            "live_wallpaper_res.",
            "performance_mode.",
            "perf_mode.",
            "splash_screen.",
            "splash.",
            "usb_boot_auto_back.",
            "open_at_boot.",
            "taskbar.",
            "rna_ai."
        )
        if (settingPrefixes.any { lower.startsWith(it) }) {
            val normalizedCmd = if (lower.startsWith("setting.")) cmd.substringAfter(".") else cmd
            val key = normalizedCmd.substringBefore(".").lowercase(Locale.US).trim()
            val value = normalizedCmd.substringAfter(".").trim()
            applySettingToPrefs(prefs, key, value)
            onApplyControlPanelSettingCallback?.invoke(key, value)
            return "Updated setting $key to $value"
        }

        // 4. Launch File Explorer or any installed app by package / name
        val targetPkg = cmd.removePrefix("open.").removePrefix("app.").trim()
        if (targetPkg.equals("file_explorer", ignoreCase = true) ||
            targetPkg.equals("com.rna.fileexplorer", ignoreCase = true) ||
            targetPkg.equals("rna file explorer", ignoreCase = true)
        ) {
            runCatching {
                context.startActivity(
                    Intent(context, RnaFileExplorerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            return "Opened RNA File Explorer"
        }

        if (onLaunchAppCallback != null) {
            onLaunchAppCallback?.invoke(targetPkg)
            return "Launched $targetPkg"
        } else {
            val pm = context.packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(targetPkg)
                ?: pm.getLaunchIntentForPackage(targetPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
                return "Launched $targetPkg"
            }
        }
        return null
    }

    private fun persistWallpaperSelectionDirectly(
        context: Context,
        prefs: SharedPreferences,
        wallpaperName: String,
        profileName: String?
    ) {
        val isLive = wallpaperName.endsWith(".mp4", ignoreCase = true) || wallpaperName.startsWith("live_", ignoreCase = true)
        val normalizedAsset = when {
            isLive && !wallpaperName.contains("/") -> "asset:///live_wallpapers/$wallpaperName"
            else -> wallpaperName
        }
        val profiles = loadTvProfiles(prefs)
        val targetProfile = if (!profileName.isNullOrBlank()) {
            profiles.firstOrNull { it.name.equals(profileName, ignoreCase = true) }
        } else {
            val activeId = getActiveTvProfileId(prefs)
            profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        }
        if (targetProfile != null) {
            val updated = profiles.map { p ->
                if (p.id == targetProfile.id) p.copy(wallpaperUri = normalizedAsset) else p
            }
            saveTvProfiles(prefs, updated)
        }
        if (isLive) {
            prefs.edit()
                .putString("active_live_wallpaper_uri", normalizedAsset)
                .putString("wallpaper_mode_type", "LIVE")
                .apply()
        } else {
            prefs.edit()
                .putString("selected_tv_wallpaper", normalizedAsset)
                .putString("wallpaper_mode_type", "STATIC")
                .apply()
        }
    }

    private fun applySettingToPrefs(prefs: SharedPreferences, key: String, value: String) {
        val boolVal = value.lowercase(Locale.US) in setOf("true", "on", "1", "yes", "enabled", "enable")
        val editor = prefs.edit()
        when (key) {
            "icon_shape", "app_icon_shape" -> editor.putString("control_app_icon_shape", value)
            "ambient_idle", "ambient_idle_minutes" -> {
                val mins = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                editor.putInt("control_ambient_idle_minutes", mins)
            }
            "live_res", "live_wallpaper_res" -> {
                val norm = if (value.endsWith("p", ignoreCase = true)) value.lowercase(Locale.US) else "${value}p"
                editor.putString("control_live_wallpaper_res", norm)
            }
            "performance_mode", "perf_mode" -> {
                TvPerformanceMode.isEnabled = boolVal
                editor.putBoolean("control_performance_mode", boolVal)
            }
            "splash_screen", "splash" -> editor.putBoolean("splash_screen_enabled", boolVal)
            "usb_boot_auto_back" -> editor.putBoolean("control_usb_boot_auto_back", boolVal)
            "open_at_boot" -> editor.putBoolean(KEY_OPEN_RNA_AT_BOOT, boolVal)
            "taskbar", "taskbar_enabled" -> editor.putBoolean("taskbar_enabled", boolVal)
            "rna_ai", "rna_ai_enabled" -> editor.putBoolean(KEY_RNAAI_ENABLED_IN_CONTROL_PANEL, boolVal)
        }
        editor.apply()
    }

    private fun resolveInstalledAppPackageAndLabel(context: Context, query: String): Pair<String, String> {
        val clean = query.trim().removeSurrounding("\"").removeSurrounding("'")
        val lower = clean.lowercase(Locale.US)
        val knownMap = mapOf(
            "netflix" to ("com.netflix.mediaclient" to "Netflix"),
            "youtube" to ("com.google.android.youtube.tv" to "YouTube"),
            "prime video" to ("com.amazon.amazonvideo.livingroom" to "Prime Video"),
            "prime" to ("com.amazon.amazonvideo.livingroom" to "Prime Video"),
            "disney+" to ("com.disney.disneyplus" to "Disney+"),
            "disney" to ("com.disney.disneyplus" to "Disney+"),
            "spotify" to ("com.spotify.tv.android" to "Spotify"),
            "twitch" to ("tv.twitch.android.viewer" to "Twitch"),
            "hulu" to ("com.hulu.livingroomplus" to "Hulu"),
            "plex" to ("com.plexapp.android" to "Plex")
        )
        val pm = context.packageManager
        val installed = runCatching {
            val lIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) }
            val sIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            (pm.queryIntentActivities(lIntent, 0) + pm.queryIntentActivities(sIntent, 0)).mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val lbl = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                pkg to lbl
            }.distinctBy { it.first }
        }.getOrDefault(emptyList())

        installed.firstOrNull { it.first.equals(clean, ignoreCase = true) }?.let { return it }
        installed.firstOrNull { it.second.equals(clean, ignoreCase = true) }?.let { return it }
        installed.firstOrNull { it.second.lowercase(Locale.US).contains(lower) || it.first.lowercase(Locale.US).contains(lower) }?.let { return it }
        knownMap[lower]?.let { return it }
        return clean to clean.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }
    }

    private fun executeTriggerRnaToolCommand(
        context: Context,
        prefs: SharedPreferences,
        rawCmd: String
    ): String? {
        val body = rawCmd.removePrefix("trigger.").removePrefix("TRIGGER.").trim()
        val verb = body.substringBefore(".").lowercase(Locale.US).trim()
        val afterVerb = body.substringAfter(".", "").trim()
        if (afterVerb.isBlank()) return null

        // Split target and argument cleanly (handling dotted package names like `app:com.netflix.mediaclient.volume:21`)
        val (targetToken, argToken) = splitTriggerTargetAndArg(afterVerb, verb == "clear")

        val rules = RnaTriggersStore.loadRules(prefs)
        val targetLower = targetToken.lowercase(Locale.US)
        val resolvedRule: RnaTriggerRule = when {
            targetLower == "device_boot" || targetLower == "boot" || targetLower == "power_on" -> {
                rules.firstOrNull { it.type == RnaTriggerType.DEVICE_BOOT }
                    ?: RnaTriggerRule(
                        id = RnaTriggersStore.DEFAULT_BOOT_ID,
                        type = RnaTriggerType.DEVICE_BOOT,
                        title = "When Device Turns On",
                        enabled = true,
                        isBuiltIn = true
                    )
            }
            targetLower == "screen_on" || targetLower == "wake" -> {
                rules.firstOrNull { it.type == RnaTriggerType.SCREEN_ON }
                    ?: RnaTriggerRule(
                        id = RnaTriggersStore.DEFAULT_SCREEN_ON_ID,
                        type = RnaTriggerType.SCREEN_ON,
                        title = "When Screen Turns On",
                        enabled = true,
                        isBuiltIn = true
                    )
            }
            else -> {
                val appQuery = targetToken.removePrefix("app:").removePrefix("APP:").trim()
                val (pkg, lbl) = resolveInstalledAppPackageAndLabel(context, appQuery)
                RnaTriggersStore.findOrCreateAppTrigger(prefs, pkg, lbl).second
            }
        }

        return when (verb) {
            "add" -> {
                val parsedAction = parseTriggerActionSpec(context, argToken) ?: return null
                val updated = resolvedRule.copy(
                    enabled = true,
                    actions = resolvedRule.actions + parsedAction
                )
                RnaTriggersStore.upsertRule(prefs, updated)
                "Added '${parsedAction.displayTitle()}' to trigger '${updated.title}'"
            }
            "remove" -> {
                val argLower = argToken.lowercase(Locale.US)
                val updated = if (argLower == "all" || argLower.isBlank()) {
                    resolvedRule.copy(actions = emptyList())
                } else {
                    val idx1 = argLower.filter { it.isDigit() }.toIntOrNull() ?: 1
                    val idx0 = (idx1 - 1).coerceIn(0, (resolvedRule.actions.size - 1).coerceAtLeast(0))
                    val mutable = resolvedRule.actions.toMutableList()
                    if (idx0 in mutable.indices) mutable.removeAt(idx0)
                    resolvedRule.copy(actions = mutable)
                }
                RnaTriggersStore.upsertRule(prefs, updated)
                "Updated actions for trigger '${updated.title}'"
            }
            "clear" -> {
                val updated = resolvedRule.copy(actions = emptyList())
                RnaTriggersStore.upsertRule(prefs, updated)
                "Cleared all actions on '${updated.title}'"
            }
            "toggle" -> {
                val on = argToken.lowercase(Locale.US) in setOf("on", "true", "enable", "enabled", "1")
                val updated = resolvedRule.copy(enabled = on)
                RnaTriggersStore.upsertRule(prefs, updated)
                "${if (on) "Enabled" else "Paused"} trigger '${updated.title}'"
            }
            else -> null
        }
    }

    private fun splitTriggerTargetAndArg(input: String, noArgExpected: Boolean): Pair<String, String> {
        if (noArgExpected) return input to ""
        val knownActionPrefixes = listOf(
            ".volume:", ".open_app:", ".press:", ".delay:",
            ".boot_warm", ".warm_bg", ".previous_app", ".rna_home", ".taskbar", ".recent_apps",
            ".voice_ai", ".agent", ".rna_ai", ".browser",
            ".all", ".on", ".off", ".true", ".false"
        )
        val lower = input.lowercase(Locale.US)
        for (marker in knownActionPrefixes) {
            val idx = lower.lastIndexOf(marker)
            if (idx > 0) {
                return input.substring(0, idx).trim() to input.substring(idx + 1).trim()
            }
        }
        val lastDot = input.lastIndexOf('.')
        return if (lastDot > 0) {
            input.substring(0, lastDot).trim() to input.substring(lastDot + 1).trim()
        } else {
            input to ""
        }
    }

    private fun parseTriggerActionSpec(context: Context, spec: String): RnaTriggerActionItem? {
        val clean = spec.trim()
        val lower = clean.lowercase(Locale.US)
        return when {
            lower.startsWith("volume:") -> {
                val pct = clean.substringAfter(":").filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 100) ?: return null
                RnaTriggerActionItem(type = RnaTriggerActionType.SET_VOLUME_PERCENT, volumePercent = pct)
            }
            lower.startsWith("open_app:") -> {
                val query = clean.substringAfter(":").trim()
                val (pkg, lbl) = resolveInstalledAppPackageAndLabel(context, query)
                RnaTriggerActionItem(
                    type = RnaTriggerActionType.OPEN_APP,
                    targetPackage = pkg,
                    targetLabel = lbl
                )
            }
            lower.startsWith("press:") -> {
                val keyName = lower.substringAfter(":").trim()
                val keyCode = when (keyName) {
                    "back" -> android.view.KeyEvent.KEYCODE_BACK
                    "home" -> android.view.KeyEvent.KEYCODE_HOME
                    "ok", "center", "select", "enter" -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
                    "up" -> android.view.KeyEvent.KEYCODE_DPAD_UP
                    "down" -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
                    "left" -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
                    "right" -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                    "play_pause", "play", "pause" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    "mute" -> android.view.KeyEvent.KEYCODE_VOLUME_MUTE
                    "vol_up", "volume_up" -> android.view.KeyEvent.KEYCODE_VOLUME_UP
                    "vol_down", "volume_down" -> android.view.KeyEvent.KEYCODE_VOLUME_DOWN
                    "menu" -> android.view.KeyEvent.KEYCODE_MENU
                    else -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
                }
                RnaTriggerActionItem(type = RnaTriggerActionType.PRESS_BUTTON, keyCode = keyCode)
            }
            lower.startsWith("delay:") -> {
                val ms = clean.substringAfter(":").filter { it.isDigit() }.toLongOrNull()?.coerceIn(100L, 30_000L) ?: 1000L
                RnaTriggerActionItem(type = RnaTriggerActionType.DELAY_MS, delayMs = ms)
            }
            lower == "boot_warm" || lower == "warm_bg" || lower == "warm_background" -> RnaTriggerActionItem(type = RnaTriggerActionType.BOOT_WARM_RNA_TV)
            lower == "previous_app" || lower == "open_previous_app" -> RnaTriggerActionItem(type = RnaTriggerActionType.OPEN_PREVIOUS_APP)
            lower == "rna_home" -> RnaTriggerActionItem(type = RnaTriggerActionType.RNA_HOME)
            lower == "taskbar" -> RnaTriggerActionItem(type = RnaTriggerActionType.QUICK_APPS_TASKBAR)
            lower == "recent_apps" -> RnaTriggerActionItem(type = RnaTriggerActionType.RECENT_APPS_DRAWER)
            lower == "voice_ai" -> RnaTriggerActionItem(type = RnaTriggerActionType.LIVE_VOICE_AI)
            lower == "agent" -> RnaTriggerActionItem(type = RnaTriggerActionType.AUTONOMOUS_AGENT)
            lower == "rna_ai" -> RnaTriggerActionItem(type = RnaTriggerActionType.RNA_AI_APP)
            lower == "browser" -> RnaTriggerActionItem(type = RnaTriggerActionType.RNA_BROWSER)
            else -> null
        }
    }
}

/**
 * Floating Hover Card displayed on top of both the Normal AI Assistant and Autonomous Agent
 * whenever an RNA App is created via `app-tools`. Shows the App Logo, App Name, and URL / HTML5 badge
 * (never dumping raw HTML code), plus a "Launch App" button.
 */
@Composable
fun RnaAppCreationHoverCardOverlay(
    context: Context,
    modifier: Modifier = Modifier
) {
    val preview by RnaAiToolsEngine.hoverAppPreview.collectAsState()
    AnimatedVisibility(
        visible = preview != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        val item = preview ?: return@AnimatedVisibility
        var launchFocused by remember { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xF5111625),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.horizontalGradient(listOf(CyanPrimary, EmeraldSuccess, VioletSecondary))
            ),
            shadowElevation = 24.dp,
            modifier = Modifier
                .widthIn(min = 340.dp, max = 520.dp)
                .padding(16.dp)
                .testTag("rna_ai_created_app_hover_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier.size(56.dp)
                ) {
                    AsyncImage(
                        model = item.logoUri,
                        contentDescription = item.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "RNA. APP CREATED",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.appName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.displaySubtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = {
                        val app = item.createdApp
                        val intent = Intent(context, WebContainerActivity::class.java).apply {
                            putExtra(WebContainerActivity.EXTRA_APP_ID, app.id)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                Toast.makeText(context, "App saved to Home & Apps!", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (launchFocused) Color.White else CyanPrimary,
                        contentColor = Color(0xFF0A0E17)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .onFocusChanged { launchFocused = it.isFocused }
                        .focusable()
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                IconButton(
                    onClick = { RnaAiToolsEngine.dismissHoverPreview() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Preview",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
