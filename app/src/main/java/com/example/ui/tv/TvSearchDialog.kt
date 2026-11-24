package com.example.ui.tv

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.wizard.decodeDataUriToImageBitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "TvSearchDialog"

data class TvSearchWebResult(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String
)

sealed interface AiSearchUiState {
    data object Idle : AiSearchUiState
    data object Loading : AiSearchUiState
    data class Success(
        val headline: String,
        val summary: String,
        val keyPoints: List<String>,
        val followUpQueries: List<String>,
        val results: List<TvSearchWebResult>
    ) : AiSearchUiState
    data class Error(val message: String) : AiSearchUiState
}

private object TvSearchAiEngine {
    suspend fun generateSearchJson(context: Context, prompt: String): String = withContext(Dispatchers.IO) {
        val outcome = RnaFreeAiRouter.generateTextOrVision(
            context = context,
            prompt = prompt,
            temperature = 0.25f
        )
        outcome.text
    }
}

/**
 * Full-screen RNA. TV Local + Files + AI & Web Search Utility.
 * Strictly orders results as:
 * 1. APPS (Installed TV apps, Mobile apps, and RNA. Web Apps)
 * 2. FILES (Local .rna packages, .html web files, .zip bundles on TV storage)
 * 3. GEMINI AI SMART ANSWER & WEB RESULTS (Live Web Search + Gemini AI Overview -> opens RNA. TV Browser with Top Bar & Mouse)
 */
