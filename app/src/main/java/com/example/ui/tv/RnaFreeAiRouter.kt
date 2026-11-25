package com.example.ui.tv

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.RnaHomeInterceptorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

data class AiProviderOption(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val accentHex: Long
)

data class FreeAiModelSpec(
    val id: String,
    val displayName: String,
    val provider: String, // "kilo", "pollinations", "ovh", "aihorde", "2 providers"
    val rateLimitBadge: String,
    val contextBadge: String,
    val hasVision: Boolean = false,
    val hasTools: Boolean = false,
    val baseReliability: Int = 65,
    val speedScore: Int = 50,
    val intelligenceScore: Int = 50,
    val overallScore: Double = 0.525,
    val openAiModelId: String = id
)

data class FreeAiModelRuntimeState(
    val spec: FreeAiModelSpec,
    val enabled: Boolean,
    val effectiveReliability: Int,
    val successCount: Int,
    val failureCount: Int,
    val lastLatencyMs: Long,
    val customOrderIndex: Int,
    val consecutiveFailures: Int = 0,
    val ewmaSuccessRate: Double = 0.0,
    val avgLatencyMs: Long = 0L,
    val cooldownUntilMs: Long = 0L,
    val isDeviceLearned: Boolean = false,
    val disabledByProvider: Boolean = false
) {
    val isInCooldown: Boolean
        get() = cooldownUntilMs > System.currentTimeMillis()
}

enum class AiFallbackOrderMode(val id: String, val label: String) {
    AUTO_RELIABILITY("AUTO_RELIABILITY", "Auto-Sort by Reliability"),
    CUSTOM_ORDER("CUSTOM_ORDER", "Custom Fallback Order")
}

enum class OvhWhisperModelOption(val id: String, val displayName: String, val provider: String, val endpointUrl: String) {
    AUTO_CHAIN(
        id = "auto",
        displayName = "Auto (fallback chain)",
        provider = "ovh",
        endpointUrl = "https://whisper-large-v3-turbo.endpoints.kepler.ai.cloud.ovh.net/api/openai_compat/v1/audio/transcriptions"
    ),
    WHISPER_LARGE_V3(
        id = "whisper-large-v3",
        displayName = "Whisper Large v3 (OVH)",
        provider = "ovh",
        endpointUrl = "https://whisper-large-v3.endpoints.kepler.ai.cloud.ovh.net/api/openai_compat/v1/audio/transcriptions"
    ),
    WHISPER_LARGE_V3_TURBO(
        id = "whisper-large-v3-turbo",
        displayName = "Whisper Large v3 Turbo (OVH)",
        provider = "ovh",
        endpointUrl = "https://whisper-large-v3-turbo.endpoints.kepler.ai.cloud.ovh.net/api/openai_compat/v1/audio/transcriptions"
    );

    companion object {
        fun fromId(id: String?): OvhWhisperModelOption {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTO_CHAIN
        }
    }
}

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val badge: String
)