@Composable
fun TvSearchUtilityDialog(
    allApps: List<UnifiedLauncherApp>,
    onLaunchApp: (UnifiedLauncherApp) -> Unit,
    onInstallRnaFile: (Uri) -> Unit,
    onOpenWebUrlInBrowser: (url: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFieldFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var allDeviceFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var liveWebResults by remember { mutableStateOf<List<TvSearchWebResult>>(emptyList()) }
    var isFetchingWeb by remember { mutableStateOf(false) }
    var aiState by remember { mutableStateOf<AiSearchUiState>(AiSearchUiState.Idle) }
    var aiJob by remember { mutableStateOf<Job?>(null) }
    var lastAiQuery by remember { mutableStateOf("") }

    fun runGeminiSmartSearch(searchQuery: String) {
        val q = searchQuery.trim()
        if (q.isEmpty()) return
        lastAiQuery = q
        aiJob?.cancel()
        aiJob = scope.launch {
            aiState = AiSearchUiState.Loading
            try {
                val jsonText = withTimeout(45_000L) {
                    TvSearchAiEngine.generateSearchJson(
                        context = context,
                        prompt = """
                        The user on RNA. Android TV searched for: "$q".
                        Return strictly valid JSON with these exact keys:
                        {
                          "headline": "Short direct title or answer headline for $q",
                          "aiSummary": "Clear, factual 2-3 sentence overview answering $q",
                          "keyPoints": ["Key point 1", "Key point 2", "Key point 3"],
                          "followUpQueries": ["Follow-up query 1", "Follow-up query 2", "Follow-up query 3"],
                          "webResults": [
                            {
                              "title": "Page title",
                              "snippet": "1-sentence description",
                              "url": "https://...",
                              "source": "Domain or site name"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
                }
                val cleanedJson = jsonText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(cleanedJson)
                val headline = json.optString("headline", "AI Overview for \"$q\"")
                val summary = json.optString("aiSummary", "")
                val pointsArr = json.optJSONArray("keyPoints") ?: JSONArray()
                val keyPoints = buildList {
                    for (i in 0 until pointsArr.length()) {
                        val pt = pointsArr.optString(i).trim()
                        if (pt.isNotEmpty()) add(pt)
                    }
                }
                val followArr = json.optJSONArray("followUpQueries") ?: JSONArray()
                val followUps = buildList {
                    for (i in 0 until followArr.length()) {
                        val fq = followArr.optString(i).trim()
                        if (fq.isNotEmpty()) add(fq)
                    }
                }
                val arr = json.optJSONArray("webResults") ?: JSONArray()
                val parsed = mutableListOf<TvSearchWebResult>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsed.add(
                        TvSearchWebResult(
                            title = obj.optString("title", q),
                            snippet = obj.optString("snippet", ""),
                            url = obj.optString("url", "https://www.google.com/search?q=${Uri.encode(q)}"),
                            source = obj.optString("source", "Web")
                        )
                    )
                }
                aiState = AiSearchUiState.Success(
                    headline = headline,
                    summary = summary,
                    keyPoints = keyPoints,
                    followUpQueries = followUps,
                    results = parsed
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini smart search timed out", e)
                aiState = AiSearchUiState.Error("AI search timed out. Standard web & Wikipedia results are ready below.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini smart search failed", e)
                aiState = AiSearchUiState.Error(e.localizedMessage ?: "AI overview unavailable right now.")
            }
        }
    }

    var hasStoragePerm by remember { mutableStateOf(hasFullStorageAccess(context)) }
    var showSearchKeyboard by remember { mutableStateOf(false) }

    // Scan all real TV files across external storage & app storage
    LaunchedEffect(hasStoragePerm) {
        withContext(Dispatchers.IO) {
            val tvFilesDir = File(context.filesDir, "tv_search_files").apply { mkdirs() }
            val sampleHtml = File(tvFilesDir, "rna_tv_welcome_portal.html")
            if (!sampleHtml.exists()) {
                sampleHtml.writeText(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8"/>
                        <title>RNA. TV Local Web Portal</title>
                        <style>
                            body { background:#11131A; color:#E3E1EC; font-family:sans-serif; padding:48px; }
                            .card { background:#1E202B; border-radius:20px; padding:28px; max-width:720px; border:1px solid #454653; }
                            h1 { color:#BAC3FF; margin-top:0; }
                            a { color:#A6F2D1; font-weight:bold; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <h1>RNA. TV Local HTML Document</h1>
                            <p>Opened directly from local TV storage inside the RNA. TV Browser with Top Bar and Virtual Mouse.</p>
                            <p><a href="https://www.wikipedia.org">Visit Wikipedia</a> • <a href="https://news.ycombinator.com">Hacker News</a></p>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()
                )
            }

            listOf("astro_dodge.rna", "neocalc_express.rna", "rna_web_playground.rna").forEach { legacyFile ->
                val f = File(tvFilesDir, legacyFile)
                if (f.exists()) f.delete()
            }

            val scanned = scanFilesRecursive(
                context = context,
                filterPredicate = { true },
                maxResults = 500
            )
            allDeviceFiles = scanned.distinctBy { it.absolutePath }
        }
    }

    // Filter 1: APPS (First in order!)
    val matchingApps = remember(query, allApps) {
        val q = query.trim()
        if (q.isEmpty()) {
            allApps.take(8)
        } else {
            allApps.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    // Filter 2: FILES (Second in order! Matches all real files on the TV)
    val matchingFiles = remember(query, allDeviceFiles) {
        val q = query.trim()
        if (q.isEmpty()) {
            allDeviceFiles.take(12)
        } else {
            allDeviceFiles.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.extension.equals(q.removePrefix("."), ignoreCase = true)
            }.take(30)
        }
    }

    // Filter 3: Live Web Results + Auto-Debounced Gemini AI Overview
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            liveWebResults = listOf(
                TvSearchWebResult(
                    title = "Google Search",
                    snippet = "Search the entire web with RNA. TV Browser, Top Bar & Virtual Mouse",
                    url = "https://www.google.com",
                    source = "google.com"
                ),
                TvSearchWebResult(
                    title = "YouTube TV & Videos",
                    snippet = "Watch 4K videos, live streams, and channels in RNA. Browser",
                    url = "https://www.youtube.com",
                    source = "youtube.com"
                ),
                TvSearchWebResult(
                    title = "Wikipedia — The Free Encyclopedia",
                    snippet = "Explore millions of articles with D-Pad spatial focus or Virtual Mouse",
                    url = "https://www.wikipedia.org",
                    source = "wikipedia.org"
                ),
                TvSearchWebResult(
                    title = "Poki Web Games",
                    snippet = "Play instant HTML5 web games on RNA. TV",
                    url = "https://poki.com",
                    source = "poki.com"
                )
            )
            aiJob?.cancel()
            aiState = AiSearchUiState.Idle
            return@LaunchedEffect
        }

        delay(260L)
        isFetchingWeb = true
        val encoded = Uri.encode(trimmed)
        val baseResults = mutableListOf(
            TvSearchWebResult(
                title = "Search Google for \"$trimmed\"",
                snippet = "Open full Google Web Search results for \"$trimmed\" in RNA. TV Browser",
                url = "https://www.google.com/search?q=$encoded",
                source = "Google Web"
            ),
            TvSearchWebResult(
                title = "Search YouTube for \"$trimmed\"",
                snippet = "Watch videos matching \"$trimmed\" in RNA. TV Browser",
                url = "https://www.youtube.com/results?search_query=$encoded",
                source = "YouTube"
            ),
            TvSearchWebResult(
                title = "Search DuckDuckGo for \"$trimmed\"",
                snippet = "Privacy-first web results for \"$trimmed\" in RNA. TV Browser",
                url = "https://duckduckgo.com/?q=$encoded",
                source = "DuckDuckGo"
            )
        )

        withContext(Dispatchers.IO) {
            try {
                val wikiUrl = URL("https://en.wikipedia.org/w/api.php?action=opensearch&search=$encoded&limit=4&namespace=0&format=json")
                val conn = (wikiUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    setRequestProperty("User-Agent", "RNA-TV-Launcher/2.0")
                }
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val arr = JSONArray(body)
                    val titles = arr.optJSONArray(1)
                    val descriptions = arr.optJSONArray(2)
                    val urls = arr.optJSONArray(3)
                    if (titles != null && urls != null) {
                        for (i in 0 until titles.length()) {
                            val t = titles.optString(i)
                            val d = descriptions?.optString(i)?.ifBlank { "Wikipedia encyclopedia article for $t" } ?: "Wikipedia article"
                            val u = urls.optString(i)
                            if (t.isNotBlank() && u.isNotBlank()) {
                                baseResults.add(
                                    TvSearchWebResult(
                                        title = t,
                                        snippet = d,
                                        url = u,
                                        source = "Wikipedia"
                                    )
                                )
                            }
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
            }
        }
        liveWebResults = baseResults
        isFetchingWeb = false

        // Automatically trigger AI Smart Answer after user pauses typing (3+ chars)
        if (trimmed.length >= 3 && !trimmed.equals(lastAiQuery, ignoreCase = true)) {
            delay(500L)
            runGeminiSmartSearch(trimmed)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EnableSystemKeyboardInDialog()
        val dialogView = LocalView.current
        val dialogKeyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            delay(80L)
            try {
                searchFieldFocusRequester.requestFocus()
            } catch (_: Exception) {}
            if (!isRnaKeyboardDefaultIme(context)) {
                openDeviceDefaultKeyboard(
                    context = context,
                    view = dialogView,
                    focusRequester = searchFieldFocusRequester,
                    keyboardController = dialogKeyboardController
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xF511131A), Color(0xFA161822))
                        )
                    )
                    .testTag("tv_search_utility_dialog"),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 42.dp, vertical = 26.dp)
                ) {
                    // Top Search Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF222534))
                                .testTag("tv_search_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    "Ask AI anything, or search TV Apps, Files & Web...",
                                    color = Color(0xFF8F909E)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = CyanPrimary
                                )
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = {
                                        query = ""
                                        lastAiQuery = ""
                                        aiState = AiSearchUiState.Idle
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color(0xFFC6C5D4)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E202B),
                                unfocusedContainerColor = Color(0xFF191B24),
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFieldFocusRequester)
                                .tvKeyboardFieldTrigger(
                                    context = context,
                                    view = dialogView,
                                    focusRequester = searchFieldFocusRequester,
                                    keyboardController = dialogKeyboardController,
                                    onOpenRnaKeyboard = { showSearchKeyboard = true }
                                )
                                .testTag("tv_search_input_field")
                        )

                        Button(
                            onClick = {
                                if (isRnaKeyboardDefaultIme(context)) {
                                    showSearchKeyboard = true
                                } else {
                                    openDeviceDefaultKeyboard(
                                        context = context,
                                        view = dialogView,
                                        focusRequester = searchFieldFocusRequester,
                                        keyboardController = dialogKeyboardController
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222534),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("tv_search_keyboard_button")
                        ) {
                            Text("Keyboard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val targetQuery = query.ifBlank { "Best free streaming & gaming websites for Android TV" }
                                if (query.isBlank()) query = targetQuery
                                runGeminiSmartSearch(targetQuery)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF303F9F),
                                contentColor = Color(0xFFDEE0FF)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("tv_search_ai_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask AI", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val target = if (query.isBlank()) {
                                    "https://www.google.com"
                                } else if (query.contains(".") && !query.contains(" ")) {
                                    if (query.startsWith("http")) query else "https://$query"
                                } else {
                                    "https://www.google.com/search?q=${Uri.encode(query)}"
                                }
                                onOpenWebUrlInBrowser(target, query.ifBlank { "Google Search" })
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color(0xFF172778)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("tv_search_open_browser_button")
                        ) {
                            Icon(imageVector = Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Browser", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Suggestion & AI Prompt Chips (D-Pad Focusable)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf(
                            "Top 4K Movies to Watch",
                            "Best HTML5 TV Games",
                            "YouTube",
                            "Wikipedia",
                            "Latest Tech News",
                            ".mp4",
                            ".apk",
                            ".rna",
                            "GitHub"
                        )
                        items(suggestions) { chip ->
                            var chipFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    chipFocused -> Color.White
                                    query.equals(chip, ignoreCase = true) -> CyanPrimary
                                    else -> Color(0xFF222534)
                                },
                                border = BorderStroke(
                                    width = if (chipFocused) 2.dp else 1.dp,
                                    color = if (chipFocused) CyanPrimary else Color(0x28FFFFFF)
                                ),
                                modifier = Modifier
                                    .onFocusChanged { chipFocused = it.isFocused }
                                    .clickable {
                                        query = chip
                                        if (!chip.startsWith(".")) {
                                            runGeminiSmartSearch(chip)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (chipFocused || query.equals(chip, ignoreCase = true)) Color(0xFF172778) else CyanPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = chip,
                                        color = if (chipFocused || query.equals(chip, ignoreCase = true)) Color(0xFF11131A) else Color(0xFFE3E1EC),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Results List strictly ordered: 1. APPS -> 2. FILES -> 3. AI SMART ANSWER & WEB RESULTS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("tv_search_results_list"),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // ── 1. APPS SECTION (FIRST) ──
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "1. APPS (${matchingApps.size})",
                                        color = CyanPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                if (matchingApps.isEmpty()) {
                                    Text(
                                        text = "No installed TV or RNA. apps matching \"$query\"",
                                        color = Color(0xFF8F909E),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.testTag("tv_search_apps_row")
                                    ) {
                                        items(matchingApps, key = { it.uniqueKey }) { app ->
                                            SearchAppResultCard(
                                                app = app,
                                                onClick = {
                                                    onDismiss()
                                                    onLaunchApp(app)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 2. FILES SECTION (SECOND) ──
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "2. FILES ON TV (${matchingFiles.size})",
                                            color = EmeraldSuccess,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    if (!hasStoragePerm) {
                                        Button(
                                            onClick = {
                                                launchAllFilesAccessSettings(context)
                                                hasStoragePerm = hasFullStorageAccess(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AmberAccent,
                                                contentColor = Color(0xFF11131A)
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Text("Grant Storage Access", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (matchingFiles.isEmpty()) {
                                    Text(
                                        text = "No files on storage matching \"$query\"",
                                        color = Color(0xFF8F909E),
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        matchingFiles.forEach { file ->
                                            SearchFileResultRow(
                                                file = file,
                                                onClick = {
                                                    if (file.name.endsWith(".rna", ignoreCase = true)) {
                                                        onDismiss()
                                                        onInstallRnaFile(Uri.fromFile(file))
                                                    } else {
                                                        onDismiss()
                                                        try {
                                                            val intent = android.content.Intent(
                                                                context,
                                                                com.example.RnaFileExplorerActivity::class.java
                                                            ).apply {
                                                                putExtra(
                                                                    com.example.RnaFileExplorerActivity.EXTRA_INITIAL_PATH,
                                                                    file.absolutePath
                                                                )
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (_: Exception) {
                                                            onOpenWebUrlInBrowser("file://${file.absolutePath}", file.name)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    // ── 3. GEMINI AI OVERVIEW & WEB RESULTS SECTION (THIRD) ──
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = VioletSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "3. AI ASSISTANT & WEB RESULTS",
                                    color = VioletSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                if (isFetchingWeb) {
                                    CircularProgressIndicator(
                                        color = CyanPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Gemini AI Smart Overview Card
                            when (val state = aiState) {
                                is AiSearchUiState.Loading -> {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFF1D2235),
                                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = CyanPrimary,
                                                strokeWidth = 2.5.dp,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "EdVanced 1.1 Pro is synthesizing an answer for \"$query\"...",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Powered by EdVanced 1.1 Pro • Finding key facts and verified web links",
                                                    color = Color(0xFF90909E),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                is AiSearchUiState.Success -> {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xFF1D2238),
                                        border = BorderStroke(1.5.dp, CyanPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("tv_search_ai_overview_card")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(22.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CyanPrimary
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = Color(0xFF172778),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = "POWERED BY EDVANCED 1.1 PRO",
                                                            color = Color(0xFF172778),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = state.headline,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (state.summary.isNotBlank()) {
                                                Text(
                                                    text = state.summary,
                                                    color = Color(0xFFE3E1EC),
                                                    fontSize = 13.sp,
                                                    lineHeight = 19.sp
                                                )
                                            }

                                            if (state.keyPoints.isNotEmpty()) {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color(0xFF151828))
                                                        .padding(14.dp)
                                                ) {
                                                    state.keyPoints.forEach { point ->
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = "•",
                                                                color = EmeraldSuccess,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = point,
                                                                color = Color(0xFFD0CEE0),
                                                                fontSize = 12.sp,
                                                                lineHeight = 17.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (state.followUpQueries.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Ask follow-up:",
                                                        color = AmberAccent,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        items(state.followUpQueries) { followUp ->
                                                            var followFocused by remember { mutableStateOf(false) }
                                                            Surface(
                                                                shape = RoundedCornerShape(14.dp),
                                                                color = if (followFocused) CyanPrimary else Color(0xFF282D46),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (followFocused) Color.White else CyanPrimary.copy(alpha = 0.4f)
                                                                ),
                                                                modifier = Modifier
                                                                    .onFocusChanged { followFocused = it.isFocused }
                                                                    .clickable {
                                                                        query = followUp
                                                                        runGeminiSmartSearch(followUp)
                                                                    }
                                                            ) {
                                                                Text(
                                                                    text = followUp,
                                                                    color = if (followFocused) Color(0xFF172778) else Color.White,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (state.results.isNotEmpty()) {
                                                Text(
                                                    text = "AI Recommended Web Destinations (Opens in RNA. Browser):",
                                                    color = CyanPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                state.results.forEach { webRes ->
                                                    SearchWebResultCard(
                                                        result = webRes,
                                                        onClick = {
                                                            onDismiss()
                                                            onOpenWebUrlInBrowser(webRes.url, webRes.title)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is AiSearchUiState.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF2A1E24),
                                        border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.message,
                                                color = CoralAccent,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = { runGeminiSmartSearch(query.ifBlank { "Android TV Web Apps" }) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF3A2832),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Retry AI", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                AiSearchUiState.Idle -> {}
                            }

                            // Live Web Search Result Cards
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                liveWebResults.forEach { result ->
                                    SearchWebResultCard(
                                        result = result,
                                        onClick = {
                                            onDismiss()
                                            onOpenWebUrlInBrowser(result.url, result.title)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
            RnaTvKeyboardOverlay(
                visible = showSearchKeyboard && isRnaKeyboardDefaultIme(context),
                currentText = query,
                fieldHint = "Search Apps, Files on TV, or Ask AI...",
                showTextPreviewBar = true,
                onTextChange = { query = it },
                onCommitChar = { query += it },
                onBackspace = {
                    if (query.isNotEmpty()) {
                        query = query.dropLast(1)
                    }
                },
                onSearchOrSubmit = {
                    showSearchKeyboard = false
                    if (query.isNotBlank()) {
                        runGeminiSmartSearch(query.trim())
                    }
                },
                onDismiss = {
                    showSearchKeyboard = false
                }
            )
        }
    }
}

@Composable
private fun SearchAppResultCard(
    app: UnifiedLauncherApp,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SearchAppScale"
    )
    val decodedDataLogo = remember(app.customLogoUri) {
        decodeDataUriToImageBitmap(app.customLogoUri)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2B3046) else Color(0xFF1E202B)
        ),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .width(195.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF282A37),
                modifier = Modifier.size(46.dp)
            ) {
                when {
                    decodedDataLogo != null -> {
                        Image(
                            bitmap = decodedDataLogo,
                            contentDescription = app.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    !app.customLogoUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = app.customLogoUri,
                            contentDescription = app.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    app.nativeIcon != null -> {
                        val bmp = remember(app.packageName) {
                            try {
                                app.nativeIcon.toBitmap(120, 120).asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = app.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                    else -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = app.title.take(2).uppercase(),
                                color = CyanPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchFileResultRow(
    file: File,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val isRnaPackage = file.name.endsWith(".rna", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFocused) Color(0xFF2B3046) else Color(0xFF1E202B),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) EmeraldSuccess else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRnaPackage) EmeraldSuccess.copy(alpha = 0.18f) else CyanPrimary.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRnaPackage) Icons.Default.InstallMobile else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isRnaPackage) EmeraldSuccess else CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${(file.length() / 1024).coerceAtLeast(1)} KB",
                        color = Color(0xFF90909E),
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRnaPackage) EmeraldSuccess else CyanPrimary
            ) {
                Text(
                    text = if (isRnaPackage) "Install .rna" else "Open in File Explorer",
                    color = Color(0xFF11131A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchWebResultCard(
    result: TvSearchWebResult,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isFocused) Color(0xFF2B3046) else Color(0xFF191B24),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) CyanPrimary else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF303F9F)
                    ) {
                        Text(
                            text = result.source,
                            color = Color(0xFFDEE0FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = result.url,
                        color = Color(0xFF8F909E),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.snippet.isNotBlank()) {
                    Text(
                        text = result.snippet,
                        color = Color(0xFFC6C5D4),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF282A37),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Browser + Mouse",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