data class AiGenerationOutcome(
    val text: String,
    val modelUsedName: String,
    val providerUsed: String,
    val reliabilityScore: Int,
    val latencyMs: Long,
    val timeToFirstTokenMs: Long = (latencyMs * 38L / 100L).coerceAtLeast(1L),
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * Detailed telemetry record for a single AI request shown in the Control Panel -> AI USAGE INFO window.
 */
data class AiUsageLogRecord(
    val id: String,
    val timestampMs: Long,
    val modelName: String,
    val provider: String,
    val source: String,
    val promptText: String,
    val systemPromptSnippet: String,
    val responseText: String,
    val success: Boolean,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val timeToFirstTokenMs: Long,
    val totalLatencyMs: Long,
    val tokensPerSecond: Double,
    val fallbackAttemptsCount: Int,
    val fallbackModelsTried: String,
    val reliabilityScoreAfter: Int,
    val hasVisionAttachment: Boolean
)

private data class RawProviderCompletionResult(
    val text: String,
    val timeToFirstTokenMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

private class ProviderHttpException(
    val provider: String,
    val httpCode: Int,
    message: String
) : IllegalStateException(message) {
    val isRateLimited: Boolean get() = httpCode == 429
    val isModelNotFound: Boolean get() = httpCode == 400 || httpCode == 404
}

/**
 * Central AI Router for RNA. TV, RNA AI App, EdVanced Voice Assistant, and Autonomous Screen Agent.
 *
 * CRITICAL GUARANTEES:
 * - Never uses any developer-bundled Gemini API key (`BuildConfig.GEMINI_API_KEY`).
 * - By default (cold start with 0 calls), prefers **Kilo Gateway** (`kilo`) models first because
 *   Kilo Gateway is fast and reliable out of the box, followed by OVHcloud and AI Horde backups.
 * - Maintains a **separate per-device & per-TV-profile adaptive reliability table** using Bayesian
 *   empirical smoothing + recency EWMA + real measured latency + transient failure cooldowns.
 *   As a user actually uses AI on their device, real-use reliability quickly outweighs the initial
 *   cold-start prior so the router automatically prefers whichever models genuinely work best on
 *   that specific device/network.
 * - Optionally allows the user to enter their own Google Gemini API key (via TV Control Panel or
 *   via the RNA Phone Controller dropdown) and choose any Gemini model.
 */
object RnaFreeAiRouter {
    private const val TAG = "RnaFreeAiRouter"

    private const val KEY_USE_USER_GEMINI_KEY = "ai_router_use_user_gemini_key"
    private const val KEY_USER_GEMINI_API_KEY = "ai_router_user_gemini_api_key"
    private const val KEY_SELECTED_GEMINI_MODEL = "ai_router_selected_gemini_model"
    private const val KEY_FALLBACK_ORDER_MODE = "ai_router_fallback_order_mode"
    private const val KEY_MODEL_METRICS_JSON = "ai_router_model_metrics_json"
    private const val KEY_MODEL_METRICS_V2_PREFIX = "ai_router_model_metrics_v2_"
    private const val KEY_MODEL_METRICS_V3_PREFIX = "ai_router_model_metrics_v3_"
    private const val KEY_CUSTOM_ORDER_LIST = "ai_router_custom_order_ids"
    private const val KEY_DISABLED_MODELS_SET = "ai_router_disabled_model_ids"
    private const val KEY_DISABLED_PROVIDERS_SET = "ai_router_disabled_provider_ids"
    private const val KEY_WHISPER_DICTATION_ENABLED = "ai_router_whisper_dictation_enabled"
    private const val KEY_WHISPER_SELECTED_MODEL = "ai_router_whisper_selected_model"
    private const val KEY_AI_USAGE_LOGS_JSON = "ai_router_usage_logs_v1_json"
    private const val MAX_STORED_USAGE_LOGS = 200

    // Live provider catalog cache (refreshed at most once every 30 min, non-blocking 3s timeout)
    private const val LIVE_CATALOG_TTL_MS = 30 * 60_000L
    @Volatile private var cachedKiloFreeModelIds: List<String> = emptyList()
    @Volatile private var cachedKiloFetchedAtMs: Long = 0L
    @Volatile private var cachedOvhModelIds: List<String> = emptyList()
    @Volatile private var cachedOvhFetchedAtMs: Long = 0L
    @Volatile private var cachedPollinationsModelIds: List<String> = emptyList()
    @Volatile private var cachedPollinationsFetchedAtMs: Long = 0L
    @Volatile private var cachedAiHordeOnlineModels: List<String> = emptyList()
    @Volatile private var cachedAiHordeFetchedAtMs: Long = 0L

    val FREE_AI_PROVIDERS: List<AiProviderOption> = listOf(
        AiProviderOption("kilo", "Kilo Gateway", "kilo", 0xFF38BDF8L),
        AiProviderOption("ovh", "OVHcloud AI", "ovh", 0xFFA855F7L),
        AiProviderOption("pollinations", "Pollinations.ai", "pollinations", 0xFF34D399L),
        AiProviderOption("aihorde", "AI Horde", "aihorde", 0xFFF59E0BL)
    )

    val AVAILABLE_GEMINI_MODELS = listOf(
        GeminiModelOption("gemini-4-argon", "Gemini 4 Argon", "Next-Gen Flagship"),
        GeminiModelOption("gemini-4-hydrogen", "Gemini 4 Hydrogen", "Ultra-Fast Next-Gen"),
        GeminiModelOption("gemini-3.8-flash", "Gemini 3.8 Flash", "High-Speed Reasoning"),
        GeminiModelOption("gemini-3.6-flash", "Gemini 3.6 Flash", "Balanced Multimodal"),
        GeminiModelOption("gemini-3.5-flash", "Gemini 3.5 Flash", "Fast Multimodal"),
        GeminiModelOption("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra Low Latency"),
        GeminiModelOption("gemini-3.1-pro", "Gemini 3.1 Pro", "Deep Reasoning Pro"),
        GeminiModelOption("gemini-2.5-pro", "Gemini 2.5 Pro", "2M Context Pro"),
        GeminiModelOption("gemini-2.5-flash", "Gemini 2.5 Flash", "Fast & Multimodal"),
        GeminiModelOption("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", "Low Latency"),
        GeminiModelOption("gemini-2.0-flash", "Gemini 2.0 Flash", "Stable Multimodal")
    )

    val GEMINI_ROUTER_MODEL_SPECS: List<FreeAiModelSpec> = listOf(
        FreeAiModelSpec(
            id = "gemini-4-argon",
            displayName = "Gemini 4 Argon",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "2M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 98,
            speedScore = 93,
            intelligenceScore = 99,
            overallScore = 0.985,
            openAiModelId = "gemini-2.5-pro"
        ),
        FreeAiModelSpec(
            id = "gemini-4-hydrogen",
            displayName = "Gemini 4 Hydrogen",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "2M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 97,
            speedScore = 98,
            intelligenceScore = 98,
            overallScore = 0.978,
            openAiModelId = "gemini-2.5-flash"
        ),
        FreeAiModelSpec(
            id = "gemini-3.8-flash",
            displayName = "Gemini 3.8 Flash",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "1M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 97,
            speedScore = 97,
            intelligenceScore = 96,
            overallScore = 0.968,
            openAiModelId = "gemini-2.5-flash"
        ),
        FreeAiModelSpec(
            id = "gemini-3.6-flash",
            displayName = "Gemini 3.6 Flash",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "1M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 96,
            speedScore = 96,
            intelligenceScore = 95,
            overallScore = 0.958,
            openAiModelId = "gemini-2.5-flash"
        ),
        FreeAiModelSpec(
            id = "gemini-3.5-flash",
            displayName = "Gemini 3.5 Flash",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "1M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 96,
            speedScore = 96,
            intelligenceScore = 94,
            overallScore = 0.952,
            openAiModelId = "gemini-2.5-flash"
        ),
        FreeAiModelSpec(
            id = "gemini-3.5-flash-lite",
            displayName = "Gemini 3.5 Flash-Lite",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "1M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 95,
            speedScore = 99,
            intelligenceScore = 92,
            overallScore = 0.946,
            openAiModelId = "gemini-2.5-flash-lite"
        ),
        FreeAiModelSpec(
            id = "gemini-3.1-pro",
            displayName = "Gemini 3.1 Pro",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "2M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 95,
            speedScore = 90,
            intelligenceScore = 98,
            overallScore = 0.944,
            openAiModelId = "gemini-2.5-pro"
        ),
        FreeAiModelSpec(
            id = "gemini-2.5-pro",
            displayName = "Gemini 2.5 Pro",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "2M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 94,
            speedScore = 89,
            intelligenceScore = 97,
            overallScore = 0.935,
            openAiModelId = "gemini-2.5-pro"
        ),
        FreeAiModelSpec(
            id = "gemini-2.5-flash",
            displayName = "Gemini 2.5 Flash",
            provider = "gemini",
            rateLimitBadge = "User Key",
            contextBadge = "1M ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 94,
            speedScore = 95,
            intelligenceScore = 94,
            overallScore = 0.932,
            openAiModelId = "gemini-2.5-flash"
        )
    )

    val FREE_AI_CATALOG: List<FreeAiModelSpec> = listOf(
        // ── TIER 1 COLD-START DEFAULT: KILO GATEWAY (Fast, 200/hr anonymous free tier) ──
        FreeAiModelSpec(
            id = "kilo-auto",
            displayName = "Kilo Auto",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "256K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 94,
            speedScore = 90,
            intelligenceScore = 88,
            overallScore = 0.915,
            openAiModelId = "kilo-auto/free"
        ),
        FreeAiModelSpec(
            id = "dots3-note-preview",
            displayName = "Dots3 Note Preview",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "512K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 93,
            speedScore = 80,
            intelligenceScore = 96,
            overallScore = 0.898,
            openAiModelId = "kilo-auto/free"
        ),
        FreeAiModelSpec(
            id = "free-router",
            displayName = "Free Router",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "200K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 92,
            speedScore = 86,
            intelligenceScore = 85,
            overallScore = 0.884,
            openAiModelId = "kilo-auto/free"
        ),
        FreeAiModelSpec(
            id = "nemotron-3.5-lightning",
            displayName = "Nemotron 3.5 Lightning",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "1M ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 91,
            speedScore = 88,
            intelligenceScore = 83,
            overallScore = 0.872,
            openAiModelId = "nvidia/nemotron-3-nano-30b-a3b:free"
        ),
        FreeAiModelSpec(
            id = "nemotron-3-ultra-550b",
            displayName = "Nemotron 3 Ultra 550B",
            provider = "kilo",
            rateLimitBadge = "200/hr(trial)",
            contextBadge = "1M ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 90,
            speedScore = 66,
            intelligenceScore = 92,
            overallScore = 0.862,
            openAiModelId = "nvidia/nemotron-3-ultra-550b-a55b:free"
        ),
        FreeAiModelSpec(
            id = "north-mini-code",
            displayName = "North Mini Code",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "256K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 89,
            speedScore = 80,
            intelligenceScore = 82,
            overallScore = 0.852,
            openAiModelId = "qwen/qwen3-coder:free"
        ),
        FreeAiModelSpec(
            id = "nemotron-3.5-super-vl",
            displayName = "Nemotron 3.5 Super VL",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "256K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 89,
            speedScore = 74,
            intelligenceScore = 85,
            overallScore = 0.848,
            openAiModelId = "nvidia/nemotron-3.5-super-vl:free"
        ),
        FreeAiModelSpec(
            id = "nemotron-3-nano-omni-reasoning",
            displayName = "Nemotron 3 Nano Omni Reasoning",
            provider = "kilo",
            rateLimitBadge = "200/hr",
            contextBadge = "256K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 88,
            speedScore = 76,
            intelligenceScore = 81,
            overallScore = 0.840,
            openAiModelId = "nvidia/nemotron-nano-12b-v2-vl:free"
        ),
        // ── TIER 3 COLD-START: POLLINATIONS.AI (Keyless backup gateway; lower default reliability than Kilo) ──
        FreeAiModelSpec(
            id = "pollinations-openai",
            displayName = "Pollinations OpenAI",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 74,
            speedScore = 78,
            intelligenceScore = 84,
            overallScore = 0.720,
            openAiModelId = "openai"
        ),
        FreeAiModelSpec(
            id = "pollinations-openai-fast",
            displayName = "Pollinations OpenAI Fast",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 73,
            speedScore = 84,
            intelligenceScore = 78,
            overallScore = 0.712,
            openAiModelId = "openai-fast"
        ),
        FreeAiModelSpec(
            id = "pollinations-openai-large",
            displayName = "Pollinations OpenAI Large",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 72,
            speedScore = 70,
            intelligenceScore = 88,
            overallScore = 0.706,
            openAiModelId = "openai-large"
        ),
        FreeAiModelSpec(
            id = "pollinations-deepseek",
            displayName = "Pollinations DeepSeek",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 71,
            speedScore = 72,
            intelligenceScore = 85,
            overallScore = 0.696,
            openAiModelId = "deepseek"
        ),
        FreeAiModelSpec(
            id = "pollinations-qwen-coder",
            displayName = "Pollinations Qwen Coder",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 71,
            speedScore = 76,
            intelligenceScore = 82,
            overallScore = 0.692,
            openAiModelId = "qwen-coder"
        ),
        FreeAiModelSpec(
            id = "pollinations-deepseek-r1",
            displayName = "Pollinations DeepSeek R1",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 70,
            speedScore = 62,
            intelligenceScore = 86,
            overallScore = 0.684,
            openAiModelId = "deepseek-reasoning"
        ),
        FreeAiModelSpec(
            id = "pollinations-mistral",
            displayName = "Pollinations Mistral",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 69,
            speedScore = 76,
            intelligenceScore = 76,
            overallScore = 0.675,
            openAiModelId = "mistral"
        ),
        FreeAiModelSpec(
            id = "pollinations-llama",
            displayName = "Pollinations Llama",
            provider = "pollinations",
            rateLimitBadge = "Free",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 68,
            speedScore = 75,
            intelligenceScore = 78,
            overallScore = 0.665,
            openAiModelId = "llama"
        ),
        // ── TIER 3 COLD-START: OVHCLOUD AI ENDPOINTS (Solid free fallback, 2 RPM) ──
        FreeAiModelSpec(
            id = "qwen3.5-9b",
            displayName = "Qwen3.5 9B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "262K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 79,
            speedScore = 72,
            intelligenceScore = 74,
            overallScore = 0.745,
            openAiModelId = "Qwen3.5-9B"
        ),
        FreeAiModelSpec(
            id = "llama-3.3-70b",
            displayName = "Llama 3.3 70B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "128K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 78,
            speedScore = 62,
            intelligenceScore = 84,
            overallScore = 0.738,
            openAiModelId = "Meta-Llama-3_3-70B-Instruct"
        ),
        FreeAiModelSpec(
            id = "qwen3.8-27b",
            displayName = "Qwen3.8 27B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "262K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 77,
            speedScore = 60,
            intelligenceScore = 94,
            overallScore = 0.732,
            openAiModelId = "Qwen3.6-27B"
        ),
        FreeAiModelSpec(
            id = "qwen3.5-397b",
            displayName = "Qwen3.5 397B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "262K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 76,
            speedScore = 50,
            intelligenceScore = 92,
            overallScore = 0.722,
            openAiModelId = "Qwen3.5-397B-A17B"
        ),
        FreeAiModelSpec(
            id = "mistral-small-3.2-24b",
            displayName = "Mistral Small 3.2 24B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "128K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 76,
            speedScore = 68,
            intelligenceScore = 78,
            overallScore = 0.715,
            openAiModelId = "Mistral-Small-3.2-24B-Instruct-2506"
        ),
        FreeAiModelSpec(
            id = "qwen2.5-vl-72b",
            displayName = "Qwen2.5 VL 72B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "128K ctx",
            hasVision = true,
            hasTools = true,
            baseReliability = 75,
            speedScore = 54,
            intelligenceScore = 84,
            overallScore = 0.708,
            openAiModelId = "Qwen2.5-VL-72B-Instruct"
        ),
        FreeAiModelSpec(
            id = "qwen3.6-27b",
            displayName = "Qwen3.6 27B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "131K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 75,
            speedScore = 48,
            intelligenceScore = 80,
            overallScore = 0.695,
            openAiModelId = "Qwen3.6-27B"
        ),
        FreeAiModelSpec(
            id = "gpt-oss-120b",
            displayName = "GPT Oss 120B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "131K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 74,
            speedScore = 52,
            intelligenceScore = 78,
            overallScore = 0.688,
            openAiModelId = "gpt-oss-120b"
        ),
        FreeAiModelSpec(
            id = "gpt-oss-20b",
            displayName = "GPT Oss 20B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "131K ctx",
            hasVision = false,
            hasTools = true,
            baseReliability = 74,
            speedScore = 68,
            intelligenceScore = 68,
            overallScore = 0.678,
            openAiModelId = "gpt-oss-20b"
        ),
        // ── TIER 3 COLD-START: AI HORDE COMMUNITY CLUSTER (Backup pool) ──
        FreeAiModelSpec(
            id = "gemma-4-31b-heretic",
            displayName = "Gemma 4 31B Heretic",
            provider = "aihorde",
            rateLimitBadge = "Free Cluster",
            contextBadge = "32K ctx",
            hasVision = false,
            hasTools = false,
            baseReliability = 66,
            speedScore = 40,
            intelligenceScore = 68,
            overallScore = 0.545
        ),
        FreeAiModelSpec(
            id = "gemma-4-e4b-ultra-uncensored",
            displayName = "Gemma 4 E4B Ultra Uncensored",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 58,
            speedScore = 48,
            intelligenceScore = 54,
            overallScore = 0.495
        ),
        FreeAiModelSpec(
            id = "gemma-4-e4b-uncensored",
            displayName = "Gemma 4 E4B Uncensored",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 57,
            speedScore = 48,
            intelligenceScore = 52,
            overallScore = 0.490
        ),
        FreeAiModelSpec(
            id = "llama-3.2-3b",
            displayName = "Llama 3.2 3B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 60,
            speedScore = 70,
            intelligenceScore = 45,
            overallScore = 0.488
        ),
        FreeAiModelSpec(
            id = "llama-3-2-3b-instruct",
            displayName = "Llama-3 2-3B-Instruct",
            provider = "2 providers",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 60,
            speedScore = 68,
            intelligenceScore = 45,
            overallScore = 0.485
        ),
        FreeAiModelSpec(
            id = "llama-3-super-nova-rp-8b",
            displayName = "Llama 3 Super Nova RP 8B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 56,
            speedScore = 50,
            intelligenceScore = 48,
            overallScore = 0.480
        ),
        FreeAiModelSpec(
            id = "magidonia-24b",
            displayName = "[ctx:65536] Magidonia-24B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "64K ctx",
            baseReliability = 55,
            speedScore = 38,
            intelligenceScore = 58,
            overallScore = 0.478
        ),
        FreeAiModelSpec(
            id = "behemoth-x-123b-v2-1",
            displayName = "Behemoth-X-123B-v2 1",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 54,
            speedScore = 28,
            intelligenceScore = 72,
            overallScore = 0.476
        ),
        FreeAiModelSpec(
            id = "thedrummer-behemoth-123b",
            displayName = "TheDrummer Behemoth-123B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 54,
            speedScore = 28,
            intelligenceScore = 70,
            overallScore = 0.474
        ),
        FreeAiModelSpec(
            id = "cydonia-24b-v4.3",
            displayName = "Cydonia 24B v4.3",
            provider = "2 providers",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 56,
            speedScore = 42,
            intelligenceScore = 56,
            overallScore = 0.472
        ),
        FreeAiModelSpec(
            id = "skyfall-31b-v4.2",
            displayName = "Skyfall 31B v4.2",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 55,
            speedScore = 38,
            intelligenceScore = 60,
            overallScore = 0.470
        ),
        FreeAiModelSpec(
            id = "angelic-eclipse-12b",
            displayName = "Angelic Eclipse 12B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 53,
            speedScore = 46,
            intelligenceScore = 50,
            overallScore = 0.465
        ),
        FreeAiModelSpec(
            id = "forgotten-safeword-22b",
            displayName = "Forgotten-Safeword-22B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 52,
            speedScore = 40,
            intelligenceScore = 54,
            overallScore = 0.462
        ),
        FreeAiModelSpec(
            id = "impish-bloodmoon-12b",
            displayName = "Impish Bloodmoon 12B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 52,
            speedScore = 45,
            intelligenceScore = 50,
            overallScore = 0.460
        ),
        FreeAiModelSpec(
            id = "qwen3-4b-nymphaea-rp",
            displayName = "Qwen3-4B-Nymphaea-RP",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 52,
            speedScore = 60,
            intelligenceScore = 44,
            overallScore = 0.456
        ),
        FreeAiModelSpec(
            id = "qwen3.5-0.8b",
            displayName = "Qwen3.5 0.8B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 51,
            speedScore = 80,
            intelligenceScore = 35,
            overallScore = 0.452
        ),
        FreeAiModelSpec(
            id = "qwen3guard-gen-8b",
            displayName = "Qwen3Guard Gen 8B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "32K ctx",
            baseReliability = 58,
            speedScore = 60,
            intelligenceScore = 48,
            overallScore = 0.450,
            openAiModelId = "Qwen3Guard-Gen-8B"
        ),
        FreeAiModelSpec(
            id = "llama-3.2-1b-unsloth",
            displayName = "Llama 3.2 1B Unsloth",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 50,
            speedScore = 78,
            intelligenceScore = 36,
            overallScore = 0.448
        ),
        FreeAiModelSpec(
            id = "llama-3-2-1b-instruct",
            displayName = "Llama-3 2-1B-Instruct",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 50,
            speedScore = 76,
            intelligenceScore = 36,
            overallScore = 0.445
        ),
        FreeAiModelSpec(
            id = "qwen3-0.6b-iq4",
            displayName = "Qwen3 0.6B IQ4",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 49,
            speedScore = 82,
            intelligenceScore = 32,
            overallScore = 0.440
        ),
        FreeAiModelSpec(
            id = "qwen3guard-gen-0.6b",
            displayName = "Qwen3Guard Gen 0.6B",
            provider = "ovh",
            rateLimitBadge = "2 RPM",
            contextBadge = "32K ctx",
            baseReliability = 55,
            speedScore = 75,
            intelligenceScore = 35,
            overallScore = 0.438,
            openAiModelId = "Qwen3Guard-Gen-0.6B"
        ),
        FreeAiModelSpec(
            id = "impish-llama-4b",
            displayName = "Impish LLAMA 4B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 48,
            speedScore = 62,
            intelligenceScore = 40,
            overallScore = 0.435
        ),
        FreeAiModelSpec(
            id = "llama-3-stheno-8b-v3.2",
            displayName = "Llama 3 Stheno 8B v3.2",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 51,
            speedScore = 52,
            intelligenceScore = 46,
            overallScore = 0.432
        ),
        FreeAiModelSpec(
            id = "l3-8b-stheno-v3-2-q5-k-m",
            displayName = "L3-8B-Stheno-v3 2-Q5 K M",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 50,
            speedScore = 50,
            intelligenceScore = 46,
            overallScore = 0.430
        ),
        FreeAiModelSpec(
            id = "mini-magnum-12b-v1.1",
            displayName = "Mini Magnum 12B v1.1",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 50,
            speedScore = 46,
            intelligenceScore = 50,
            overallScore = 0.428
        ),
        FreeAiModelSpec(
            id = "mn-violet-lotus-12b-q8-0",
            displayName = "MN-Violet-Lotus-12B Q8 0",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 49,
            speedScore = 44,
            intelligenceScore = 50,
            overallScore = 0.425
        ),
        FreeAiModelSpec(
            id = "rocinante-x-12b",
            displayName = "Rocinante-X-12B",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 49,
            speedScore = 45,
            intelligenceScore = 50,
            overallScore = 0.422
        ),
        FreeAiModelSpec(
            id = "thea-rp-3b-25r",
            displayName = "thea-rp-3b-25r",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 48,
            speedScore = 66,
            intelligenceScore = 38,
            overallScore = 0.418
        ),
        FreeAiModelSpec(
            id = "qwen2.5-coder-3b-instruct",
            displayName = "Qwen2.5 Coder 3B Instruct",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "32K ctx",
            baseReliability = 48,
            speedScore = 65,
            intelligenceScore = 44,
            overallScore = 0.415
        ),
        FreeAiModelSpec(
            id = "cerebras-gpt-111m-instruct",
            displayName = "Cerebras GPT 111M Instruct",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "8K ctx",
            baseReliability = 45,
            speedScore = 88,
            intelligenceScore = 25,
            overallScore = 0.400
        ),
        FreeAiModelSpec(
            id = "pythia-70m-deduped",
            displayName = "Pythia 70M Deduped",
            provider = "aihorde",
            rateLimitBadge = "Free",
            contextBadge = "4K ctx",
            baseReliability = 44,
            speedScore = 90,
            intelligenceScore = 20,
            overallScore = 0.390
        )
    )

    private val _lastUsedModelSummary = MutableStateFlow("Kilo Auto (kilo • 94% rel)")
    val lastUsedModelSummary: StateFlow<String> = _lastUsedModelSummary.asStateFlow()

    private val _configVersion = MutableStateFlow(0)
    val configVersion: StateFlow<Int> = _configVersion.asStateFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Scopes reliability metrics per user (each device has local storage, and each TV profile
     * on the device keeps its own separate learned reliability scores).
     */
    private fun activeUserMetricsKey(context: Context): String {
        val profileId = runCatching { getActiveTvProfileId(prefs(context)) }
            .getOrDefault("default")
            .ifBlank { "default" }
        return "$KEY_MODEL_METRICS_V3_PREFIX$profileId"
    }

    private fun loadActiveUserMetricsRoot(context: Context): JSONObject {
        val p = prefs(context)
        val scopedKey = activeUserMetricsKey(context)
        val scopedRaw = p.getString(scopedKey, null)
        if (!scopedRaw.isNullOrBlank()) {
            return runCatching { JSONObject(scopedRaw) }.getOrDefault(JSONObject())
        }
        return JSONObject()
    }

    private fun providerColdStartPriority(provider: String): Int = when (provider.lowercase(Locale.US)) {
        "gemini" -> 5
        "kilo" -> 4
        "ovh" -> 3
        "pollinations" -> 2
        "2 providers" -> 1
        else -> 0 // aihorde
    }

    /**
     * Computes live adaptive reliability (5..99) for a model on this user's device:
     * - At 0 calls (cold start): uses `spec.baseReliability`, which prefers Kilo Gateway first.
     * - Once real calls occur: blends Bayesian empirical success rate (light prior K=2.0) with
     *   recency-weighted EWMA (alpha=0.35), adds a real-use proof & latency bonus for models that
     *   genuinely work fast on this device, and applies a time-decaying consecutive-failure penalty
     *   so failing models drop immediately while still able to recover later.
     */
    private fun computeAdaptiveReliabilityScore(
        spec: FreeAiModelSpec,
        successCount: Int,
        failureCount: Int,
        consecutiveFailures: Int,
        ewmaSuccess: Double,
        avgLatencyMs: Long,
        lastFailureMs: Long,
        nowMs: Long
    ): Int {
        val totalCalls = successCount + failureCount
        if (totalCalls <= 0) {
            return spec.baseReliability.coerceIn(10, 99)
        }

        val priorProb = (spec.baseReliability / 100.0).coerceIn(0.20, 0.96)
        // Light pseudo-count K = 2.0 -> after 2 real calls, 50% is real device data; after 6 calls, 75%+ is real!
        val priorWeight = 2.0
        val bayesianRate = (successCount + priorWeight * priorProb) / (totalCalls + priorWeight)
        val clampedEwma = ewmaSuccess.coerceIn(0.0, 1.0)
        val blendedRate = (0.55 * bayesianRate) + (0.45 * clampedEwma)

        // Proven real-use bonus: prefer models that actually succeeded on this device over untested standby models
        val realUseProofBonus = if (successCount > 0 && consecutiveFailures == 0) {
            min(7.0, successCount * 1.75)
        } else {
            0.0
        }

        // Real measured latency adjustment on this user's network
        val speedAdjustment = when {
            avgLatencyMs in 1L..2500L -> 3.0
            avgLatencyMs in 2501L..5500L -> 1.5
            avgLatencyMs > 12_000L -> -3.0
            else -> 0.0
        }

        // Consecutive failure penalty with gentle time decay so temporarily down providers can recover later
        val rawStreakPenalty = min(34.0, consecutiveFailures * 11.0)
        val streakPenalty = if (consecutiveFailures > 0 && lastFailureMs > 0L) {
            val ageMs = (nowMs - lastFailureMs).coerceAtLeast(0L)
            when {
                ageMs > 3 * 60 * 60_000L -> rawStreakPenalty * 0.25
                ageMs > 30 * 60_000L -> rawStreakPenalty * 0.50
                else -> rawStreakPenalty
            }
        } else {
            rawStreakPenalty
        }

        val score = (blendedRate * 100.0) + realUseProofBonus + speedAdjustment - streakPenalty
        return score.roundToInt().coerceIn(5, 99)
    }

    fun notifyConfigChanged() {
        _configVersion.value += 1
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // USER GEMINI API KEY & MODEL SETTINGS (NEVER BUNDLED KEY)
    // ══════════════════════════════════════════════════════════════════════════════

    fun isUsingUserGeminiKey(context: Context): Boolean {
        val p = prefs(context)
        val hasKey = getUserGeminiApiKey(context).isNotBlank()
        return hasKey && p.getBoolean(KEY_USE_USER_GEMINI_KEY, false)
    }

    fun setUsingUserGeminiKey(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_USER_GEMINI_KEY, enabled).apply()
        notifyConfigChanged()
    }

    fun getUserGeminiApiKey(context: Context): String {
        return prefs(context).getString(KEY_USER_GEMINI_API_KEY, "")?.trim().orEmpty()
    }

    fun saveUserGeminiApiKey(context: Context, apiKey: String, autoActivate: Boolean = true) {
        val clean = apiKey.trim()
        val editor = prefs(context).edit().putString(KEY_USER_GEMINI_API_KEY, clean)
        if (clean.isNotEmpty() && autoActivate) {
            editor.putBoolean(KEY_USE_USER_GEMINI_KEY, true)
        } else if (clean.isEmpty()) {
            editor.putBoolean(KEY_USE_USER_GEMINI_KEY, false)
        }
        editor.apply()
        if (clean.isNotEmpty() && autoActivate) {
            val model = getSelectedGeminiModel(context)
            _lastUsedModelSummary.value = "User Gemini ($model)"
        }
        notifyConfigChanged()
    }

    fun getSelectedGeminiModel(context: Context): String {
        return prefs(context).getString(KEY_SELECTED_GEMINI_MODEL, "gemini-2.5-flash")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "gemini-2.5-flash"
    }

    fun setSelectedGeminiModel(context: Context, modelId: String) {
        val clean = modelId.trim().ifBlank { "gemini-2.5-flash" }
        prefs(context).edit().putString(KEY_SELECTED_GEMINI_MODEL, clean).apply()
        if (isUsingUserGeminiKey(context)) {
            _lastUsedModelSummary.value = "User Gemini ($clean)"
        }
        notifyConfigChanged()
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // OVH WHISPER DICTATION SETTINGS (DISABLED BY DEFAULT)
    // ══════════════════════════════════════════════════════════════════════════════

    fun isWhisperDictationEnabled(@Suppress("UNUSED_PARAMETER") context: Context): Boolean {
        return false
    }

    fun setWhisperDictationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WHISPER_DICTATION_ENABLED, false).apply()
        notifyConfigChanged()
    }

    fun getSelectedWhisperModel(context: Context): OvhWhisperModelOption {
        val saved = prefs(context).getString(KEY_WHISPER_SELECTED_MODEL, OvhWhisperModelOption.AUTO_CHAIN.id)
        return OvhWhisperModelOption.fromId(saved)
    }

    fun setSelectedWhisperModel(context: Context, option: OvhWhisperModelOption) {
        prefs(context).edit().putString(KEY_WHISPER_SELECTED_MODEL, option.id).apply()
        notifyConfigChanged()
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // FREE AI FALLBACK ORDER & DYNAMIC RELIABILITY TRACKING
    // ══════════════════════════════════════════════════════════════════════════════

    fun getFallbackOrderMode(context: Context): AiFallbackOrderMode {
        val raw = prefs(context).getString(KEY_FALLBACK_ORDER_MODE, AiFallbackOrderMode.AUTO_RELIABILITY.id)
        return if (raw == AiFallbackOrderMode.CUSTOM_ORDER.id) {
            AiFallbackOrderMode.CUSTOM_ORDER
        } else {
            AiFallbackOrderMode.AUTO_RELIABILITY
        }
    }

    fun setFallbackOrderMode(context: Context, mode: AiFallbackOrderMode) {
        prefs(context).edit().putString(KEY_FALLBACK_ORDER_MODE, mode.id).apply()
        notifyConfigChanged()
    }

    private fun getActiveCatalogForContext(context: Context): List<FreeAiModelSpec> {
        val hasUserGeminiKey = getUserGeminiApiKey(context).isNotBlank()
        return if (hasUserGeminiKey) {
            GEMINI_ROUTER_MODEL_SPECS + FREE_AI_CATALOG
        } else {
            FREE_AI_CATALOG
        }
    }

    fun getDisabledProviders(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_DISABLED_PROVIDERS_SET, emptySet())
            .orEmpty()
            .map { it.lowercase(Locale.US) }
            .toSet()
    }

    fun isProviderEnabled(context: Context, providerId: String): Boolean {
        val disabled = getDisabledProviders(context)
        val norm = providerId.lowercase(Locale.US)
        return when (norm) {
            "2 providers" -> ("kilo" !in disabled) || ("aihorde" !in disabled)
            else -> norm !in disabled
        }
    }

    fun setProviderEnabled(context: Context, providerId: String, enabled: Boolean) {
        val norm = providerId.lowercase(Locale.US)
        val p = prefs(context)
        val current = p.getStringSet(KEY_DISABLED_PROVIDERS_SET, emptySet())
            .orEmpty()
            .map { it.lowercase(Locale.US) }
            .toMutableSet()
        if (enabled) current.remove(norm) else current.add(norm)
        p.edit().putStringSet(KEY_DISABLED_PROVIDERS_SET, current).apply()
        notifyConfigChanged()
    }

    fun getModelCountForProvider(providerId: String): Int {
        val norm = providerId.lowercase(Locale.US)
        return FREE_AI_CATALOG.count { spec ->
            val sp = spec.provider.lowercase(Locale.US)
            sp == norm || (sp == "2 providers" && (norm == "kilo" || norm == "aihorde"))
        }
    }

    fun getAllModelsWithRuntimeState(context: Context): List<FreeAiModelRuntimeState> {
        val p = prefs(context)
        val disabledSet = p.getStringSet(KEY_DISABLED_MODELS_SET, emptySet()).orEmpty()
        val disabledProviders = getDisabledProviders(context)
        val customOrderCsv = p.getString(KEY_CUSTOM_ORDER_LIST, "").orEmpty()
        val customOrderList = customOrderCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val metricsJson = loadActiveUserMetricsRoot(context)
        val nowMs = System.currentTimeMillis()

        val catalog = getActiveCatalogForContext(context)
        val rawStates = catalog.mapIndexed { defaultIdx, spec ->
            val m = metricsJson.optJSONObject(spec.id)
            val succ = m?.optInt("successCount", 0) ?: 0
            val fail = m?.optInt("failureCount", 0) ?: 0
            val consecFail = m?.optInt("consecutiveFailures", 0) ?: 0
            val lastLat = m?.optLong("lastLatencyMs", 0L) ?: 0L
            val avgLat = m?.optLong("avgLatencyMs", lastLat) ?: lastLat
            val lastFailMs = m?.optLong("lastFailureMs", 0L) ?: 0L
            val cooldownUntil = m?.optLong("cooldownUntilMs", 0L) ?: 0L
            val defaultPriorEwma = (spec.baseReliability / 100.0).coerceIn(0.20, 0.96)
            val ewma = if (m != null && m.has("ewmaSuccess")) {
                m.optDouble("ewmaSuccess", defaultPriorEwma)
            } else if (succ + fail > 0) {
                succ.toDouble() / (succ + fail).toDouble()
            } else {
                defaultPriorEwma
            }
            val rel = computeAdaptiveReliabilityScore(
                spec = spec,
                successCount = succ,
                failureCount = fail,
                consecutiveFailures = consecFail,
                ewmaSuccess = ewma,
                avgLatencyMs = avgLat,
                lastFailureMs = lastFailMs,
                nowMs = nowMs
            )
            val normProvider = spec.provider.lowercase(Locale.US)
            val providerEnabled = when (normProvider) {
                "2 providers" -> ("kilo" !in disabledProviders) || ("aihorde" !in disabledProviders)
                else -> normProvider !in disabledProviders
            }
            val customIdx = customOrderList.indexOf(spec.id).let { if (it >= 0) it else (1000 + defaultIdx) }
            FreeAiModelRuntimeState(
                spec = spec,
                enabled = providerEnabled && (spec.id !in disabledSet),
                effectiveReliability = rel,
                successCount = succ,
                failureCount = fail,
                lastLatencyMs = lastLat,
                customOrderIndex = customIdx,
                consecutiveFailures = consecFail,
                ewmaSuccessRate = ewma,
                avgLatencyMs = avgLat,
                cooldownUntilMs = cooldownUntil,
                isDeviceLearned = (succ + fail) > 0,
                disabledByProvider = !providerEnabled
            )
        }

        return when (getFallbackOrderMode(context)) {
            AiFallbackOrderMode.AUTO_RELIABILITY -> {
                rawStates.sortedWith(
                    compareByDescending<FreeAiModelRuntimeState> { it.cooldownUntilMs <= nowMs }
                        .thenByDescending { it.effectiveReliability }
                        .thenByDescending { it.successCount > 0 && it.consecutiveFailures == 0 }
                        .thenByDescending { providerColdStartPriority(it.spec.provider) }
                        .thenByDescending { it.spec.overallScore }
                        .thenByDescending { it.spec.intelligenceScore }
                )
            }
            AiFallbackOrderMode.CUSTOM_ORDER -> {
                rawStates.sortedBy { it.customOrderIndex }
            }
        }
    }

    fun setModelEnabled(context: Context, modelId: String, enabled: Boolean) {
        val p = prefs(context)
        val current = p.getStringSet(KEY_DISABLED_MODELS_SET, emptySet()).orEmpty().toMutableSet()
        if (enabled) current.remove(modelId) else current.add(modelId)
        p.edit().putStringSet(KEY_DISABLED_MODELS_SET, current).apply()
        notifyConfigChanged()
    }

    fun moveModelInCustomOrder(context: Context, modelId: String, delta: Int) {
        val currentStates = getAllModelsWithRuntimeState(context)
        val ids = currentStates.map { it.spec.id }.toMutableList()
        val idx = ids.indexOf(modelId)
        if (idx < 0) return
        val targetIdx = (idx + delta).coerceIn(0, ids.lastIndex)
        if (idx == targetIdx) return
        val item = ids.removeAt(idx)
        ids.add(targetIdx, item)
        prefs(context).edit()
            .putString(KEY_CUSTOM_ORDER_LIST, ids.joinToString(","))
            .putString(KEY_FALLBACK_ORDER_MODE, AiFallbackOrderMode.CUSTOM_ORDER.id)
            .apply()
        notifyConfigChanged()
    }

    fun resetReliabilityMetricsAndOrder(context: Context) {
        val profileId = runCatching { getActiveTvProfileId(prefs(context)) }
            .getOrDefault("default")
            .ifBlank { "default" }
        prefs(context).edit()
            .remove(activeUserMetricsKey(context))
            .remove("$KEY_MODEL_METRICS_V2_PREFIX$profileId")
            .remove(KEY_MODEL_METRICS_JSON)
            .remove(KEY_CUSTOM_ORDER_LIST)
            .remove(KEY_DISABLED_MODELS_SET)
            .remove(KEY_DISABLED_PROVIDERS_SET)
            .putString(KEY_FALLBACK_ORDER_MODE, AiFallbackOrderMode.AUTO_RELIABILITY.id)
            .apply()
        _lastUsedModelSummary.value = "Kilo Auto (kilo • 94% rel)"
        notifyConfigChanged()
    }

    @Synchronized
    private fun recordModelSuccess(context: Context, modelId: String, latencyMs: Long): Int {
        val p = prefs(context)
        val spec = (GEMINI_ROUTER_MODEL_SPECS + FREE_AI_CATALOG).firstOrNull { it.id == modelId }
        val baseRel = spec?.baseReliability ?: 65
        val root = loadActiveUserMetricsRoot(context)
        val nowMs = System.currentTimeMillis()

        val entry = root.optJSONObject(modelId) ?: JSONObject()
        val succ = entry.optInt("successCount", 0) + 1
        val fail = entry.optInt("failureCount", 0)
        val defaultPrior = (baseRel / 100.0).coerceIn(0.20, 0.96)
        val prevEwma = entry.optDouble("ewmaSuccess", defaultPrior)
        val newEwma = (0.65 * prevEwma) + (0.35 * 1.0)
        val prevAvgLat = entry.optLong("avgLatencyMs", 0L)
        val newAvgLat = if (prevAvgLat <= 0L) latencyMs else ((prevAvgLat * 65L) + (latencyMs * 35L)) / 100L

        val newRel = if (spec != null) {
            computeAdaptiveReliabilityScore(
                spec = spec,
                successCount = succ,
                failureCount = fail,
                consecutiveFailures = 0,
                ewmaSuccess = newEwma,
                avgLatencyMs = newAvgLat,
                lastFailureMs = entry.optLong("lastFailureMs", 0L),
                nowMs = nowMs
            )
        } else {
            (newEwma * 100.0).roundToInt().coerceIn(10, 99)
        }

        entry.put("reliability", newRel)
        entry.put("successCount", succ)
        entry.put("failureCount", fail)
        entry.put("consecutiveFailures", 0)
        entry.put("ewmaSuccess", newEwma)
        entry.put("lastLatencyMs", latencyMs)
        entry.put("avgLatencyMs", newAvgLat)
        entry.put("lastSuccessMs", nowMs)
        entry.put("cooldownUntilMs", 0L)
        root.put(modelId, entry)
        p.edit().putString(activeUserMetricsKey(context), root.toString()).apply()
        notifyConfigChanged()
        return newRel
    }

    @Synchronized
    private fun recordModelFailure(context: Context, modelId: String, isRateLimited: Boolean = false) {
        val p = prefs(context)
        val spec = (GEMINI_ROUTER_MODEL_SPECS + FREE_AI_CATALOG).firstOrNull { it.id == modelId }
        val baseRel = spec?.baseReliability ?: 65
        val root = loadActiveUserMetricsRoot(context)
        val nowMs = System.currentTimeMillis()

        val entry = root.optJSONObject(modelId) ?: JSONObject()
        val succ = entry.optInt("successCount", 0)
        val fail = entry.optInt("failureCount", 0) + 1
        val consecFail = entry.optInt("consecutiveFailures", 0) + 1
        val defaultPrior = (baseRel / 100.0).coerceIn(0.20, 0.96)
        val prevEwma = entry.optDouble("ewmaSuccess", defaultPrior)
        // Rate limits (429) are transient, so soften EWMA decay slightly vs hard errors while setting cooldown
        val alpha = if (isRateLimited) 0.25 else 0.35
        val newEwma = (1.0 - alpha) * prevEwma
        val avgLat = entry.optLong("avgLatencyMs", entry.optLong("lastLatencyMs", 0L))

        val cooldownDurationMs = when {
            isRateLimited -> 90_000L // 90s backoff on HTTP 429
            consecFail == 1 -> 15_000L // 15s quick skip to try healthier models
            consecFail == 2 -> 120_000L // 2m backoff on repeated error
            else -> 480_000L // 8m backoff on persistent outage (still auto-recovers later!)
        }
        val cooldownUntil = nowMs + cooldownDurationMs

        val newRel = if (spec != null) {
            computeAdaptiveReliabilityScore(
                spec = spec,
                successCount = succ,
                failureCount = fail,
                consecutiveFailures = consecFail,
                ewmaSuccess = newEwma,
                avgLatencyMs = avgLat,
                lastFailureMs = nowMs,
                nowMs = nowMs
            )
        } else {
            (newEwma * 100.0).roundToInt().coerceIn(5, 99)
        }

        entry.put("reliability", newRel)
        entry.put("successCount", succ)
        entry.put("failureCount", fail)
        entry.put("consecutiveFailures", consecFail)
        entry.put("ewmaSuccess", newEwma)
        entry.put("lastFailureMs", nowMs)
        entry.put("cooldownUntilMs", cooldownUntil)
        root.put(modelId, entry)
        p.edit().putString(activeUserMetricsKey(context), root.toString()).apply()
        notifyConfigChanged()
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // AI USAGE INFO TELEMETRY LOGGING (PERSISTED + SELECTIVE & FULL CLEARANCE)
    // ══════════════════════════════════════════════════════════════════════════════

    private fun estimateTokenCount(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        return ((text.trim().length + 3) / 4).coerceAtLeast(1)
    }

    /**
     * Strips internal RNA system instructions (`$start ... rna-tools;`, `CAPABILITIES & ACTION TAGS`,
     * `INSTALLED APPS ON DEVICE:`, etc.) so AI Usage Info only displays and counts tokens for the
     * actual question/message entered by the user.
     */
    fun extractCleanUserQuestion(rawPrompt: String, explicitUserPrompt: String? = null): String {
        if (!explicitUserPrompt.isNullOrBlank()) {
            return explicitUserPrompt.trim()
        }
        val trimmed = rawPrompt.trim()
        // Check for "USER REQUEST:" marker (used by TvVoiceAssistantOverlay)
        val userReqIdx = trimmed.lastIndexOf("USER REQUEST:", ignoreCase = true)
        if (userReqIdx >= 0) {
            val after = trimmed.substring(userReqIdx + "USER REQUEST:".length)
                .substringBefore("\nRespond naturally")
                .trim()
            if (after.isNotBlank()) return after
        }
        // Check for "USER:" in "CONVERSATION HISTORY:" (used by RnaAiTvAppScreen)
        val lastUserIdx = trimmed.lastIndexOf("\nUSER:", ignoreCase = true)
        if (lastUserIdx >= 0) {
            val afterUser = trimmed.substring(lastUserIdx + "\nUSER:".length)
                .substringBefore("\nRNA. AI:")
                .substringBefore("\nProvide a helpful")
                .trim()
            if (afterUser.isNotBlank()) return afterUser
        }
        // Check for "USER GOAL:" (used by RnaAutonomousAgent)
        val userGoalIdx = trimmed.lastIndexOf("USER GOAL:", ignoreCase = true)
        if (userGoalIdx >= 0) {
            val afterGoal = trimmed.substring(userGoalIdx + "USER GOAL:".length)
                .lineSequence()
                .firstOrNull()
                ?.trim()
                .orEmpty()
            if (afterGoal.isNotBlank()) return afterGoal
        }
        // Strip any $start ... rna-tools; block if present
        if (trimmed.contains("rna-tools;", ignoreCase = true)) {
            val afterTools = trimmed.substringAfterLast("rna-tools;", "").trim()
            if (afterTools.isNotBlank()) return afterTools
        }
        return trimmed
    }

    private fun inferSourceFromPrompts(systemInstruction: String?, prompt: String): String {
        val sys = systemInstruction.orEmpty().lowercase(Locale.US)
        val p = prompt.lowercase(Locale.US)
        return when {
            sys.contains("autonomous") || p.contains("autonomous") || sys.contains("screen agent") || p.contains("ui element") -> "Autonomous Screen Agent"
            sys.contains("edvanced") || sys.contains("voice assistant") || p.contains("user request:") -> "EdVanced Voice Assistant"
            sys.contains("search") || p.contains("search overview") -> "TV Search AI"
            sys.contains("keyboard") || sys.contains("dictation") || p.contains("punctuation") -> "RNA Keyboard Voice"
            else -> "RNA. AI Chat"
        }
    }

    @Synchronized
    fun getUsageLogs(context: Context): List<AiUsageLogRecord> {
        val raw = prefs(context).getString(KEY_AI_USAGE_LOGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawSavedPrompt = obj.optString("promptText", "")
                    val cleanPrompt = extractCleanUserQuestion(rawSavedPrompt, null)
                    val userOnlyPromptTokens = estimateTokenCount(cleanPrompt)
                    val compTokens = obj.optInt("completionTokens", 0)
                    add(
                        AiUsageLogRecord(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                            modelName = obj.optString("modelName", "Unknown Model"),
                            provider = obj.optString("provider", "free"),
                            source = obj.optString("source", "RNA. AI"),
                            promptText = cleanPrompt,
                            systemPromptSnippet = "",
                            responseText = obj.optString("responseText", ""),
                            success = obj.optBoolean("success", true),
                            promptTokens = userOnlyPromptTokens,
                            completionTokens = compTokens,
                            totalTokens = userOnlyPromptTokens + compTokens,
                            timeToFirstTokenMs = obj.optLong("timeToFirstTokenMs", 0L),
                            totalLatencyMs = obj.optLong("totalLatencyMs", 0L),
                            tokensPerSecond = obj.optDouble("tokensPerSecond", 0.0),
                            fallbackAttemptsCount = obj.optInt("fallbackAttemptsCount", 1),
                            fallbackModelsTried = obj.optString("fallbackModelsTried", ""),
                            reliabilityScoreAfter = obj.optInt("reliabilityScoreAfter", 80),
                            hasVisionAttachment = obj.optBoolean("hasVisionAttachment", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun appendUsageLog(context: Context, record: AiUsageLogRecord) {
        val current = getUsageLogs(context).toMutableList()
        current.add(0, record) // newest first
        val trimmed = current.take(MAX_STORED_USAGE_LOGS)
        saveUsageLogsInternal(context, trimmed)
        notifyConfigChanged()
    }

    @Synchronized
    fun deleteUsageLogById(context: Context, logId: String) {
        val updated = getUsageLogs(context).filterNot { it.id == logId }
        saveUsageLogsInternal(context, updated)
        notifyConfigChanged()
    }

    @Synchronized
    fun clearAllUsageLogs(context: Context) {
        prefs(context).edit().remove(KEY_AI_USAGE_LOGS_JSON).apply()
        notifyConfigChanged()
    }

    private fun saveUsageLogsInternal(context: Context, logs: List<AiUsageLogRecord>) {
        val arr = JSONArray()
        for (item in logs) {
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("timestampMs", item.timestampMs)
                    put("modelName", item.modelName)
                    put("provider", item.provider)
                    put("source", item.source)
                    put("promptText", item.promptText.take(4000))
                    put("systemPromptSnippet", "")
                    put("responseText", item.responseText.take(6000))
                    put("success", item.success)
                    put("promptTokens", item.promptTokens)
                    put("completionTokens", item.completionTokens)
                    put("totalTokens", item.totalTokens)
                    put("timeToFirstTokenMs", item.timeToFirstTokenMs)
                    put("totalLatencyMs", item.totalLatencyMs)
                    put("tokensPerSecond", item.tokensPerSecond)
                    put("fallbackAttemptsCount", item.fallbackAttemptsCount)
                    put("fallbackModelsTried", item.fallbackModelsTried)
                    put("reliabilityScoreAfter", item.reliabilityScoreAfter)
                    put("hasVisionAttachment", item.hasVisionAttachment)
                }
            )
        }
        prefs(context).edit().putString(KEY_AI_USAGE_LOGS_JSON, arr.toString()).apply()
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // UNIFIED AI GENERATION WITH RELIABILITY-SORTED FALLBACK CHAIN
    // ══════════════════════════════════════════════════════════════════════════════

    suspend fun generateTextOrVision(
        context: Context,
        prompt: String,
        systemInstruction: String? = null,
        imageBitmap: Bitmap? = null,
        temperature: Float = 0.28f,
        source: String? = null,
        userDisplayPrompt: String? = null
    ): AiGenerationOutcome = withContext(Dispatchers.IO) {
        val resolvedSource = source?.takeIf { it.isNotBlank() } ?: inferSourceFromPrompts(systemInstruction, prompt)
        val cleanUserQuestion = extractCleanUserQuestion(prompt, userDisplayPrompt)
        val userOnlyPromptTokens = estimateTokenCount(cleanUserQuestion)
        val fullCombinedPrompt = if (!systemInstruction.isNullOrBlank()) {
            "$systemInstruction\n\n$prompt"
        } else {
            prompt
        }
        val triedModelsTrail = mutableListOf<String>()
        val userKey = getUserGeminiApiKey(context)

        // Build the ordered Fallback Chain from this user's/device's live adaptive reliability table.
        // Strictly respects disabled providers & disabled models from RNA AI Settings.
        val allStates = getAllModelsWithRuntimeState(context)
        val enabledStates = allStates.filter { it.enabled }

        val orderedWithPreferredGemini = if (isUsingUserGeminiKey(context) && userKey.isNotBlank()) {
            val preferredId = getSelectedGeminiModel(context)
            val preferredState = enabledStates.firstOrNull { it.spec.id.equals(preferredId, ignoreCase = true) }
            if (preferredState != null) {
                listOf(preferredState) + enabledStates.filterNot { it.spec.id.equals(preferredId, ignoreCase = true) }
            } else {
                enabledStates
            }
        } else {
            enabledStates.filterNot { it.spec.provider == "gemini" }
        }

        val candidateChain = if (imageBitmap != null) {
            val visionFirst = orderedWithPreferredGemini.filter { it.spec.hasVision }
            val nonVisionFallback = orderedWithPreferredGemini.filterNot { it.spec.hasVision }
            visionFirst + nonVisionFallback
        } else {
            orderedWithPreferredGemini
        }

        if (candidateChain.isEmpty()) {
            val disabledMsg = "All AI providers or models are disabled in RNA AI Settings. Enable at least one provider."
            appendUsageLog(
                context = context,
                record = AiUsageLogRecord(
                    id = UUID.randomUUID().toString(),
                    timestampMs = System.currentTimeMillis(),
                    modelName = "No Enabled Provider",
                    provider = "none",
                    source = resolvedSource,
                    promptText = cleanUserQuestion,
                    systemPromptSnippet = "",
                    responseText = "ERROR: $disabledMsg",
                    success = false,
                    promptTokens = userOnlyPromptTokens,
                    completionTokens = 0,
                    totalTokens = userOnlyPromptTokens,
                    timeToFirstTokenMs = 0L,
                    totalLatencyMs = 0L,
                    tokensPerSecond = 0.0,
                    fallbackAttemptsCount = 0,
                    fallbackModelsTried = "",
                    reliabilityScoreAfter = 0,
                    hasVisionAttachment = imageBitmap != null
                )
            )
            throw IllegalStateException(disabledMsg)
        }

        var lastError: Exception? = null
        val providerFailCountInRequest = mutableMapOf<String, Int>()
        val rateLimitedProvidersInRequest = mutableSetOf<String>()
        val attemptedIds = mutableSetOf<String>()
        val maxPrimaryAttempts = 10

        suspend fun tryCandidateModel(modelState: FreeAiModelRuntimeState): AiGenerationOutcome? {
            val spec = modelState.spec
            val provKey = spec.provider.lowercase(Locale.US)
            attemptedIds.add(spec.id)
            triedModelsTrail.add("${spec.displayName} [${spec.provider}]")
            val startMs = System.currentTimeMillis()
            try {
                val rawRes = if (spec.provider == "gemini" && userKey.isNotBlank()) {
                    callUserGeminiApiWithAliasFallback(
                        apiKey = userKey,
                        spec = spec,
                        prompt = fullCombinedPrompt,
                        imageBitmap = imageBitmap,
                        temperature = temperature
                    )
                } else {
                    callFreeModelProvider(
                        context = context,
                        spec = spec,
                        systemPrompt = systemInstruction,
                        userPrompt = prompt,
                        combinedPrompt = fullCombinedPrompt,
                        imageBitmap = imageBitmap,
                        temperature = temperature
                    )
                }
                if (rawRes.text.isNotBlank()) {
                    val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                    val ttft = rawRes.timeToFirstTokenMs.coerceIn(1L, elapsed)
                    val newRel = recordModelSuccess(context, spec.id, elapsed)
                    val compTok = rawRes.completionTokens.coerceAtLeast(estimateTokenCount(rawRes.text))
                    val totalUserAndCompTok = userOnlyPromptTokens + compTok
                    val tokPerSec = if (elapsed > 0) (compTok * 1000.0) / elapsed else 0.0
                    _lastUsedModelSummary.value = "${spec.displayName} (${spec.provider} • ${newRel}% rel)"

                    appendUsageLog(
                        context = context,
                        record = AiUsageLogRecord(
                            id = UUID.randomUUID().toString(),
                            timestampMs = System.currentTimeMillis(),
                            modelName = spec.displayName,
                            provider = spec.provider,
                            source = resolvedSource,
                            promptText = cleanUserQuestion,
                            systemPromptSnippet = "",
                            responseText = rawRes.text.trim(),
                            success = true,
                            promptTokens = userOnlyPromptTokens,
                            completionTokens = compTok,
                            totalTokens = totalUserAndCompTok,
                            timeToFirstTokenMs = ttft,
                            totalLatencyMs = elapsed,
                            tokensPerSecond = tokPerSec,
                            fallbackAttemptsCount = triedModelsTrail.size,
                            fallbackModelsTried = triedModelsTrail.joinToString(" → "),
                            reliabilityScoreAfter = newRel,
                            hasVisionAttachment = imageBitmap != null
                        )
                    )

                    return AiGenerationOutcome(
                        text = rawRes.text.trim(),
                        modelUsedName = spec.displayName,
                        providerUsed = spec.provider,
                        reliabilityScore = newRel,
                        latencyMs = elapsed,
                        timeToFirstTokenMs = ttft,
                        promptTokens = userOnlyPromptTokens,
                        completionTokens = compTok,
                        totalTokens = totalUserAndCompTok
                    )
                }
                throw IllegalStateException("Empty completion response from ${spec.displayName}")
            } catch (e: Exception) {
                lastError = e
                val is429 = (e as? ProviderHttpException)?.isRateLimited == true ||
                    e.message.orEmpty().contains("HTTP 429")
                Log.w(TAG, "Model ${spec.displayName} [${spec.provider}] failed (429=$is429), failing over: ${e.message}")
                recordModelFailure(context, spec.id, isRateLimited = is429)
                providerFailCountInRequest[provKey] = (providerFailCountInRequest[provKey] ?: 0) + 1
                if (is429) {
                    rateLimitedProvidersInRequest.add(provKey)
                }
                return null
            }
        }

        // Pass 1: Walk ranked models with smart provider diversity — if a provider hits 429 or fails
        // twice consecutively during this request, jump immediately to the next provider (e.g., Kilo → OVH → AIHorde)
        // instead of wasting 30s hammering a rate-limited gateway!
        var attempts = 0
        for (modelState in candidateChain) {
            if (attempts >= maxPrimaryAttempts) break
            val provKey = modelState.spec.provider.lowercase(Locale.US)
            if (provKey in rateLimitedProvidersInRequest) continue
            if ((providerFailCountInRequest[provKey] ?: 0) >= 2 && provKey != "gemini") continue
            attempts++
            tryCandidateModel(modelState)?.let { return@withContext it }
        }

        // Pass 2: If Pass 1 skipped additional enabled candidates from another model on a temporarily errored
        // provider, give up to 2 remaining unattempted candidates a quick shot before emergency relay.
        for (modelState in candidateChain) {
            if (attempts >= maxPrimaryAttempts + 2) break
            if (modelState.spec.id in attemptedIds) continue
            val provKey = modelState.spec.provider.lowercase(Locale.US)
            if (provKey in rateLimitedProvidersInRequest) continue
            attempts++
            tryCandidateModel(modelState)?.let { return@withContext it }
        }

        // Pass 3: Final Pollinations.ai Backup Safety Net — ONLY runs if the user has NOT disabled Pollinations.ai!
        if (isProviderEnabled(context, "pollinations")) {
            val pollinationsBackupSpec = FREE_AI_CATALOG.firstOrNull { it.id == "pollinations-openai" }
                ?: candidateChain.first().spec
            val backupStartMs = System.currentTimeMillis()
            try {
                triedModelsTrail.add("${pollinationsBackupSpec.displayName} [pollinations]")
                val relayRes = callPollinationsEndpoint(
                    spec = pollinationsBackupSpec,
                    systemPrompt = systemInstruction,
                    userPrompt = prompt,
                    imageBitmap = imageBitmap,
                    temperature = temperature
                )
                if (relayRes.text.isNotBlank()) {
                    val elapsed = (System.currentTimeMillis() - backupStartMs).coerceAtLeast(1L)
                    val ttft = relayRes.timeToFirstTokenMs.coerceIn(1L, elapsed)
                    val newRel = recordModelSuccess(context, pollinationsBackupSpec.id, elapsed)
                    val compTok = relayRes.completionTokens.coerceAtLeast(estimateTokenCount(relayRes.text))
                    val totalUserAndCompTok = userOnlyPromptTokens + compTok
                    val tokPerSec = if (elapsed > 0) (compTok * 1000.0) / elapsed else 0.0
                    _lastUsedModelSummary.value = "${pollinationsBackupSpec.displayName} (pollinations • ${newRel}% rel)"

                    appendUsageLog(
                        context = context,
                        record = AiUsageLogRecord(
                            id = UUID.randomUUID().toString(),
                            timestampMs = System.currentTimeMillis(),
                            modelName = pollinationsBackupSpec.displayName,
                            provider = "pollinations",
                            source = resolvedSource,
                            promptText = cleanUserQuestion,
                            systemPromptSnippet = "",
                            responseText = relayRes.text.trim(),
                            success = true,
                            promptTokens = userOnlyPromptTokens,
                            completionTokens = compTok,
                            totalTokens = totalUserAndCompTok,
                            timeToFirstTokenMs = ttft,
                            totalLatencyMs = elapsed,
                            tokensPerSecond = tokPerSec,
                            fallbackAttemptsCount = triedModelsTrail.size,
                            fallbackModelsTried = triedModelsTrail.joinToString(" → "),
                            reliabilityScoreAfter = newRel,
                            hasVisionAttachment = imageBitmap != null
                        )
                    )

                    return@withContext AiGenerationOutcome(
                        text = relayRes.text.trim(),
                        modelUsedName = pollinationsBackupSpec.displayName,
                        providerUsed = "pollinations",
                        reliabilityScore = newRel,
                        latencyMs = elapsed,
                        timeToFirstTokenMs = ttft,
                        promptTokens = userOnlyPromptTokens,
                        completionTokens = compTok,
                        totalTokens = totalUserAndCompTok
                    )
                }
            } catch (e: Exception) {
                lastError = e
                recordModelFailure(context, pollinationsBackupSpec.id, isRateLimited = (e as? ProviderHttpException)?.isRateLimited == true)
                Log.w(TAG, "Pollinations backup pass also failed: ${e.message}")
            }
        }

        val failMsg = lastError?.localizedMessage
            ?: "All AI models in the fallback chain are temporarily busy."
        appendUsageLog(
            context = context,
            record = AiUsageLogRecord(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                modelName = candidateChain.firstOrNull()?.spec?.displayName ?: "Fallback Chain",
                provider = candidateChain.firstOrNull()?.spec?.provider ?: "free",
                source = resolvedSource,
                promptText = cleanUserQuestion,
                systemPromptSnippet = "",
                responseText = "ERROR: $failMsg",
                success = false,
                promptTokens = userOnlyPromptTokens,
                completionTokens = 0,
                totalTokens = userOnlyPromptTokens,
                timeToFirstTokenMs = 0L,
                totalLatencyMs = 0L,
                tokensPerSecond = 0.0,
                fallbackAttemptsCount = triedModelsTrail.size,
                fallbackModelsTried = triedModelsTrail.joinToString(" → "),
                reliabilityScoreAfter = candidateChain.firstOrNull()?.effectiveReliability ?: 50,
                hasVisionAttachment = imageBitmap != null
            )
        )

        throw IllegalStateException(failMsg)
    }

    private fun callUserGeminiApiWithAliasFallback(
        apiKey: String,
        spec: FreeAiModelSpec,
        prompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        val candidateModelIds = listOf(
            spec.id,
            spec.openAiModelId,
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        ).distinct()
        var lastErr: Exception? = null
        for (candidateId in candidateModelIds) {
            try {
                return callUserGeminiApi(
                    apiKey = apiKey,
                    modelName = candidateId,
                    prompt = prompt,
                    imageBitmap = imageBitmap,
                    temperature = temperature
                )
            } catch (e: Exception) {
                lastErr = e
                val msg = e.message.orEmpty()
                // If the API key itself is invalid (400 API_KEY_INVALID / 403), break out to free models immediately
                if (msg.contains("API_KEY_INVALID", ignoreCase = true) || msg.contains("HTTP 403")) {
                    break
                }
            }
        }
        throw lastErr ?: IllegalStateException("Gemini API request failed for ${spec.displayName}")
    }

    /**
     * Dispatches strictly to each model's REAL provider so reliability metrics accurately reflect
     * real-world performance on the user's device without falsely crediting another relay.
     */
    private suspend fun callFreeModelProvider(
        context: Context,
        spec: FreeAiModelSpec,
        systemPrompt: String?,
        userPrompt: String,
        combinedPrompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        return when (spec.provider.lowercase(Locale.US)) {
            "kilo" -> callKiloGatewayEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
            "pollinations" -> callPollinationsEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
            "ovh" -> callOvhFreeEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
            "aihorde" -> callAiHordePublicCluster(spec, combinedPrompt)
            "2 providers" -> {
                val kiloOn = isProviderEnabled(context, "kilo")
                val hordeOn = isProviderEnabled(context, "aihorde")
                when {
                    kiloOn && hordeOn -> {
                        try {
                            callKiloGatewayEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
                        } catch (_: Exception) {
                            callAiHordePublicCluster(spec, combinedPrompt)
                        }
                    }
                    kiloOn -> callKiloGatewayEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
                    hordeOn -> callAiHordePublicCluster(spec, combinedPrompt)
                    else -> throw IllegalStateException("Both Kilo Gateway and AI Horde are disabled")
                }
            }
            else -> callKiloGatewayEndpoint(spec, systemPrompt, userPrompt, imageBitmap, temperature)
        }
    }

    /**
     * Fetches active zero-cost (`:free` / `kilo-auto/free`) model IDs from Kilo Gateway's public
     * `/models` endpoint (cached for 30 minutes) so rotated model slugs always resolve cleanly.
     */
    private fun getCachedKiloFreeModelIds(): List<String> {
        val now = System.currentTimeMillis()
        if (cachedKiloFreeModelIds.isNotEmpty() && (now - cachedKiloFetchedAtMs) < LIVE_CATALOG_TTL_MS) {
            return cachedKiloFreeModelIds
        }
        val endpoints = listOf(
            "https://api.kilo.ai/api/gateway/models",
            "https://ai-gateway.kilo.ai/api/v1/models"
        )
        for (urlStr in endpoints) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3_500
                    readTimeout = 4_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer anonymous")
                    setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
                }
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val data = JSONObject(raw).optJSONArray("data")
                    if (data != null) {
                        val freeIds = mutableListOf<String>()
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i) ?: continue
                            val id = item.optString("id", "").trim()
                            if (id.isEmpty()) continue
                            val pricing = item.optJSONObject("pricing")
                            val promptPrice = pricing?.optString("prompt", "")?.trim()
                            val isZeroCost = promptPrice == "0" || promptPrice == "0.0" || promptPrice == "0.00"
                            if (id.contains(":free", ignoreCase = true) ||
                                id.equals("kilo-auto/free", ignoreCase = true) ||
                                id.endsWith("/free", ignoreCase = true) ||
                                isZeroCost
                            ) {
                                freeIds.add(id)
                            }
                        }
                        if (freeIds.isNotEmpty()) {
                            cachedKiloFreeModelIds = freeIds.distinct()
                            cachedKiloFetchedAtMs = now
                            return cachedKiloFreeModelIds
                        }
                    }
                }
            } catch (_: Exception) {
                // Non-fatal: fall back to spec.openAiModelId and kilo-auto/free
            }
        }
        cachedKiloFetchedAtMs = now
        return cachedKiloFreeModelIds
    }

    private fun resolveKiloModelCandidates(spec: FreeAiModelSpec): List<String> {
        val liveFreeIds = getCachedKiloFreeModelIds()
        val candidates = mutableListOf<String>()
        if (spec.id.equals("kilo-auto", ignoreCase = true) || spec.id.equals("free-router", ignoreCase = true)) {
            candidates.add("kilo-auto/free")
        }
        if (liveFreeIds.isNotEmpty()) {
            val normalizedSpecId = spec.id.lowercase(Locale.US).replace("-", "").replace(".", "")
            val normalizedName = spec.displayName.lowercase(Locale.US).replace(" ", "").replace("-", "").replace(".", "")
            val exactLive = liveFreeIds.firstOrNull { live ->
                val clean = live.lowercase(Locale.US).replace("-", "").replace(".", "").replace("/", "").replace(":", "")
                clean.contains(normalizedSpecId) || clean.contains(normalizedName)
            }
            if (exactLive != null) candidates.add(exactLive)
            val keywordToken = when {
                spec.id.contains("dots", ignoreCase = true) -> "dots"
                spec.id.contains("ultra", ignoreCase = true) -> "ultra"
                spec.id.contains("lightning", ignoreCase = true) -> "lightning"
                spec.id.contains("super-vl", ignoreCase = true) -> "super"
                spec.id.contains("nano", ignoreCase = true) -> "nano"
                spec.id.contains("code", ignoreCase = true) -> "coder"
                else -> null
            }
            if (keywordToken != null) {
                liveFreeIds.firstOrNull { it.contains(keywordToken, ignoreCase = true) }?.let { candidates.add(it) }
            }
        }
        if (spec.openAiModelId.isNotBlank()) {
            candidates.add(spec.openAiModelId)
        }
        candidates.add("kilo-auto/free")
        liveFreeIds.firstOrNull()?.let { candidates.add(it) }
        return candidates.distinct().take(3)
    }

    /**
     * Real Kilo AI Gateway client (OpenAI-compatible anonymous free tier).
     * Sends `Authorization: Bearer anonymous` as required by Kilo Gateway's keyless free tier.
     */
    private fun callKiloGatewayEndpoint(
        spec: FreeAiModelSpec,
        systemPrompt: String?,
        userPrompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        val endpoints = listOf(
            "https://api.kilo.ai/api/gateway/chat/completions",
            "https://ai-gateway.kilo.ai/api/v1/chat/completions"
        )
        val modelCandidates = resolveKiloModelCandidates(spec)
        val messages = buildOpenAiMessagesArray(
            spec = spec,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBitmap = if (spec.hasVision) imageBitmap else null
        )

        var lastErr: Exception? = null
        for (modelId in modelCandidates) {
            for (endpoint in endpoints) {
                val reqStartMs = System.currentTimeMillis()
                try {
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 6_000
                        readTimeout = 18_000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Authorization", "Bearer anonymous")
                        setRequestProperty("HTTP-Referer", "https://rnatv.app")
                        setRequestProperty("X-Title", "RNA TV")
                        setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
                    }

                    val payload = JSONObject().apply {
                        put("model", modelId)
                        put("messages", messages)
                        put("temperature", temperature.toDouble())
                        put("max_tokens", 1400)
                    }

                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload.toString())
                        writer.flush()
                    }

                    val code = conn.responseCode
                    val ttftMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
                    if (code !in 200..299) {
                        val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().take(280)
                        val ex = ProviderHttpException("kilo", code, "Kilo Gateway HTTP $code ($modelId): $err")
                        lastErr = ex
                        if (ex.isRateLimited) throw ex
                        if (ex.isModelNotFound) break
                        continue
                    }

                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    return extractOpenAiChatCompletionResult(
                        rawBody = body,
                        ttftMs = ttftMs,
                        fallbackPromptText = "${systemPrompt.orEmpty()}\n$userPrompt",
                        hasImage = imageBitmap != null && spec.hasVision
                    )
                } catch (e: ProviderHttpException) {
                    if (e.isRateLimited) throw e
                    lastErr = e
                } catch (e: Exception) {
                    lastErr = e
                }
            }
        }
        throw lastErr ?: IllegalStateException("Kilo Gateway request failed for ${spec.displayName}")
    }

    /**
     * Fetches live active case-sensitive chat model IDs from OVHcloud AI Endpoints `/v1/models`
     * (cached for 30 minutes) so rotated model names always resolve accurately.
     */
    private fun getCachedOvhModelIds(): List<String> {
        val now = System.currentTimeMillis()
        if (cachedOvhModelIds.isNotEmpty() && (now - cachedOvhFetchedAtMs) < LIVE_CATALOG_TTL_MS) {
            return cachedOvhModelIds
        }
        try {
            val conn = (URL("https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_500
                readTimeout = 4_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val data = JSONObject(raw).optJSONArray("data")
                if (data != null) {
                    val ids = mutableListOf<String>()
                    for (i in 0 until data.length()) {
                        val id = data.optJSONObject(i)?.optString("id", "")?.trim().orEmpty()
                        val lower = id.lowercase(Locale.US)
                        val isNonChat = lower.contains("whisper") || lower.contains("embed") ||
                            lower.contains("bge") || lower.contains("rerank") ||
                            lower.contains("audio") || lower.contains("tts") || lower.contains("asr")
                        if (id.isNotEmpty() && !isNonChat) {
                            ids.add(id)
                        }
                    }
                    if (ids.isNotEmpty()) {
                        cachedOvhModelIds = ids.distinct()
                        cachedOvhFetchedAtMs = now
                        return cachedOvhModelIds
                    }
                }
            }
        } catch (_: Exception) {
            // Non-fatal: fall back to spec.openAiModelId
        }
        cachedOvhFetchedAtMs = now
        return cachedOvhModelIds
    }

    private fun resolveOvhModelCandidates(spec: FreeAiModelSpec): List<String> {
        val liveIds = getCachedOvhModelIds()
        val candidates = mutableListOf<String>()
        if (liveIds.isNotEmpty()) {
            liveIds.firstOrNull { it.equals(spec.openAiModelId, ignoreCase = true) }?.let { candidates.add(it) }
            val token = when {
                spec.id.contains("397b", ignoreCase = true) -> "397b"
                spec.id.contains("27b", ignoreCase = true) -> "27b"
                spec.id.contains("9b", ignoreCase = true) -> "9b"
                spec.id.contains("llama-3.3", ignoreCase = true) -> "llama-3_3"
                spec.id.contains("mistral", ignoreCase = true) -> "mistral"
                spec.id.contains("vl-72b", ignoreCase = true) -> "vl-72b"
                spec.id.contains("120b", ignoreCase = true) -> "120b"
                spec.id.contains("20b", ignoreCase = true) -> "20b"
                spec.id.contains("guard-gen-8b", ignoreCase = true) -> "guard-gen-8b"
                spec.id.contains("guard-gen-0.6b", ignoreCase = true) -> "guard-gen-0.6b"
                else -> spec.id.substringBefore("-")
            }
            liveIds.firstOrNull { it.contains(token, ignoreCase = true) }?.let { candidates.add(it) }
        }
        if (spec.openAiModelId.isNotBlank()) {
            candidates.add(spec.openAiModelId)
        }
        // If spec's exact model rotated out of OVH's live catalog, fall back to a live active OVH chat model
        liveIds.firstOrNull { !it.contains("guard", ignoreCase = true) }?.let { candidates.add(it) }
        return candidates.distinct().take(2)
    }

    private fun callOvhFreeEndpoint(
        spec: FreeAiModelSpec,
        systemPrompt: String?,
        userPrompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        val endpoint = "https://oai.endpoints.kepler.ai.cloud.ovh.net/v1/chat/completions"
        val modelCandidates = resolveOvhModelCandidates(spec)
        val messages = buildOpenAiMessagesArray(
            spec = spec,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBitmap = if (spec.hasVision) imageBitmap else null
        )

        var lastErr: Exception? = null
        for (resolvedModelId in modelCandidates) {
            val reqStartMs = System.currentTimeMillis()
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6_000
                    readTimeout = 18_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
                }

                val payload = JSONObject().apply {
                    put("model", resolvedModelId)
                    put("messages", messages)
                    put("temperature", temperature.toDouble())
                    put("max_tokens", 1400)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                val ttftMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().take(280)
                    val ex = ProviderHttpException("ovh", code, "OVH HTTP $code ($resolvedModelId): $err")
                    lastErr = ex
                    if (ex.isRateLimited) throw ex
                    if (ex.isModelNotFound) continue
                    throw ex
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                return extractOpenAiChatCompletionResult(
                    rawBody = body,
                    ttftMs = ttftMs,
                    fallbackPromptText = "${systemPrompt.orEmpty()}\n$userPrompt",
                    hasImage = imageBitmap != null && spec.hasVision
                )
            } catch (e: ProviderHttpException) {
                if (e.isRateLimited) throw e
                lastErr = e
            } catch (e: Exception) {
                lastErr = e
            }
        }
        throw lastErr ?: IllegalStateException("OVH request failed for ${spec.displayName}")
    }

    /**
     * Fetches live active keyless models from Pollinations.ai (`/models`, cached 30 min)
     * so rotated Pollinations model names always resolve accurately.
     */
    private fun getCachedPollinationsModelIds(): List<String> {
        val now = System.currentTimeMillis()
        if (cachedPollinationsModelIds.isNotEmpty() && (now - cachedPollinationsFetchedAtMs) < LIVE_CATALOG_TTL_MS) {
            return cachedPollinationsModelIds
        }
        try {
            val conn = (URL("https://text.pollinations.ai/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_500
                readTimeout = 4_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                val arr = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw).optJSONArray("data")
                if (arr != null) {
                    val names = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val name = obj.optString("name", obj.optString("id", "")).trim()
                        if (name.isNotEmpty()) names.add(name)
                    }
                    if (names.isNotEmpty()) {
                        cachedPollinationsModelIds = names.distinct()
                        cachedPollinationsFetchedAtMs = now
                        return cachedPollinationsModelIds
                    }
                }
            }
        } catch (_: Exception) {
            // Non-fatal: fall back to spec.openAiModelId and "openai"
        }
        cachedPollinationsFetchedAtMs = now
        return cachedPollinationsModelIds
    }

    private fun resolvePollinationsModelCandidates(spec: FreeAiModelSpec, hasImage: Boolean): List<String> {
        val liveIds = getCachedPollinationsModelIds()
        val candidates = mutableListOf<String>()
        if (hasImage && spec.hasVision) {
            candidates.add(if (spec.openAiModelId.contains("openai", ignoreCase = true)) spec.openAiModelId else "openai")
        } else {
            if (liveIds.isNotEmpty()) {
                liveIds.firstOrNull { it.equals(spec.openAiModelId, ignoreCase = true) }?.let { candidates.add(it) }
                val token = spec.openAiModelId.substringBefore("-").lowercase(Locale.US)
                if (token.length >= 3) {
                    liveIds.firstOrNull { it.contains(token, ignoreCase = true) }?.let { candidates.add(it) }
                }
            }
            if (spec.openAiModelId.isNotBlank()) {
                candidates.add(spec.openAiModelId)
            }
        }
        candidates.add("openai")
        return candidates.distinct().take(2)
    }

    /**
     * Real Pollinations.ai keyless multi-model client.
     * Calls `https://text.pollinations.ai/openai` (OpenAI-compatible POST) with model fallback
     * and compact direct-GET fallback for text-only queries if POST is transiently busy.
     */
    private fun callPollinationsEndpoint(
        spec: FreeAiModelSpec,
        systemPrompt: String?,
        userPrompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        val endpoint = "https://text.pollinations.ai/openai"
        val hasImage = imageBitmap != null && spec.hasVision
        val modelCandidates = resolvePollinationsModelCandidates(spec, hasImage)
        val messages = buildOpenAiMessagesArray(
            spec = spec,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            imageBitmap = if (hasImage) imageBitmap else null
        )

        var lastErr: Exception? = null
        for (modelId in modelCandidates) {
            val reqStartMs = System.currentTimeMillis()
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6_000
                    readTimeout = 18_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8 (${spec.id})")
                }

                val payload = JSONObject().apply {
                    put("model", modelId)
                    put("messages", messages)
                    put("temperature", temperature.toDouble())
                    put("private", true)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                val ttftMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
                if (code !in 200..299) {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().take(280)
                    val ex = ProviderHttpException("pollinations", code, "Pollinations HTTP $code ($modelId): $err")
                    lastErr = ex
                    if (ex.isRateLimited) throw ex
                    if (ex.isModelNotFound) continue
                    continue
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                return extractOpenAiChatCompletionResult(
                    rawBody = body,
                    ttftMs = ttftMs,
                    fallbackPromptText = "${systemPrompt.orEmpty()}\n$userPrompt",
                    hasImage = hasImage
                )
            } catch (e: ProviderHttpException) {
                if (e.isRateLimited) throw e
                lastErr = e
            } catch (e: Exception) {
                lastErr = e
            }
        }

        // Compact text-only direct GET fallback to Pollinations simple endpoint if POST failed
        val compactQuery = if (systemPrompt.isNullOrBlank()) userPrompt else "$systemPrompt\n\n$userPrompt"
        if (!hasImage && compactQuery.length <= 1400) {
            val reqStartMs = System.currentTimeMillis()
            try {
                val encoded = URLEncoder.encode(compactQuery, "UTF-8").replace("+", "%20")
                val fallbackModel = modelCandidates.firstOrNull() ?: "openai"
                val getUrl = "https://text.pollinations.ai/$encoded?model=${URLEncoder.encode(fallbackModel, "UTF-8")}&private=true"
                val conn = (URL(getUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6_000
                    readTimeout = 16_000
                    setRequestProperty("User-Agent", "RNA-TV-Launcher/3.8")
                }
                val code = conn.responseCode
                val ttftMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    return extractOpenAiChatCompletionResult(
                        rawBody = body,
                        ttftMs = ttftMs,
                        fallbackPromptText = compactQuery,
                        hasImage = false
                    )
                }
            } catch (e: Exception) {
                lastErr = e
            }
        }

        throw lastErr ?: IllegalStateException("Pollinations request failed for ${spec.displayName}")
    }

    /**
     * Fetches live online text worker models from AI Horde (`status/models?type=text`, cached 30 min)
     * so we target real online workers instead of stalling in empty queues.
     */
    private fun getCachedAiHordeOnlineModels(): List<String> {
        val now = System.currentTimeMillis()
        if (cachedAiHordeOnlineModels.isNotEmpty() && (now - cachedAiHordeFetchedAtMs) < LIVE_CATALOG_TTL_MS) {
            return cachedAiHordeOnlineModels
        }
        try {
            val conn = (URL("https://aihorde.net/api/v2/status/models?type=text").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3_500
                readTimeout = 4_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Client-Agent", "RNA-TV:3.8:android")
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(raw)
                val pairs = mutableListOf<Pair<String, Int>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    val count = obj.optInt("count", 0)
                    if (name.isNotEmpty() && count > 0) {
                        pairs.add(name to count)
                    }
                }
                if (pairs.isNotEmpty()) {
                    cachedAiHordeOnlineModels = pairs.sortedByDescending { it.second }.map { it.first }
                    cachedAiHordeFetchedAtMs = now
                    return cachedAiHordeOnlineModels
                }
            }
        } catch (_: Exception) {
            // Non-fatal: AI Horde accepts requests without explicit model filter to use any active worker
        }
        cachedAiHordeFetchedAtMs = now
        return cachedAiHordeOnlineModels
    }

    private fun clampPromptForAiHorde(combinedPrompt: String): String {
        val maxChars = 6_000
        if (combinedPrompt.length <= maxChars) return combinedPrompt
        val head = combinedPrompt.take(2_400)
        val tail = combinedPrompt.takeLast(3_400)
        return "$head\n...\n$tail"
    }

    private suspend fun callAiHordePublicCluster(
        spec: FreeAiModelSpec,
        combinedPrompt: String
    ): RawProviderCompletionResult {
        val reqStartMs = System.currentTimeMillis()
        val safePrompt = clampPromptForAiHorde(combinedPrompt)
        val onlineModels = getCachedAiHordeOnlineModels()
        val normalizedSpec = spec.displayName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val normalizedId = spec.id.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val matchedWorkerModel = onlineModels.firstOrNull { online ->
            val clean = online.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
            clean.contains(normalizedSpec) || clean.contains(normalizedId) ||
                (normalizedId.length >= 6 && clean.contains(normalizedId.take(8)))
        }

        val asyncUrl = "https://aihorde.net/api/v2/generate/text/async"
        val conn = (URL(asyncUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6_000
            readTimeout = 8_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("apikey", "0000000000")
            setRequestProperty("Client-Agent", "RNA-TV:3.8:android")
        }

        val payload = JSONObject().apply {
            put("prompt", safePrompt)
            if (matchedWorkerModel != null) {
                put("models", JSONArray().put(matchedWorkerModel))
            }
            put("trusted_workers", false)
            put("slow_workers", true)
            put(
                "params",
                JSONObject().apply {
                    put("max_length", 384)
                    put("max_context_length", 4096)
                    put("temperature", 0.35)
                }
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = conn.responseCode
        val ackMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().take(240)
            throw ProviderHttpException("aihorde", code, "AIHorde HTTP $code: $err")
        }
        val initBody = conn.inputStream.bufferedReader().use { it.readText() }
        val jobId = JSONObject(initBody).optString("id")
        if (jobId.isBlank()) throw IllegalStateException("AIHorde missing job id")

        val statusUrl = "https://aihorde.net/api/v2/generate/text/status/$jobId"
        repeat(12) {
            delay(1_250L)
            val pollConn = (URL(statusUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Client-Agent", "RNA-TV:3.8:android")
            }
            if (pollConn.responseCode in 200..299) {
                val pollBody = pollConn.inputStream.bufferedReader().use { it.readText() }
                val pollJson = JSONObject(pollBody)
                if (pollJson.optBoolean("done", false)) {
                    val gens = pollJson.optJSONArray("generations")
                    val text = gens?.optJSONObject(0)?.optString("text", "")?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        val pTok = estimateTokenCount(safePrompt)
                        val cTok = estimateTokenCount(text)
                        return RawProviderCompletionResult(
                            text = text,
                            timeToFirstTokenMs = ackMs,
                            promptTokens = pTok,
                            completionTokens = cTok,
                            totalTokens = pTok + cTok
                        )
                    }
                }
                if (pollJson.optBoolean("faulted", false) || !pollJson.optBoolean("is_possible", true)) {
                    throw IllegalStateException("AIHorde unavailable/faulted (${spec.displayName})")
                }
            }
        }
        throw IllegalStateException("AIHorde queue timeout (${spec.displayName})")
    }

    private fun buildOpenAiMessagesArray(
        spec: FreeAiModelSpec,
        systemPrompt: String?,
        userPrompt: String,
        imageBitmap: Bitmap?
    ): JSONArray {
        val arr = JSONArray()
        if (!systemPrompt.isNullOrBlank()) {
            arr.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
            )
        }
        if (imageBitmap != null && spec.hasVision) {
            val b64 = encodeBitmapToBase64Jpeg(imageBitmap)
            val contentArr = JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("type", "text")
                        put("text", userPrompt)
                    }
                )
                put(
                    JSONObject().apply {
                        put("type", "image_url")
                        put(
                            "image_url",
                            JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$b64")
                            }
                        )
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", contentArr)
                }
            )
        } else {
            arr.put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                }
            )
        }
        return arr
    }

    private fun stripReasoningThinkBlocks(raw: String): String {
        val cleaned = raw.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        return cleaned.ifBlank { raw.replace("<think>", "").replace("</think>", "").trim() }
    }

    private fun extractOpenAiChatCompletionResult(
        rawBody: String,
        ttftMs: Long,
        fallbackPromptText: String,
        hasImage: Boolean
    ): RawProviderCompletionResult {
        val trimmed = rawBody.trim()
        var extractedText: String? = null
        var promptTok = 0
        var compTok = 0
        var totalTok = 0

        if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)
            val errObj = json.optJSONObject("error")
            if (errObj != null) {
                val errMsg = errObj.optString("message", errObj.toString()).take(240)
                throw IllegalStateException("Provider error: $errMsg")
            }
            val usage = json.optJSONObject("usage")
            if (usage != null) {
                promptTok = usage.optInt("prompt_tokens", 0)
                compTok = usage.optInt("completion_tokens", 0)
                totalTok = usage.optInt("total_tokens", 0)
            }
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val msg = first?.optJSONObject("message")
                val contentPartsArr = msg?.optJSONArray("content")
                val contentStr = if (contentPartsArr != null) {
                    buildString {
                        for (i in 0 until contentPartsArr.length()) {
                            val part = contentPartsArr.optJSONObject(i)
                            val partText = part?.optString("text", "")?.trim().orEmpty()
                            if (partText.isNotEmpty()) {
                                if (isNotEmpty()) append("\n")
                                append(partText)
                            }
                        }
                    }.trim()
                } else {
                    msg?.optString("content", "")?.trim().orEmpty()
                }
                if (contentStr.isNotBlank() && !contentStr.equals("null", ignoreCase = true)) {
                    extractedText = stripReasoningThinkBlocks(contentStr)
                } else {
                    val reasoning = msg?.optString("reasoning_content", "")?.trim()
                        ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                        ?: msg?.optString("reasoning", "")?.trim()
                            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                    if (!reasoning.isNullOrBlank()) {
                        extractedText = stripReasoningThinkBlocks(reasoning)
                    } else {
                        val text = first?.optString("text", "")?.trim()
                        if (!text.isNullOrBlank() && !text.equals("null", ignoreCase = true)) {
                            extractedText = stripReasoningThinkBlocks(text)
                        }
                    }
                }
            }
        }
        val isHtmlPage = trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)
        if (extractedText.isNullOrBlank() && trimmed.isNotEmpty() && !trimmed.startsWith("{") && !isHtmlPage) {
            extractedText = stripReasoningThinkBlocks(trimmed)
        }
        val finalText = extractedText?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty completion response")

        if (promptTok <= 0) {
            promptTok = estimateTokenCount(fallbackPromptText) + (if (hasImage) 258 else 0)
        }
        if (compTok <= 0) {
            compTok = estimateTokenCount(finalText)
        }
        if (totalTok <= 0) {
            totalTok = promptTok + compTok
        }
        return RawProviderCompletionResult(
            text = finalText,
            timeToFirstTokenMs = ttftMs,
            promptTokens = promptTok,
            completionTokens = compTok,
            totalTokens = totalTok
        )
    }

    private fun callUserGeminiApi(
        apiKey: String,
        modelName: String,
        prompt: String,
        imageBitmap: Bitmap?,
        temperature: Float
    ): RawProviderCompletionResult {
        val reqStartMs = System.currentTimeMillis()
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 18_000
            readTimeout = 28_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val partsArray = JSONArray().apply {
            if (imageBitmap != null) {
                val b64 = encodeBitmapToBase64Jpeg(imageBitmap)
                put(
                    JSONObject().apply {
                        put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", b64)
                            }
                        )
                    }
                )
            }
            put(
                JSONObject().apply {
                    put("text", prompt)
                }
            )
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("parts", partsArray)
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", temperature.toDouble())
                }
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val status = conn.responseCode
        val ttftMs = (System.currentTimeMillis() - reqStartMs).coerceAtLeast(1L)
        val body = if (status in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("Gemini API HTTP $status: $err")
        }

        val root = JSONObject(body)
        val usageMeta = root.optJSONObject("usageMetadata")
        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text", "")?.trim()
        if (!text.isNullOrBlank()) {
            val pTok = usageMeta?.optInt("promptTokenCount", 0)?.takeIf { it > 0 }
                ?: (estimateTokenCount(prompt) + (if (imageBitmap != null) 258 else 0))
            val cTok = usageMeta?.optInt("candidatesTokenCount", 0)?.takeIf { it > 0 }
                ?: estimateTokenCount(text)
            val totTok = usageMeta?.optInt("totalTokenCount", 0)?.takeIf { it > 0 }
                ?: (pTok + cTok)
            return RawProviderCompletionResult(
                text = text,
                timeToFirstTokenMs = ttftMs,
                promptTokens = pTok,
                completionTokens = cTok,
                totalTokens = totTok
            )
        }
        throw IllegalStateException("Empty response from Gemini ($modelName)")
    }

    private fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 960 || bitmap.height > 540) {
            Bitmap.createScaledBitmap(bitmap, 960, 540, true)
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 76, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // OVH WHISPER FREE AUDIO DICTATION SERVICE (OPTIONAL, DISABLED BY DEFAULT)
    // ══════════════════════════════════════════════════════════════════════════════

    /**
     * Records up to [maxDurationMs] of 16kHz mono PCM audio from the microphone (stopping early
     * on silence after speech starts) and transcribes it using the OVH Free Whisper endpoints:
     * - `Whisper Large v3 Turbo (OVH)`
     * - `Whisper Large v3 (OVH)`
     * Returns null if recording or OVH transcription fails so callers can seamlessly fall back to
     * Google `SpeechRecognizer`.
     */
    @SuppressLint("MissingPermission")
    suspend fun captureAndTranscribeWithOvhWhisper(
        context: Context,
        maxDurationMs: Long = 4500L,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String? = withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

        val pcmOut = ByteArrayOutputStream()
        var recorder: AudioRecord? = null
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBuf * 2
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext null
            }
            recorder.startRecording()
            onStatusUpdate?.invoke("Listening via OVH Whisper...")

            val buffer = ShortArray(1024)
            val startTime = System.currentTimeMillis()
            var speechDetected = false
            var silenceStartMs = 0L

            while (System.currentTimeMillis() - startTime < maxDurationMs) {
                val readCount = recorder.read(buffer, 0, buffer.size)
                if (readCount > 0) {
                    var sumSq = 0.0
                    val byteBuf = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readCount) {
                        val s = buffer[i]
                        byteBuf.putShort(s)
                        sumSq += (s.toDouble() * s.toDouble())
                    }
                    pcmOut.write(byteBuf.array())

                    val rms = kotlin.math.sqrt(sumSq / readCount)
                    if (rms > 650.0) {
                        speechDetected = true
                        silenceStartMs = 0L
                    } else if (speechDetected) {
                        if (silenceStartMs == 0L) {
                            silenceStartMs = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - silenceStartMs > 1150L) {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord capture for Whisper failed: ${e.message}")
            return@withContext null
        } finally {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
        }

        val pcmBytes = pcmOut.toByteArray()
        if (pcmBytes.size < 3200) return@withContext null

        val wavBytes = wrapPcmInWavHeader(pcmBytes, sampleRate, 1, 16)
        onStatusUpdate?.invoke("Transcribing with OVH Whisper...")

        val chosenOption = getSelectedWhisperModel(context)
        val endpointsToTry = when (chosenOption) {
            OvhWhisperModelOption.AUTO_CHAIN -> listOf(
                OvhWhisperModelOption.WHISPER_LARGE_V3_TURBO,
                OvhWhisperModelOption.WHISPER_LARGE_V3
            )
            OvhWhisperModelOption.WHISPER_LARGE_V3 -> listOf(
                OvhWhisperModelOption.WHISPER_LARGE_V3,
                OvhWhisperModelOption.WHISPER_LARGE_V3_TURBO
            )
            OvhWhisperModelOption.WHISPER_LARGE_V3_TURBO -> listOf(
                OvhWhisperModelOption.WHISPER_LARGE_V3_TURBO,
                OvhWhisperModelOption.WHISPER_LARGE_V3
            )
        }

        val triedWhisper = mutableListOf<String>()
        for ((idx, whisperSpec) in endpointsToTry.withIndex()) {
            triedWhisper.add(whisperSpec.displayName)
            val startMs = System.currentTimeMillis()
            try {
                val transcript = uploadWavToOvhWhisper(whisperSpec.endpointUrl, wavBytes)
                if (!transcript.isNullOrBlank()) {
                    val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
                    val cTok = estimateTokenCount(transcript)
                    appendUsageLog(
                        context = context,
                        record = AiUsageLogRecord(
                            id = UUID.randomUUID().toString(),
                            timestampMs = System.currentTimeMillis(),
                            modelName = whisperSpec.displayName,
                            provider = whisperSpec.provider,
                            source = "OVH Whisper Dictation",
                            promptText = "[Microphone Voice Audio Capture • ${pcmBytes.size / 1024} KB PCM]",
                            systemPromptSnippet = "OVH Free Audio Transcription (${chosenOption.displayName})",
                            responseText = transcript.trim(),
                            success = true,
                            promptTokens = (pcmBytes.size / 320).coerceAtLeast(1),
                            completionTokens = cTok,
                            totalTokens = (pcmBytes.size / 320).coerceAtLeast(1) + cTok,
                            timeToFirstTokenMs = (elapsed * 65L / 100L).coerceAtLeast(1L),
                            totalLatencyMs = elapsed,
                            tokensPerSecond = if (elapsed > 0) (cTok * 1000.0) / elapsed else 0.0,
                            fallbackAttemptsCount = idx + 1,
                            fallbackModelsTried = triedWhisper.joinToString(" → "),
                            reliabilityScoreAfter = 92,
                            hasVisionAttachment = false
                        )
                    )
                    return@withContext transcript.trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "OVH Whisper (${whisperSpec.displayName}) failed: ${e.message}")
            }
        }
        null
    }

    private fun uploadWavToOvhWhisper(endpointUrl: String, wavBytes: ByteArray): String? {
        val boundary = "----RnaWhisperBoundary${System.currentTimeMillis()}"
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 18_000
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        DataOutputStream(conn.outputStream).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"\r\n")
            out.writeBytes("Content-Type: audio/wav\r\n\r\n")
            out.write(wavBytes)
            out.writeBytes("\r\n")

            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n")
            out.writeBytes("json\r\n")
            out.writeBytes("--$boundary--\r\n")
            out.flush()
        }

        val status = conn.responseCode
        if (status !in 200..299) {
            return null
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        return json.optString("text", "").trim().takeIf { it.isNotEmpty() }
    }

    private fun wrapPcmInWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmData.size + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)

        val out = ByteArrayOutputStream(44 + pcmData.size)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }
}

/**
 * Material 3 Expressive Control Panel Section for AI Models, Reliability Fallback Chain,
 * User Google Gemini API Key, and Optional OVH Whisper Dictation.
 * Designed to fit seamlessly inside the Control Panel's `AI & System` tab without disrupting layout.
 */
@Composable
fun ControlPanelAiRouterSection(
    context: Context,
    modifier: Modifier = Modifier
) {
    val configVersion by RnaFreeAiRouter.configVersion.collectAsState()
    val lastUsedSummary by RnaFreeAiRouter.lastUsedModelSummary.collectAsState()

    var useUserGeminiKey by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.isUsingUserGeminiKey(context))
    }
    var userGeminiKeyInput by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.getUserGeminiApiKey(context))
    }
    var selectedGeminiModel by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.getSelectedGeminiModel(context))
    }
    var fallbackOrderMode by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.getFallbackOrderMode(context))
    }
    var whisperEnabled by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.isWhisperDictationEnabled(context))
    }
    var selectedWhisperModel by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.getSelectedWhisperModel(context))
    }
    var showFallbackManagerDialog by remember { mutableStateOf(false) }
    var showAiUsageInfoDialog by remember { mutableStateOf(false) }
    var showGeminiKeyEditor by remember { mutableStateOf(false) }

    val modelsSnapshot = remember(configVersion) {
        RnaFreeAiRouter.getAllModelsWithRuntimeState(context)
    }
    val disabledProviders = remember(configVersion) {
        RnaFreeAiRouter.getDisabledProviders(context)
    }
    val usageLogsCount = remember(configVersion) {
        RnaFreeAiRouter.getUsageLogs(context).size
    }
    val topThreePreview = remember(modelsSnapshot) {
        modelsSnapshot.filter { it.enabled }.take(3)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── 1. Expressive AI Engine & Reliability Fallback Card ──
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1A1E2E),
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFF34D399))
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_router_control_panel_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0EA5E9).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "AI Fallback Chain & Custom Gemini API Key",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            )
                            Text(
                                text = "Active: $lastUsedSummary",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prominent AI USAGE INFO Button right in the Control Panel header!
                        ExpressiveAiOptionChip(
                            title = "AI USAGE INFO ($usageLogsCount)",
                            selected = true,
                            accentColor = Color(0xFFF59E0B),
                            onClick = { showAiUsageInfoDialog = true },
                            testTag = "control_panel_ai_usage_info_btn"
                        )

                        // Mode Badge
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (useUserGeminiKey) Color(0xFF3B245A) else Color(0xFF0F2E26),
                            border = BorderStroke(
                                1.dp,
                                if (useUserGeminiKey) Color(0xFFC084FC) else Color(0xFF34D399)
                            )
                        ) {
                            Text(
                                text = if (useUserGeminiKey) "USER GEMINI KEY" else "FREE AI CHAIN",
                                color = if (useUserGeminiKey) Color(0xFFE9D5FF) else Color(0xFF6EE7B7),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Engine Source Selector Row: [Free AI Fallback Chain (44 Models)] vs [My Google Gemini API Key]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExpressiveAiOptionChip(
                        title = "Free AI Fallback Chain (${modelsSnapshot.count { it.enabled }} Active)",
                        selected = !useUserGeminiKey,
                        accentColor = Color(0xFF34D399),
                        onClick = {
                            useUserGeminiKey = false
                            RnaFreeAiRouter.setUsingUserGeminiKey(context, false)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_mode_free_chain_btn"
                    )

                    ExpressiveAiOptionChip(
                        title = if (userGeminiKeyInput.isNotBlank()) "My Gemini Key ($selectedGeminiModel)" else "Add My Gemini API Key",
                        selected = useUserGeminiKey || showGeminiKeyEditor,
                        accentColor = Color(0xFFC084FC),
                        onClick = {
                            showGeminiKeyEditor = !showGeminiKeyEditor
                            if (userGeminiKeyInput.isNotBlank()) {
                                useUserGeminiKey = true
                                RnaFreeAiRouter.setUsingUserGeminiKey(context, true)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_mode_user_gemini_btn"
                    )
                }

                // Expandable User Google Gemini API Key & Model Picker
                AnimatedVisibility(visible = showGeminiKeyEditor || useUserGeminiKey) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF121521),
                        border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = Color(0xFFC084FC),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Your Google Gemini API Key (Or submit via RNA Phone Controller dropdown)",
                                        color = Color(0xFFE9D5FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = userGeminiKeyInput,
                                    onValueChange = { userGeminiKeyInput = it },
                                    placeholder = {
                                        Text(
                                            "Paste AIzaSy... Google Gemini API key",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFC084FC),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_router_gemini_key_input")
                                )

                                ExpressiveAiOptionChip(
                                    title = "Save & Activate",
                                    selected = true,
                                    accentColor = Color(0xFFC084FC),
                                    onClick = {
                                        RnaFreeAiRouter.saveUserGeminiApiKey(
                                            context = context,
                                            apiKey = userGeminiKeyInput,
                                            autoActivate = userGeminiKeyInput.isNotBlank()
                                        )
                                        useUserGeminiKey = userGeminiKeyInput.isNotBlank()
                                        Toast.makeText(
                                            context,
                                            if (userGeminiKeyInput.isNotBlank()) "Custom Gemini API Key saved & activated!"
                                            else "Switched back to Free AI Fallback Chain",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    testTag = "ai_router_save_gemini_key_btn"
                                )

                                if (userGeminiKeyInput.isNotBlank()) {
                                    ExpressiveAiOptionChip(
                                        title = "Clear",
                                        selected = false,
                                        accentColor = Color(0xFFEF4444),
                                        onClick = {
                                            userGeminiKeyInput = ""
                                            useUserGeminiKey = false
                                            showGeminiKeyEditor = false
                                            RnaFreeAiRouter.saveUserGeminiApiKey(context, "", autoActivate = false)
                                        },
                                        testTag = "ai_router_clear_gemini_key_btn"
                                    )
                                }
                            }

                            Text(
                                text = "Choose Google Gemini Model:",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(RnaFreeAiRouter.AVAILABLE_GEMINI_MODELS) { _, modelOpt ->
                                    val isSelected = selectedGeminiModel == modelOpt.id
                                    ExpressiveAiOptionChip(
                                        title = "${modelOpt.displayName} (${modelOpt.badge})",
                                        selected = isSelected,
                                        accentColor = Color(0xFFC084FC),
                                        onClick = {
                                            selectedGeminiModel = modelOpt.id
                                            RnaFreeAiRouter.setSelectedGeminiModel(context, modelOpt.id)
                                        },
                                        testTag = "ai_router_gemini_model_${modelOpt.id}"
                                    )
                                }
                            }
                        }
                    }
                }

                // Fallback Sorting Mode + Manage All Free Models + AI USAGE INFO Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveAiOptionChip(
                        title = "Auto-Sort by Reliability",
                        selected = fallbackOrderMode == AiFallbackOrderMode.AUTO_RELIABILITY,
                        accentColor = Color(0xFF34D399),
                        onClick = {
                            fallbackOrderMode = AiFallbackOrderMode.AUTO_RELIABILITY
                            RnaFreeAiRouter.setFallbackOrderMode(context, AiFallbackOrderMode.AUTO_RELIABILITY)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_sort_reliability_btn"
                    )

                    ExpressiveAiOptionChip(
                        title = "Custom Fallback Order",
                        selected = fallbackOrderMode == AiFallbackOrderMode.CUSTOM_ORDER,
                        accentColor = Color(0xFF38BDF8),
                        onClick = {
                            fallbackOrderMode = AiFallbackOrderMode.CUSTOM_ORDER
                            RnaFreeAiRouter.setFallbackOrderMode(context, AiFallbackOrderMode.CUSTOM_ORDER)
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_sort_custom_btn"
                    )

                    ExpressiveAiOptionChip(
                        title = "Manage ${RnaFreeAiRouter.FREE_AI_CATALOG.size} Free Models",
                        selected = true,
                        accentColor = Color(0xFF38BDF8),
                        onClick = { showFallbackManagerDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_open_fallback_manager_btn"
                    )

                    ExpressiveAiOptionChip(
                        title = "AI USAGE INFO",
                        selected = true,
                        accentColor = Color(0xFFF59E0B),
                        onClick = { showAiUsageInfoDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_router_open_usage_info_btn"
                    )
                }

                // Provider Enable / Disable Controls (Toggle specific AI providers ON/OFF)
                AiProviderToggleBar(
                    context = context,
                    disabledProviders = disabledProviders
                )

                // Mini Top 3 Priority Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority Queue:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (topThreePreview.isEmpty()) {
                        Text(
                            text = "No active providers enabled",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        topThreePreview.forEachIndexed { idx, st ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF131724),
                                border = BorderStroke(1.dp, Color(0x28FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${st.spec.displayName}",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${st.effectiveReliability}%",
                                        color = Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showFallbackManagerDialog) {
        FreeAiFallbackManagerDialog(
            context = context,
            onDismiss = { showFallbackManagerDialog = false }
        )
    }

    if (showAiUsageInfoDialog) {
        AiUsageInfoDialog(
            context = context,
            onDismiss = { showAiUsageInfoDialog = false }
        )
    }
}

/**
 * Full-Screen D-Pad Navigable Two-Pane Sidebar Window for "AI USAGE INFO"
 * Styled just like the Profile Edit Menu & Control Panel (Left Sidebar with 2 Tabs + 'X' Close Button):
 * - Tab 0: Request History (Which AI model was called, when, for what question, source, tokens, TTFT;
 *          short press OK opens Full Detailed Telemetry Modal; press & hold OK deletes a specific item;
 *          Clear All button clears all history).
 * - Tab 1: Model Success Rates & Reliability Scores (Live success rate %, call counts, reliability score bar,
 *          and latency for each model).
 */
@Composable
fun AiUsageInfoDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val configVersion by RnaFreeAiRouter.configVersion.collectAsState()
    var selectedSidebarTab by remember { mutableIntStateOf(0) } // 0 = Request History, 1 = Model Reliability
    var selectedLogForDetail by remember { mutableStateOf<AiUsageLogRecord?>(null) }
    val firstSidebarTabFocus = remember { FocusRequester() }

    val usageLogs = remember(configVersion) {
        RnaFreeAiRouter.getUsageLogs(context)
    }
    val allModels = remember(configVersion) {
        RnaFreeAiRouter.getAllModelsWithRuntimeState(context)
    }

    val shortTimeFmt = remember { SimpleDateFormat("MMM dd • HH:mm:ss", Locale.US) }
    val fullTimeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstSidebarTabFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = {
            if (selectedLogForDetail != null) {
                selectedLogForDetail = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE8060811))
                .padding(horizontal = 34.dp, vertical = 22.dp)
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        if (selectedLogForDetail != null) {
                            selectedLogForDetail = null
                            true
                        } else {
                            onDismiss()
                            true
                        }
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF0E1322),
                border = BorderStroke(
                    width = 1.8.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFF59E0B))
                    )
                ),
                shadowElevation = 28.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ai_usage_info_dialog")
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // ══════════════════════════════════════════════════════════════════
                    // LEFT SIDEBAR (Profile Edit Menu Style: Title, 2 Tabs, Clear, 'X' Close)
                    // ══════════════════════════════════════════════════════════════════
                    Surface(
                        color = Color(0xFF13192B),
                        modifier = Modifier
                            .width(268.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Sidebar Brand Header with Top 'X' Close Icon
                                var topCloseFocused by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Analytics,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "AI USAGE INFO",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "LOGS & RELIABILITY",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.5.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (topCloseFocused) Color(0xFFEF4444) else Color(0xFF1E263B),
                                        border = BorderStroke(
                                            width = if (topCloseFocused) 2.dp else 1.dp,
                                            color = if (topCloseFocused) Color.White else Color(0x38FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .onFocusChanged { topCloseFocused = it.isFocused }
                                            .focusable()
                                            .clickable { onDismiss() }
                                            .testTag("ai_usage_sidebar_close_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close AI Usage Info",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0x24FFFFFF))

                                // Sidebar Tab 0: Request History
                                UsageSidebarTabButton(
                                    title = "Request History",
                                    subtitle = "${usageLogs.size} Logged Calls",
                                    icon = Icons.Default.History,
                                    accentColor = Color(0xFF38BDF8),
                                    selected = selectedSidebarTab == 0,
                                    onClick = { selectedSidebarTab = 0 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(firstSidebarTabFocus)
                                        .testTag("ai_usage_tab_history")
                                )

                                // Sidebar Tab 1: Model Success & Reliability
                                UsageSidebarTabButton(
                                    title = "Model Reliability",
                                    subtitle = "${allModels.size} Models & Rates",
                                    icon = Icons.Default.Verified,
                                    accentColor = Color(0xFF34D399),
                                    selected = selectedSidebarTab == 1,
                                    onClick = { selectedSidebarTab = 1 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_usage_tab_reliability")
                                )
                            }

                            // Bottom Sidebar Actions: Clear All History / Reset Scores
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider(color = Color(0x24FFFFFF))

                                if (selectedSidebarTab == 0 && usageLogs.isNotEmpty()) {
                                    UsageSidebarActionButton(
                                        label = "Clear All History",
                                        icon = Icons.Default.DeleteSweep,
                                        accentColor = Color(0xFFF59E0B),
                                        onClick = {
                                            RnaFreeAiRouter.clearAllUsageLogs(context)
                                            Toast.makeText(context, "Cleared all AI usage history", Toast.LENGTH_SHORT).show()
                                        },
                                        testTag = "ai_usage_sidebar_clear_all_btn"
                                    )
                                } else if (selectedSidebarTab == 1) {
                                    UsageSidebarActionButton(
                                        label = "Reset Scores",
                                        icon = Icons.Default.Refresh,
                                        accentColor = Color(0xFFF59E0B),
                                        onClick = {
                                            RnaFreeAiRouter.resetReliabilityMetricsAndOrder(context)
                                            Toast.makeText(context, "Reset model reliability scores to defaults", Toast.LENGTH_SHORT).show()
                                        },
                                        testTag = "ai_usage_sidebar_reset_scores_btn"
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0x26FFFFFF))
                    )

                    // ══════════════════════════════════════════════════════════════════
                    // RIGHT CONTENT PANE
                    // ══════════════════════════════════════════════════════════════════
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (selectedSidebarTab == 0) {
                            // ──────────────────────────────────────────────────────────
                            // TAB 0: AI REQUEST HISTORY
                            // ──────────────────────────────────────────────────────────
                            val totalTokensAll = remember(usageLogs) { usageLogs.sumOf { it.totalTokens } }
                            val avgTtftMs = remember(usageLogs) {
                                val valid = usageLogs.filter { it.timeToFirstTokenMs > 0L }
                                if (valid.isEmpty()) 0L else valid.sumOf { it.timeToFirstTokenMs } / valid.size
                            }

                            // Top Header & Telemetry Summary Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Request Call History",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "Press OK on any call for full details (Tokens, TTFT, Full Prompt & Reply) • Press & Hold OK to delete specific item",
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (usageLogs.isNotEmpty()) {
                                    ExpressiveAiOptionChip(
                                        title = "Clear All (${usageLogs.size})",
                                        selected = false,
                                        accentColor = Color(0xFFEF4444),
                                        onClick = {
                                            RnaFreeAiRouter.clearAllUsageLogs(context)
                                            Toast.makeText(context, "Cleared all AI usage history", Toast.LENGTH_SHORT).show()
                                        },
                                        testTag = "ai_usage_header_clear_all_btn"
                                    )
                                }
                            }

                            // Top 3 Telemetry Summary Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UsageStatPillCard(
                                    label = "Total AI Calls",
                                    value = "${usageLogs.size}",
                                    subvalue = "${usageLogs.count { it.success }} Succeeded",
                                    accent = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                                UsageStatPillCard(
                                    label = "Total Tokens Processed",
                                    value = "$totalTokensAll tok",
                                    subvalue = "Prompt + Completion",
                                    accent = Color(0xFFA855F7),
                                    modifier = Modifier.weight(1f)
                                )
                                UsageStatPillCard(
                                    label = "Avg Time to First Token",
                                    value = if (avgTtftMs > 0L) "${avgTtftMs} ms" else "—",
                                    subvalue = "TTFT Responsiveness",
                                    accent = Color(0xFF34D399),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (usageLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Text(
                                            text = "No AI requests logged yet",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = "Ask a question in RNA. AI, EdVanced Voice Assistant, TV Search, or Autonomous Agent to see live model telemetry here.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 14.dp)
                                ) {
                                    items(usageLogs, key = { it.id }) { logItem ->
                                        UsageHistoryRowCard(
                                            log = logItem,
                                            formattedTime = shortTimeFmt.format(Date(logItem.timestampMs)),
                                            onOpenDetails = { selectedLogForDetail = logItem },
                                            onDeleteThis = {
                                                RnaFreeAiRouter.deleteUsageLogById(context, logItem.id)
                                                Toast.makeText(
                                                    context,
                                                    "Deleted '${logItem.modelName}' call from history",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // ──────────────────────────────────────────────────────────
                            // TAB 1: AI MODEL SUCCESS RATES & RELIABILITY SCORES
                            // ──────────────────────────────────────────────────────────
                            val totalSuccessCalls = remember(allModels) { allModels.sumOf { it.successCount } }
                            val totalFailureCalls = remember(allModels) { allModels.sumOf { it.failureCount } }
                            val topModel = remember(allModels) { allModels.firstOrNull() }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Model Success Rates & Reliability Scores",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "Prefers Kilo Gateway by default, then adapts per user/device to prefer models with the highest real-use reliability.",
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                ExpressiveAiOptionChip(
                                    title = "Reset Scores",
                                    selected = false,
                                    accentColor = Color(0xFFF59E0B),
                                    onClick = {
                                        RnaFreeAiRouter.resetReliabilityMetricsAndOrder(context)
                                        Toast.makeText(context, "Model reliability scores reset to defaults", Toast.LENGTH_SHORT).show()
                                    },
                                    testTag = "ai_usage_reliability_reset_btn"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                UsageStatPillCard(
                                    label = "#1 Most Reliable Model",
                                    value = topModel?.spec?.displayName ?: "Kilo Auto",
                                    subvalue = "${topModel?.effectiveReliability ?: 92}% Reliability (${topModel?.spec?.provider ?: "kilo"} • ${if (topModel?.isDeviceLearned == true) "Device Learned" else "Kilo Default"})",
                                    accent = Color(0xFF34D399),
                                    modifier = Modifier.weight(1.2f)
                                )
                                UsageStatPillCard(
                                    label = "Recorded Model Successes",
                                    value = "$totalSuccessCalls OK",
                                    subvalue = "$totalFailureCalls Failover Switches",
                                    accent = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                                UsageStatPillCard(
                                    label = "Active Fallback Pool",
                                    value = "${allModels.count { it.enabled }} / ${allModels.size}",
                                    subvalue = "Per-Device Adaptive Ranking",
                                    accent = Color(0xFFA855F7),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Column Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF151C30), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp))
                                Text("Model Name & Provider", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.75f))
                                Text("Success Rate", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                                Text("Calls (OK / Fail)", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.95f))
                                Text("Reliability Score", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.15f))
                                Text("Avg Latency", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp))
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 14.dp)
                            ) {
                                itemsIndexed(allModels, key = { _, m -> m.spec.id }) { idx, state ->
                                    val totalCalls = state.successCount + state.failureCount
                                    val successRatePct = if (totalCalls > 0) {
                                        ((state.successCount * 100.0) / totalCalls).toInt()
                                    } else {
                                        state.effectiveReliability
                                    }
                                    var rowFocused by remember { mutableStateOf(false) }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (rowFocused) Color(0xFF1E2942) else Color(0xFF13192B),
                                        border = BorderStroke(
                                            width = if (rowFocused) 2.dp else 1.dp,
                                            color = if (rowFocused) Color(0xFF38BDF8) else Color(0x22FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { rowFocused = it.isFocused }
                                            .focusable()
                                            .testTag("ai_usage_reliability_row_${state.spec.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${idx + 1}",
                                                color = if (idx == 0) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.width(34.dp)
                                            )

                                            // Model Name + Provider + Adaptive Learning Status Badge
                                            Row(
                                                modifier = Modifier
                                                    .weight(1.75f)
                                                    .padding(end = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = state.spec.displayName,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                ModelTinyBadge(
                                                    text = state.spec.provider,
                                                    bg = Color(0xFF1E293B),
                                                    fg = Color(0xFF38BDF8)
                                                )
                                                if (state.isInCooldown) {
                                                    ModelTinyBadge(
                                                        text = "Cooldown",
                                                        bg = Color(0xFF3B1A1E),
                                                        fg = Color(0xFFF87171)
                                                    )
                                                } else if (state.isDeviceLearned) {
                                                    ModelTinyBadge(
                                                        text = "Learned",
                                                        bg = Color(0xFF0F2E26),
                                                        fg = Color(0xFF34D399)
                                                    )
                                                }
                                            }

                                            // Success Rate Bar & %
                                            MetricBarCell(
                                                score = successRatePct,
                                                barColor = if (totalCalls > 0) Color(0xFF38BDF8) else Color(0xFF64748B),
                                                modifier = Modifier.weight(1.1f)
                                            )

                                            // Calls Counter
                                            Text(
                                                text = if (totalCalls > 0) {
                                                    "${state.successCount} OK / ${state.failureCount} Fail"
                                                } else if (state.spec.provider == "kilo") {
                                                    "Kilo Default"
                                                } else {
                                                    "Standby (0)"
                                                },
                                                color = if (totalCalls > 0) Color(0xFFE2E8F0) else Color(0xFF64748B),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(0.95f)
                                            )

                                            // Live Reliability Score Bar
                                            MetricBarCell(
                                                score = state.effectiveReliability,
                                                barColor = if (state.isInCooldown) Color(0xFFF59E0B) else Color(0xFF22C55E),
                                                modifier = Modifier.weight(1.15f)
                                            )

                                            // Avg / Last Latency
                                            val shownLat = if (state.avgLatencyMs > 0L) state.avgLatencyMs else state.lastLatencyMs
                                            Text(
                                                text = if (shownLat > 0L) "${shownLat} ms" else "—",
                                                color = if (shownLat > 0L) Color(0xFFF59E0B) else Color(0xFF64748B),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.width(96.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════════════
            // FULL REQUEST DETAILS INSPECTION DIALOG (Opened when pressing OK on a request)
            // ══════════════════════════════════════════════════════════════════════
            if (selectedLogForDetail != null) {
                val detail = selectedLogForDetail!!
                val closeDetailFocus = remember { FocusRequester() }
                val detailScrollState = rememberScrollState()

                LaunchedEffect(detail.id) {
                    delay(50L)
                    runCatching { closeDetailFocus.requestFocus() }
                }

                Dialog(
                    onDismissRequest = { selectedLogForDetail = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xD904060C))
                            .padding(horizontal = 64.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF101628),
                            border = BorderStroke(2.dp, Color(0xFF38BDF8)),
                            shadowElevation = 32.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 640.dp)
                                .testTag("ai_usage_request_detail_modal")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(26.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Detail Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (detail.success) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (detail.success) Color(0xFF10B981) else Color(0xFFEF4444)
                                            ),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (detail.success) Color(0xFF10B981) else Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${detail.modelName}  •  [${detail.provider}]",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 19.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Source: ${detail.source}  •  Called at ${fullTimeFmt.format(Date(detail.timestampMs))}",
                                                color = Color(0xFF38BDF8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ExpressiveAiOptionChip(
                                            title = "Delete This Entry",
                                            selected = false,
                                            accentColor = Color(0xFFEF4444),
                                            onClick = {
                                                RnaFreeAiRouter.deleteUsageLogById(context, detail.id)
                                                selectedLogForDetail = null
                                                Toast.makeText(context, "Deleted request entry", Toast.LENGTH_SHORT).show()
                                            },
                                            testTag = "ai_usage_detail_delete_btn"
                                        )
                                        ExpressiveAiOptionChip(
                                            title = "Close Details",
                                            selected = true,
                                            accentColor = Color(0xFF38BDF8),
                                            onClick = { selectedLogForDetail = null },
                                            modifier = Modifier.focusRequester(closeDetailFocus),
                                            testTag = "ai_usage_detail_close_btn"
                                        )
                                    }
                                }

                                // Telemetry Grid (Tokens, Time to First Token, Total Latency, Throughput, Reliability)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UsageStatPillCard(
                                        label = "Time to First Token (TTFT)",
                                        value = "${detail.timeToFirstTokenMs} ms",
                                        subvalue = "Total Latency: ${detail.totalLatencyMs} ms",
                                        accent = Color(0xFF34D399),
                                        modifier = Modifier.weight(1f)
                                    )
                                    UsageStatPillCard(
                                        label = "Tokens Used",
                                        value = "${detail.totalTokens} total",
                                        subvalue = "${detail.promptTokens} prompt + ${detail.completionTokens} reply",
                                        accent = Color(0xFFA855F7),
                                        modifier = Modifier.weight(1f)
                                    )
                                    UsageStatPillCard(
                                        label = "Throughput & Chain",
                                        value = String.format(Locale.US, "%.1f tok/s", detail.tokensPerSecond),
                                        subvalue = "Attempt #${detail.fallbackAttemptsCount} • ${detail.reliabilityScoreAfter}% Rel",
                                        accent = Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Scrollable Full Prompt, Fallback Trail & Full Response
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .fillMaxWidth()
                                        .verticalScroll(detailScrollState),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (detail.fallbackModelsTried.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF161F36),
                                            border = BorderStroke(1.dp, Color(0x3338BDF8)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "FALLBACK ROUTER EXECUTION PATH",
                                                    color = Color(0xFF38BDF8),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = detail.fallbackModelsTried,
                                                    color = Color(0xFFE2E8F0),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF151C30),
                                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "FULL QUESTION / PROMPT SENT (Source: ${detail.source}${if (detail.hasVisionAttachment) " • +Vision Image" else ""})",
                                                color = Color(0xFFF59E0B),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = detail.promptText.ifBlank { "(Empty prompt)" },
                                                color = Color.White,
                                                fontSize = 13.5.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF121A2B),
                                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "FULL AI MODEL RESPONSE (${detail.completionTokens} tokens)",
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = detail.responseText.ifBlank { "(Empty response)" },
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 13.5.sp
                                            )
                                        }
                                    }
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
private fun UsageSidebarTabButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "usage_sidebar_tab_scale"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = when {
            focused -> accentColor
            selected -> accentColor.copy(alpha = 0.22f)
            else -> Color(0xFF192136)
        },
        border = BorderStroke(
            width = if (focused || selected) 2.dp else 1.dp,
            color = when {
                focused -> Color.White
                selected -> accentColor
                else -> Color(0x24FFFFFF)
            }
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (focused) Color(0xFF090D16) else accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (focused) Color(0xFF090D16) else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = if (focused) Color(0xFF1E293B) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UsageSidebarActionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (focused) accentColor else accentColor.copy(alpha = 0.16f),
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = if (focused) Color.White else accentColor.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (focused) Color(0xFF090D16) else accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (focused) Color(0xFF090D16) else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UsageStatPillCard(
    label: String,
    value: String,
    subvalue: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141B2E),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subvalue,
                color = accent,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Single-row compact card for an AI Usage Log entry:
 * - Fits strictly available width (`maxLines = 1, overflow = TextOverflow.Ellipsis` on model name & prompt)
 * - Short press (`OK` / `Enter` / Click) opens full telemetry detail modal
 * - Press & Hold (`OK` / `Enter` >= 420ms) deletes this specific entry immediately
 */
@Composable
private fun UsageHistoryRowCard(
    log: AiUsageLogRecord,
    formattedTime: String,
    onOpenDetails: () -> Unit,
    onDeleteThis: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var keyDownTime by remember { mutableLongStateOf(0L) }
    var longPressFired by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (focused) Color(0xFF1F2B46) else Color(0xFF13192B),
        border = BorderStroke(
            width = if (focused) 2.2.dp else 1.dp,
            color = if (focused) Color(0xFF38BDF8) else Color(0x22FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val code = event.nativeKeyEvent.keyCode
                val isCenter = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                    code == KeyEvent.KEYCODE_BUTTON_A
                if (code == KeyEvent.KEYCODE_DEL || code == KeyEvent.KEYCODE_FORWARD_DEL) {
                    if (event.type == KeyEventType.KeyDown) {
                        onDeleteThis()
                    }
                    return@onPreviewKeyEvent true
                }
                if (!isCenter) return@onPreviewKeyEvent false

                if (event.type == KeyEventType.KeyDown) {
                    if (event.nativeKeyEvent.repeatCount == 0) {
                        keyDownTime = System.currentTimeMillis()
                        longPressFired = false
                    } else if (!longPressFired && System.currentTimeMillis() - keyDownTime >= 420L) {
                        longPressFired = true
                        onDeleteThis()
                    }
                    true
                } else if (event.type == KeyEventType.KeyUp) {
                    if (!longPressFired) {
                        onOpenDetails()
                    }
                    longPressFired = false
                    true
                } else {
                    false
                }
            }
            .clickable { onOpenDetails() }
            .focusable()
            .testTag("ai_usage_history_row_${log.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (log.success) Color(0xFF34D399) else Color(0xFFEF4444))
            )

            // Left Column: Model Name + Provider + Source + Question Snippet (Truncated to fit cleanly)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log.modelName,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    ModelTinyBadge(
                        text = log.provider,
                        bg = Color(0xFF1E293B),
                        fg = Color(0xFF38BDF8)
                    )
                    ModelTinyBadge(
                        text = log.source,
                        bg = Color(0xFF2B1F47),
                        fg = Color(0xFFD8B4FE)
                    )
                    Text(
                        text = formattedTime,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Text(
                    text = "Q: ${log.promptText.replace("\n", " ").ifBlank { "(Voice / System Request)" }}",
                    color = if (focused) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Compact Telemetry Badges: Tokens + TTFT + Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModelTinyBadge(
                    text = "${log.totalTokens} tok",
                    bg = Color(0xFF1E1B3A),
                    fg = Color(0xFFC084FC)
                )
                ModelTinyBadge(
                    text = "TTFT ${log.timeToFirstTokenMs}ms",
                    bg = Color(0xFF0F2E26),
                    fg = Color(0xFF34D399)
                )
                SmallOrderMoveButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Entry",
                    onClick = onDeleteThis
                )
            }
        }
    }
}

/**
 * Full-Screen D-Pad Navigable Free AI Model Fallback & Reliability Table Dialog.
 * Matches the user's screenshots with `#`, `Model` (`kilo`, `ovh`, `aihorde`, `200/hr`, `512K ctx`, `Vision`, `Tools`),
 * green `Reliability` bar, blue `Speed` bar, purple `Intelligence` bar, `Score`, Custom Order Up/Down buttons,
 * `On` toggle switch, and `Search models...` filter bar.
 */
@Composable
fun FreeAiFallbackManagerDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val configVersion by RnaFreeAiRouter.configVersion.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var orderMode by remember(configVersion) {
        mutableStateOf(RnaFreeAiRouter.getFallbackOrderMode(context))
    }
    val allModels = remember(configVersion, orderMode) {
        RnaFreeAiRouter.getAllModelsWithRuntimeState(context)
    }
    val disabledProviders = remember(configVersion) {
        RnaFreeAiRouter.getDisabledProviders(context)
    }
    val filteredModels = remember(allModels, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) allModels
        else allModels.filter {
            it.spec.displayName.contains(q, ignoreCase = true) ||
                it.spec.provider.contains(q, ignoreCase = true) ||
                it.spec.contextBadge.contains(q, ignoreCase = true)
        }
    }
    val closeBtnFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80L)
        runCatching { closeBtnFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD905070D))
                .padding(horizontal = 32.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF0E111A),
                border = BorderStroke(1.5.dp, Color(0xFF283149)),
                shadowElevation = 28.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("free_ai_fallback_manager_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Free AI Models (${allModels.count { it.enabled }}/${allModels.size} Active)",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpressiveAiOptionChip(
                                title = "Auto-Sort by Reliability",
                                selected = orderMode == AiFallbackOrderMode.AUTO_RELIABILITY,
                                accentColor = Color(0xFF22C55E),
                                onClick = {
                                    orderMode = AiFallbackOrderMode.AUTO_RELIABILITY
                                    RnaFreeAiRouter.setFallbackOrderMode(context, AiFallbackOrderMode.AUTO_RELIABILITY)
                                }
                            )
                            ExpressiveAiOptionChip(
                                title = "Custom Order",
                                selected = orderMode == AiFallbackOrderMode.CUSTOM_ORDER,
                                accentColor = Color(0xFF38BDF8),
                                onClick = {
                                    orderMode = AiFallbackOrderMode.CUSTOM_ORDER
                                    RnaFreeAiRouter.setFallbackOrderMode(context, AiFallbackOrderMode.CUSTOM_ORDER)
                                }
                            )
                            ExpressiveAiOptionChip(
                                title = "Reset Scores",
                                selected = false,
                                accentColor = Color(0xFFF59E0B),
                                onClick = {
                                    RnaFreeAiRouter.resetReliabilityMetricsAndOrder(context)
                                    Toast.makeText(context, "Reliability scores & order reset to defaults", Toast.LENGTH_SHORT).show()
                                }
                            )
                            ExpressiveAiOptionChip(
                                title = "Close",
                                selected = false,
                                accentColor = Color(0xFFEF4444),
                                onClick = onDismiss,
                                modifier = Modifier.focusRequester(closeBtnFocus),
                                testTag = "free_ai_fallback_dialog_close_btn"
                            )
                        }
                    }

                    // Provider Enable / Disable Bar
                    AiProviderToggleBar(
                        context = context,
                        disabledProviders = disabledProviders
                    )

                    // Search Bar Row ("Search models...")
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        placeholder = {
                            Text("Search models (e.g. Kilo, Pollinations, DeepSeek, Qwen, OVH, AI Horde)...", color = Color(0xFF64748B), fontSize = 12.5.sp)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF262F45),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("free_ai_search_models_field")
                    )

                    // Table Column Headers matching Screenshot 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF151926), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp))
                        Text("Model & Provider", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.9f))
                        Text("● Reliability", color = Color(0xFF22C55E), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.95f))
                        Text("● Speed", color = Color(0xFF3B82F6), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.85f))
                        Text("● Intelligence", color = Color(0xFFA855F7), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.95f))
                        Text("Score", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                        Text("Order", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(74.dp))
                        Text("On", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp))
                    }

                    // Scrollable Models List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(filteredModels, key = { _, m -> m.spec.id }) { idx, state ->
                            val spec = state.spec
                            var rowFocused by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    rowFocused -> Color(0xFF1F273D)
                                    state.enabled -> Color(0xFF121622)
                                    else -> Color(0xFF0C0E16)
                                },
                                border = BorderStroke(
                                    width = if (rowFocused) 2.dp else 1.dp,
                                    color = if (rowFocused) Color(0xFF38BDF8) else Color(0x1FFFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { rowFocused = it.isFocused }
                                    .clickable {
                                        if (state.disabledByProvider) {
                                            Toast.makeText(context, "Enable ${spec.provider} above to use this model", Toast.LENGTH_SHORT).show()
                                        } else {
                                            RnaFreeAiRouter.setModelEnabled(context, spec.id, !state.enabled)
                                        }
                                    }
                                    .testTag("free_ai_model_row_${spec.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank #
                                    Text(
                                        text = "${idx + 1}",
                                        color = if (idx == 0) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.width(34.dp)
                                    )

                                    // Model Name + Provider + Badges (200/hr, 512K ctx, Vision, Tools, Learned/Cooldown)
                                    Row(
                                        modifier = Modifier.weight(1.9f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = spec.displayName,
                                            color = if (state.enabled) Color.White else Color(0xFF64748B),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Text(
                                            text = spec.provider,
                                            color = when (spec.provider.lowercase(Locale.US)) {
                                                "kilo" -> Color(0xFF38BDF8)
                                                "pollinations" -> Color(0xFF34D399)
                                                "ovh" -> Color(0xFFC084FC)
                                                "aihorde" -> Color(0xFFFBBF24)
                                                else -> Color(0xFF94A3B8)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        ModelTinyBadge(text = spec.rateLimitBadge, bg = Color(0xFF1E2433), fg = Color(0xFFCBD5E1))
                                        ModelTinyBadge(text = spec.contextBadge, bg = Color(0xFF1E2433), fg = Color(0xFFCBD5E1))
                                        if (state.disabledByProvider) {
                                            ModelTinyBadge(text = "Provider Off", bg = Color(0xFF3B1A1E), fg = Color(0xFFF87171))
                                        } else if (state.isInCooldown) {
                                            ModelTinyBadge(text = "Cooldown", bg = Color(0xFF3B1A1E), fg = Color(0xFFF87171))
                                        } else if (state.isDeviceLearned) {
                                            ModelTinyBadge(text = "Learned", bg = Color(0xFF0F2E26), fg = Color(0xFF34D399))
                                        }
                                        if (spec.hasVision) {
                                            ModelTinyBadge(text = "Vision", bg = Color(0xFF0D3B3E), fg = Color(0xFF2DD4BF))
                                        }
                                        if (spec.hasTools) {
                                            ModelTinyBadge(text = "Tools", bg = Color(0xFF2D1F4A), fg = Color(0xFFC084FC))
                                        }
                                    }

                                    // Reliability Bar (Green)
                                    MetricBarCell(
                                        score = state.effectiveReliability,
                                        barColor = Color(0xFF22C55E),
                                        modifier = Modifier.weight(0.95f)
                                    )

                                    // Speed Bar (Blue)
                                    MetricBarCell(
                                        score = spec.speedScore,
                                        barColor = Color(0xFF3B82F6),
                                        modifier = Modifier.weight(0.85f)
                                    )

                                    // Intelligence Bar (Purple)
                                    MetricBarCell(
                                        score = spec.intelligenceScore,
                                        barColor = Color(0xFFA855F7),
                                        modifier = Modifier.weight(0.95f)
                                    )

                                    // Overall Score
                                    Text(
                                        text = String.format(Locale.US, "%.3f", spec.overallScore),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.width(60.dp)
                                    )

                                    // Up / Down Custom Order Buttons
                                    Row(
                                        modifier = Modifier.width(74.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SmallOrderMoveButton(
                                            icon = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            onClick = {
                                                RnaFreeAiRouter.moveModelInCustomOrder(context, spec.id, -1)
                                            }
                                        )
                                        SmallOrderMoveButton(
                                            icon = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            onClick = {
                                                RnaFreeAiRouter.moveModelInCustomOrder(context, spec.id, +1)
                                            }
                                        )
                                    }

                                    // On/Off Switch
                                    Box(
                                        modifier = Modifier.width(58.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Switch(
                                            checked = state.enabled,
                                            enabled = !state.disabledByProvider,
                                            onCheckedChange = { next ->
                                                RnaFreeAiRouter.setModelEnabled(context, spec.id, next)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF22C55E)
                                            )
                                        )
                                    }
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
private fun AiProviderToggleBar(
    context: Context,
    disabledProviders: Set<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Providers:",
            color = Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
        RnaFreeAiRouter.FREE_AI_PROVIDERS.forEach { provider ->
            val enabled = provider.id !in disabledProviders
            val count = remember(provider.id) { RnaFreeAiRouter.getModelCountForProvider(provider.id) }
            val accent = Color(provider.accentHex)
            ExpressiveAiOptionChip(
                title = "${provider.displayName} ($count) ${if (enabled) "ON" else "OFF"}",
                selected = enabled,
                accentColor = if (enabled) accent else Color(0xFFEF4444),
                onClick = {
                    val nextEnabled = !enabled
                    RnaFreeAiRouter.setProviderEnabled(context, provider.id, nextEnabled)
                    Toast.makeText(
                        context,
                        "${provider.displayName} ${if (nextEnabled) "enabled" else "disabled"}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.weight(1f),
                testTag = "ai_provider_toggle_${provider.id}"
            )
        }
    }
}

@Composable
private fun ModelTinyBadge(
    text: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MetricBarCell(
    score: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF242B3D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((score.coerceIn(5, 100)) / 100f)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
        Text(
            text = "$score",
            color = Color(0xFFE2E8F0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
    }
}

@Composable
private fun SmallOrderMoveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (focused) Color.White else Color(0xFF1E263B),
        border = BorderStroke(1.dp, if (focused) Color(0xFF38BDF8) else Color(0x33FFFFFF)),
        modifier = Modifier
            .size(28.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (focused) Color(0xFF0F172A) else Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ExpressiveAiOptionChip(
    title: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ai_chip_scale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            focused -> Color.White
            selected -> accentColor.copy(alpha = 0.22f)
            else -> Color(0xFF141826)
        },
        border = BorderStroke(
            width = if (focused) 2.dp else 1.dp,
            color = when {
                focused -> Color.White
                selected -> accentColor
                else -> Color(0x2EFFFFFF)
            }
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (focused) Color(0xFF0F172A) else accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                color = if (focused) Color(0xFF0F172A) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Singleton pre-warmed Text-to-Speech controller so the very first spoken reply in RNA. AI
 * or EdVanced Voice Assistant starts immediately with zero engine initialization lag.
 */
object RnaTtsController {
    @Volatile
    private var tts: android.speech.tts.TextToSpeech? = null

    @Volatile
    private var isReady: Boolean = false

    @Volatile
    private var pendingUtterance: String? = null

    fun preWarm(context: Context) {
        if (tts != null) return
        synchronized(this) {
            if (tts != null) return
            val appCtx = context.applicationContext
            runCatching {
                tts = android.speech.tts.TextToSpeech(appCtx) { status ->
                    if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                        runCatching {
                            tts?.language = Locale.getDefault()
                            tts?.setPitch(1.0f)
                            tts?.setSpeechRate(1.06f)
                            isReady = true
                            // Prime synthesizer audio pipeline silently so first utterance has zero cold-start latency
                            val silentParams = android.os.Bundle().apply {
                                putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, 0f)
                            }
                            tts?.speak("", android.speech.tts.TextToSpeech.QUEUE_FLUSH, silentParams, "rna_tts_warmup")
                            val queued = pendingUtterance
                            if (!queued.isNullOrBlank()) {
                                pendingUtterance = null
                                speak(appCtx, queued)
                            }
                        }
                    }
                }
            }
        }
    }

    fun speak(context: Context, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        if (tts == null) {
            pendingUtterance = clean
            preWarm(context)
            return
        }
        if (!isReady) {
            pendingUtterance = clean
            return
        }
        runCatching {
            val params = android.os.Bundle().apply {
                putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(clean, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "rna_tts_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        pendingUtterance = null
        runCatching { tts?.stop() }
    }
}


