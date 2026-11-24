package com.example.ui.tv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "EdVancedVoiceAssistant"
private const val MODEL_NAME = "gemini-3.5-flash-lite"

sealed interface ParsedAssistantCommand {
    data class StartApp(val targetPackageOrName: String) : ParsedAssistantCommand
    data class StartBrowser(val targetUrl: String) : ParsedAssistantCommand
}

data class AssistantResponseResult(
    val spokenDisplayReply: String,
    val commands: List<ParsedAssistantCommand>
)

/**
 * AI Engine powered by EdVanced 1.1 Pro (backed by RnaFreeAiRouter reliability-sorted free AI fallback chain
 * or optional user-provided Google Gemini API key).
 * Parses `$start [app] app;` and `$start [url] browser;` commands while stripping them from visible/spoken text.
 */
internal object EdVancedAssistantEngine {

    private val commandRegex = Regex(
        pattern = "\\\$start\\s+([^\\s;]+(?:\\s+[^\\s;]+)*?)\\s+(app|browser)\\s*;?",
        option = RegexOption.IGNORE_CASE
    )

    fun parseAndCleanResponse(rawResponse: String): AssistantResponseResult {
        val commands = mutableListOf<ParsedAssistantCommand>()
        commandRegex.findAll(rawResponse).forEach { match ->
            val target = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val kind = match.groupValues.getOrNull(2)?.trim()?.lowercase(Locale.US).orEmpty()
            if (target.isNotEmpty()) {
                when (kind) {
                    "app" -> {
                        if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(target)) {
                            commands.add(ParsedAssistantCommand.StartBrowser("https://www.google.com"))
                        } else {
                            commands.add(ParsedAssistantCommand.StartApp(target))
                        }
                    }
                    "browser" -> commands.add(ParsedAssistantCommand.StartBrowser(target))
                }
            }
        }

        // Remove all $start ...; directives from the text shown on screen and spoken by TTS
        val stripped = rawResponse
            .replace(commandRegex, "")
            .replace(Regex("\\\$start[^\\n;]*;?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val finalDisplay = if (stripped.isNotBlank()) {
            stripped
        } else {
            when (commands.firstOrNull()) {
                is ParsedAssistantCommand.StartApp -> "Opening application now."
                is ParsedAssistantCommand.StartBrowser -> "Opening in RNA Browser now."
                null -> "Done."
            }
        }

        return AssistantResponseResult(
            spokenDisplayReply = finalDisplay,
            commands = commands
        )
    }

    suspend fun generateAssistantReply(
        context: Context,
        userQuery: String,
        installedApps: List<UnifiedLauncherApp>
    ): AssistantResponseResult = withContext(Dispatchers.IO) {
        val rnaToolsSystemPrompt = RnaAiToolsEngine.buildRnaToolsAndAppToolsSystemPrompt(context, installedApps)

        val fullPrompt = """
            You are the RNA TV Voice Assistant, powered by EdVanced 1.1 Pro.
            CRITICAL RULES:
            1. NEVER mention the word "Gemini" or "Google". If asked who you are, say you are powered by EdVanced 1.1 Pro.
            2. Keep your conversational reply VERY SHORT (maximum 1 to 2 short sentences, under 22 words) so Text-to-Speech finishes quickly.
            3. You can perform real actions on the TV using these exact command formats at the end of your reply:
               - To open an installed app: ${'$'}start [package-name] app; (or ${'$'}start [package-name] rna-tools;)
               - To open a website or search in the built-in RNA Browser: ${'$'}start [website-url] browser;
               - To change wallpapers, profiles, or Control Panel settings: ${'$'}start [command] rna-tools;
               - To create an RNA App or HTML5 TV App: ${'$'}start app-name.[Name] app-tools; ${'$'}start app-name.[Name].img-gen app-tools; and ${'$'}start app-url.[url] app-tools; OR ${'$'}start app-html.{<!DOCTYPE html>...} app-tools;
            4. NEVER OPEN EXTERNAL BROWSER APPS:
               - When asked to open the browser or open anything in the browser, NEVER open any external browser app on the device (like Chrome, Firefox, or Browser). Always use ${'$'}start [website-url] browser; so it opens directly inside the RNA app's built-in Browser!
            5. SMART APP VS WEBSITE CHOICE:
               - If the user asks to simply open an app (for example: "open Netflix" or "open YouTube"), use the app command with its package name, e.g.:
                 Opening Netflix. ${'$'}start com.netflix.mediaclient app;
               - However, if the user asks to search for a specific topic/video IN YouTube (for example: "search for funny cats in YouTube" or "play lofi beats on YouTube"), you MUST open the YouTube website search URL in the RNA Browser because you cannot directly search inside the YouTube TV app:
                 Searching YouTube for funny cats. ${'$'}start https://www.youtube.com/results?search_query=funny+cats browser;
               - If the user asks to find or search something on Google or the web (for example: "find newest movies in Google"), respond with:
                 Here are the newest movies on Google. ${'$'}start https://www.google.com/search?q=newest+movies browser;

            $rnaToolsSystemPrompt

            USER REQUEST: "$userQuery"
        """.trimIndent()

        val outcome = RnaFreeAiRouter.generateTextOrVision(
            context = context,
            prompt = fullPrompt,
            temperature = 0.25f,
            source = "Voice Assistant",
            userDisplayPrompt = userQuery
        )
        val rawText = outcome.text

        val toolsReport = withContext(Dispatchers.Main) {
            RnaAiToolsEngine.processAndExecuteTools(context, rawText)
        }
        val parsed = parseAndCleanResponse(toolsReport.cleanedText)
        if (parsed.spokenDisplayReply == "Done." && toolsReport.executedSummaries.isNotEmpty()) {
            parsed.copy(spokenDisplayReply = toolsReport.executedSummaries.joinToString(". "))
        } else {
            parsed
        }
    }
}

/**
 * Helper to silence Android SpeechRecognizer start/stop system beeps during continuous
 * background "Hey RNA" wake-word listening, and to play a single crisp microphone-start
 * sound ONLY when "Hey RNA" is spoken and the bottom AI bar pops up.
 */
internal object WakeWordAudioSilencer {
    private val streamsToMute = intArrayOf(
        AudioManager.STREAM_SYSTEM,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_MUSIC
    )

    fun setSilentForWakeWordCycle(context: Context, mute: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val dir = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
        for (stream in streamsToMute) {
            runCatching { am.adjustStreamVolume(stream, dir, 0) }
        }
    }

    fun playMicReadyChime(context: Context) {
        setSilentForWakeWordCycle(context, mute = false)
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { toneGen.release() }
            }, 220L)
        }
    }
}

/**
 * Google Assistant-style TV Voice & Type AI Overlay powered by EdVanced 1.1 Pro.
 * - Automatically starts speech recognition on open and plays the microphone ready chime.
 * - Features a Type button, Listen button, and a tiny Agent logo button right in the AI bar.
 * - Shows 3 suggested questions.
 * - Speaks short replies using Android TextToSpeech (when enabled in Control Panel or when Listen is clicked).
 * - Executes `$start [app] app;` and `$start [url] browser;` automatically without speaking or showing `$start...`.
 */
@Composable
fun TvVoiceAssistantOverlay(
    visible: Boolean,
    initialWakeQuery: String? = null,
    allApps: List<UnifiedLauncherApp>,
    ttsEnabled: Boolean,
    onToggleTts: (Boolean) -> Unit,
    onLaunchAppByPackageOrName: (String) -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
    onDismiss: () -> Unit,
    onEnterAgentMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var thinkingSeconds by remember { mutableStateOf(0) }
    var isTypingMode by remember { mutableStateOf(false) }
    var typedInput by remember { mutableStateOf("") }
    var liveTranscript by remember { mutableStateOf("") }
    var assistantReply by remember {
        mutableStateOf("Listening... Speak now or press Type to ask EdVanced 1.1 Pro.")
    }

    val micButtonFocus = remember { FocusRequester() }
    val typeButtonFocus = remember { FocusRequester() }
    val listenButtonFocus = remember { FocusRequester() }
    val agentButtonFocus = remember { FocusRequester() }
    val textFieldFocus = remember { FocusRequester() }

    val suggestedQuestions = remember {
        listOf(
            "Open YouTube",
            "Find newest movies in Google",
            "Search 4K nature videos in YouTube"
        )
    }

    // Pre-warm singleton TTS engine immediately for zero first-speech lag
    LaunchedEffect(Unit) {
        RnaTtsController.preWarm(context)
    }

    LaunchedEffect(isThinking) {
        if (isThinking) {
            val startedAtMs = System.currentTimeMillis()
            thinkingSeconds = 0
            while (isThinking) {
                thinkingSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt()
                delay(250L)
            }
        } else {
            thinkingSeconds = 0
        }
    }

    fun speakTextForce(textToSpeak: String) {
        if (textToSpeak.isBlank()) return
        WakeWordAudioSilencer.setSilentForWakeWordCycle(context, mute = false)
        RnaTtsController.speak(context, textToSpeak)
    }

    fun speakTextIfEnabled(textToSpeak: String) {
        if (!ttsEnabled) return
        speakTextForce(textToSpeak)
    }

    var speechRecognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var voiceSessionSeq by remember { mutableStateOf(0) }
    var hasSubmittedVoiceSession by remember { mutableStateOf(false) }
    var latestVoicePartial by remember { mutableStateOf("") }
    var voiceSilenceJob by remember { mutableStateOf<Job?>(null) }
    var voiceEndFallbackJob by remember { mutableStateOf<Job?>(null) }

    fun cancelVoiceJobs() {
        voiceSilenceJob?.cancel()
        voiceSilenceJob = null
        voiceEndFallbackJob?.cancel()
        voiceEndFallbackJob = null
    }

    fun submitQueryToEdVanced(queryText: String) {
        val cleanedQuery = queryText.trim()
        if (cleanedQuery.isEmpty() || isThinking) return

        cancelVoiceJobs()
        isListening = false
        runCatching { speechRecognizerRef?.stopListening() }
        liveTranscript = cleanedQuery
        isThinking = true
        assistantReply = "Thinking..."

        scope.launch {
            try {
                val result = withTimeout(60_000L) {
                    EdVancedAssistantEngine.generateAssistantReply(
                        context = context,
                        userQuery = cleanedQuery,
                        installedApps = allApps
                    )
                }
                isThinking = false
                assistantReply = result.spokenDisplayReply
                speakTextIfEnabled(result.spokenDisplayReply)

                // Execute any $start [app] app; or $start [url] browser; commands
                if (result.commands.isNotEmpty()) {
                    val prefs = context.getSharedPreferences(com.example.RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
                    val ytBehavior = loadRnaAiSettings(prefs).youtubeLinkBehavior
                    delay(360L)
                    result.commands.forEach { cmd ->
                        when (cmd) {
                            is ParsedAssistantCommand.StartApp -> {
                                onLaunchAppByPackageOrName(cmd.targetPackageOrName)
                                onDismiss()
                            }
                            is ParsedAssistantCommand.StartBrowser -> {
                                if (isYouTubeUrl(cmd.targetUrl) && ytBehavior == YT_BEHAVIOR_YOUTUBE_APP) {
                                    if (!launchYouTubeAppForUrl(context, cmd.targetUrl)) {
                                        onOpenUrlInBrowser(cmd.targetUrl)
                                    }
                                } else {
                                    onOpenUrlInBrowser(cmd.targetUrl)
                                }
                                onDismiss()
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                isThinking = false
                assistantReply = "Request timed out. Please try again."
                speakTextIfEnabled(assistantReply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "EdVanced 1.1 Pro query failed", e)
                isThinking = false
                assistantReply = e.localizedMessage ?: "Could not reach EdVanced 1.1 Pro."
            }
        }
    }

    fun finalizeVoiceSession(sessionId: Int, finalText: String?) {
        if (sessionId != voiceSessionSeq || hasSubmittedVoiceSession) return
        val chosen = finalText?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestVoicePartial.trim().takeIf { it.isNotEmpty() }
        hasSubmittedVoiceSession = true
        cancelVoiceJobs()
        isListening = false
        latestVoicePartial = ""
        runCatching {
            speechRecognizerRef?.stopListening()
            speechRecognizerRef?.cancel()
        }
        if (!chosen.isNullOrBlank()) {
            submitQueryToEdVanced(chosen)
        } else if (liveTranscript.isBlank()) {
            assistantReply = "Tap Microphone to speak or press Type to enter your request."
        }
    }

    // Fallback system speech activity launcher when inline SpeechRecognizer service is absent
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        isListening = false
        val spokenList = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val firstSpoken = spokenList?.firstOrNull()?.trim()
        if (!firstSpoken.isNullOrBlank()) {
            submitQueryToEdVanced(firstSpoken)
        }
    }

    fun startVoiceRecognitionInternal() {
        RnaTtsController.stop()
        cancelVoiceJobs()
        val activeSession = voiceSessionSeq + 1
        voiceSessionSeq = activeSession
        hasSubmittedVoiceSession = false
        latestVoicePartial = ""

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                runCatching {
                    speechRecognizerRef?.stopListening()
                    speechRecognizerRef?.cancel()
                    speechRecognizerRef?.destroy()
                }
                speechRecognizerRef = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            if (activeSession != voiceSessionSeq) return
                            isListening = true
                            assistantReply = "Listening... Speak now"
                        }

                        override fun onBeginningOfSpeech() {
                            if (activeSession != voiceSessionSeq) return
                            isListening = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            if (activeSession != voiceSessionSeq || hasSubmittedVoiceSession) return
                            voiceSilenceJob?.cancel()
                            voiceEndFallbackJob?.cancel()
                            voiceEndFallbackJob = scope.launch {
                                delay(450L)
                                if (activeSession == voiceSessionSeq && !hasSubmittedVoiceSession) {
                                    finalizeVoiceSession(activeSession, latestVoicePartial)
                                }
                            }
                        }

                        override fun onError(error: Int) {
                            if (activeSession != voiceSessionSeq) return
                            if (!hasSubmittedVoiceSession && latestVoicePartial.isNotBlank()) {
                                finalizeVoiceSession(activeSession, latestVoicePartial)
                            } else {
                                cancelVoiceJobs()
                                isListening = false
                                if (liveTranscript.isBlank()) {
                                    assistantReply = "Tap Microphone to speak or press Type to enter your request."
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            if (activeSession != voiceSessionSeq) return
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val topMatch = matches?.firstOrNull()?.trim()
                            finalizeVoiceSession(activeSession, topMatch)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            if (activeSession != voiceSessionSeq || hasSubmittedVoiceSession) return
                            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val topPartial = partials?.firstOrNull()?.trim()
                            if (!topPartial.isNullOrBlank()) {
                                latestVoicePartial = topPartial
                                liveTranscript = topPartial
                                voiceSilenceJob?.cancel()
                                voiceSilenceJob = scope.launch {
                                    delay(1350L)
                                    if (activeSession == voiceSessionSeq && !hasSubmittedVoiceSession) {
                                        finalizeVoiceSession(activeSession, latestVoicePartial)
                                    }
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                }
                isListening = true
                speechRecognizerRef?.startListening(recognizerIntent)
                return
            } catch (_: Exception) {
            }
        }

        // Fallback to system speech recognizer activity or switch to typing mode smoothly
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to RNA Assistant (Powered by EdVanced 1.1 Pro)")
            }
            systemSpeechLauncher.launch(intent)
        } catch (_: Exception) {
            isListening = false
            isTypingMode = true
            assistantReply = "Voice hardware unavailable on this device. Type your question below:"
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognitionInternal()
        } else {
            isListening = false
            isTypingMode = true
            assistantReply = "Microphone permission declined. You can type your request below:"
        }
    }

    fun triggerVoiceCapture() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            startVoiceRecognitionInternal()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Automatically start listening when the overlay opens, playing the mic start chime right as the AI bar pops up!
    LaunchedEffect(visible, initialWakeQuery) {
        if (visible) {
            val prefs = context.getSharedPreferences(com.example.RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(context, prefs)
            WakeWordAudioSilencer.playMicReadyChime(context)
            isTypingMode = false
            typedInput = ""
            liveTranscript = ""
            delay(80L)
            micButtonFocus.safeRequestFocus()
            if (!initialWakeQuery.isNullOrBlank()) {
                submitQueryToEdVanced(initialWakeQuery)
            } else {
                triggerVoiceCapture()
            }
        } else {
            cancelVoiceJobs()
            isListening = false
            runCatching { speechRecognizerRef?.stopListening() }
            RnaTtsController.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cancelVoiceJobs()
            RnaTtsController.stop()
            runCatching {
                speechRecognizerRef?.stopListening()
                speechRecognizerRef?.cancel()
                speechRecognizerRef?.destroy()
            }
        }
    }

    BackHandler(enabled = visible) {
        onDismiss()
    }

    val pulseTransition = rememberInfiniteTransition(label = "AssistantMicPulse")
    val micPulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(260)),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(220))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x5506080E),
                        0.55f to Color(0xAA080B14),
                        1.0f to Color(0xF50B0E1A)
                    )
                )
                .clickable { onDismiss() }
                .testTag("tv_voice_assistant_overlay")
        ) {
            // Google Assistant-style Bottom Dock Sheet on TV Screen
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = Color(0xFA131726),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(CyanPrimary, VioletSecondary, EmeraldSuccess)
                    )
                ),
                shadowElevation = 28.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.88f)
                    .padding(bottom = 26.dp)
                    .clickable(enabled = false) {}
                    .testTag("tv_voice_assistant_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Top Branding Bar ("Powered by EdVanced 1.1 Pro" + TTS Mute/Unmute + Close)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CyanPrimary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Powered by EdVanced 1.1 Pro",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isListening) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, EmeraldSuccess)
                                ) {
                                    Text(
                                        text = "LISTENING",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick TTS Toggle Chip
                            var ttsChipFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (ttsChipFocused) Color.White else Color(0xFF22273B),
                                border = BorderStroke(1.dp, if (ttsEnabled) CyanPrimary else Color(0x44FFFFFF)),
                                modifier = Modifier
                                    .onFocusChanged { ttsChipFocused = it.isFocused }
                                    .clickable {
                                        val next = !ttsEnabled
                                        if (!next) RnaTtsController.stop()
                                        onToggleTts(next)
                                    }
                                    .testTag("tv_assistant_tts_toggle_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = null,
                                        tint = if (ttsChipFocused) Color(0xFF10131C) else if (ttsEnabled) CyanPrimary else Color(0xFF9E9FAF),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (ttsEnabled) "Voice Output: ON" else "Voice Output: OFF",
                                        color = if (ttsChipFocused) Color(0xFF10131C) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("tv_assistant_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Assistant",
                                    tint = Color(0xFFE4E1E9)
                                )
                            }
                        }
                    }

                    // 2. Main Controls Row: Microphone Button + Type Button right near Microphone + Live Response Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Microphone Button (Automatically active on launch)
                        var micFocused by remember { mutableStateOf(false) }
                        val micScale by animateFloatAsState(
                            targetValue = when {
                                isListening -> micPulseScale
                                micFocused -> 1.1f
                                else -> 1f
                            },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "MicBtnScale"
                        )
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isListening -> CyanPrimary
                                micFocused -> Color.White
                                else -> Color(0xFF232940)
                            },
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (isListening || micFocused) Color.White else CyanPrimary
                            ),
                            shadowElevation = if (isListening || micFocused) 18.dp else 6.dp,
                            modifier = Modifier
                                .size(62.dp)
                                .scale(micScale)
                                .focusRequester(micButtonFocus)
                                .onFocusChanged { micFocused = it.isFocused }
                                .clickable {
                                    if (isListening) {
                                        isListening = false
                                        runCatching { speechRecognizerRef?.stopListening() }
                                    } else {
                                        triggerVoiceRecognitionOrPermission(
                                            onTrigger = { triggerVoiceCapture() }
                                        )
                                    }
                                }
                                .focusable()
                                .testTag("tv_ai_mic_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Speak to Assistant",
                                    tint = if (isListening || micFocused) Color(0xFF10131C) else CyanPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Type Button right near the Microphone Button!
                        var typeFocused by remember { mutableStateOf(false) }
                        val typeScale by animateFloatAsState(
                            targetValue = if (typeFocused) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "TypeBtnScale"
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                typeFocused -> Color.White
                                isTypingMode -> VioletSecondary
                                else -> Color(0xFF232940)
                            },
                            border = BorderStroke(
                                width = if (typeFocused) 2.dp else 1.5.dp,
                                color = if (typeFocused) CyanPrimary else VioletSecondary
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .scale(typeScale)
                                .focusRequester(typeButtonFocus)
                                .onFocusChanged { typeFocused = it.isFocused }
                                .clickable {
                                    isTypingMode = !isTypingMode
                                    if (isTypingMode) {
                                        isListening = false
                                        runCatching { speechRecognizerRef?.stopListening() }
                                        scope.launch {
                                            delay(60L)
                                            textFieldFocus.safeRequestFocus()
                                        }
                                    }
                                }
                                .focusable()
                                .testTag("tv_ai_type_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Type to Assistant",
                                    tint = if (typeFocused || isTypingMode) Color(0xFF10131C) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Type",
                                    color = if (typeFocused || isTypingMode) Color(0xFF10131C) else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        // Listen Button right near Type & AI response so the user can replay the AI audio response over and over!
                        var listenFocused by remember { mutableStateOf(false) }
                        val listenScale by animateFloatAsState(
                            targetValue = if (listenFocused) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "ListenBtnScale"
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                listenFocused -> Color.White
                                else -> Color(0xFF232940)
                            },
                            border = BorderStroke(
                                width = if (listenFocused) 2.dp else 1.5.dp,
                                color = if (listenFocused) CyanPrimary else EmeraldSuccess
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .scale(listenScale)
                                .focusRequester(listenButtonFocus)
                                .onFocusChanged { listenFocused = it.isFocused }
                                .clickable {
                                    speakTextForce(assistantReply)
                                }
                                .focusable()
                                .testTag("tv_ai_listen_again_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to AI Response",
                                    tint = if (listenFocused) Color(0xFF10131C) else EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Listen",
                                    color = if (listenFocused) Color(0xFF10131C) else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        // Tiny Agent Logo Button: grants AI full autonomous access over TV with soft blue screen glow
                        var agentBtnFocused by remember { mutableStateOf(false) }
                        val agentScale by animateFloatAsState(
                            targetValue = if (agentBtnFocused) 1.08f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "AgentBtnScale"
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (agentBtnFocused) Color.White else Color(0xFF152544),
                            border = BorderStroke(
                                width = if (agentBtnFocused) 2.dp else 1.5.dp,
                                color = if (agentBtnFocused) CyanPrimary else Color(0xFF2979FF)
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .scale(agentScale)
                                .focusRequester(agentButtonFocus)
                                .onFocusChanged { agentBtnFocused = it.isFocused }
                                .clickable {
                                    isListening = false
                                    runCatching { speechRecognizerRef?.stopListening() }
                                    RnaTtsController.stop()
                                    RnaAutonomousAgentController.installedAppsSnapshot = allApps
                                    RnaAutonomousAgentController.enterAgentMode()
                                    if (onEnterAgentMode != null) {
                                        onEnterAgentMode.invoke()
                                    } else {
                                        onDismiss()
                                    }
                                }
                                .focusable()
                                .testTag("tv_ai_agent_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Autonomous Agent Mode",
                                    tint = if (agentBtnFocused) Color(0xFF10131C) else Color(0xFF40C4FF),
                                    modifier = Modifier.size(19.dp)
                                )
                                Text(
                                    text = "Agent",
                                    color = if (agentBtnFocused) Color(0xFF10131C) else Color(0xFF80D8FF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Spoken Transcript & Short AI Response Display
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (liveTranscript.isNotBlank()) {
                                Text(
                                    text = "\"$liveTranscript\"",
                                    color = CyanPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isThinking) {
                                    CircularProgressIndicator(
                                        color = CyanPrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                val thinkingLabel = if (thinkingSeconds == 1) "1 sec" else "$thinkingSeconds secs"
                                Text(
                                    text = if (isThinking) "Thinking($thinkingLabel)..." else assistantReply,
                                    color = Color.White,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 3. Expandable Keyboard Text Input Row when Type button is pressed
                    AnimatedVisibility(visible = isTypingMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = typedInput,
                                onValueChange = { typedInput = it },
                                placeholder = {
                                    Text(
                                        "Type a command (e.g., 'Open Netflix' or 'Find newest movies in Google')...",
                                        color = Color(0xFF8C90A4),
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0xFF3E445E),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(textFieldFocus)
                                    .testTag("tv_ai_type_input_field")
                            )

                            Button(
                                onClick = {
                                    if (typedInput.isNotBlank()) {
                                        val q = typedInput
                                        typedInput = ""
                                        submitQueryToEdVanced(q)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF10131C)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("tv_ai_send_typed_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // 4. 3 Suggested Questions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Try asking:",
                            color = Color(0xFF9E9FAF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        suggestedQuestions.forEachIndexed { idx, suggestion ->
                            var chipFocused by remember { mutableStateOf(false) }
                            val chipScale by animateFloatAsState(
                                targetValue = if (chipFocused) 1.05f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "SuggestedChipScale_$idx"
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (chipFocused) Color.White else Color(0xFF1E2438),
                                border = BorderStroke(
                                    width = if (chipFocused) 2.dp else 1.dp,
                                    color = if (chipFocused) CyanPrimary else Color(0x38FFFFFF)
                                ),
                                modifier = Modifier
                                    .scale(chipScale)
                                    .onFocusChanged { chipFocused = it.isFocused }
                                    .clickable { submitQueryToEdVanced(suggestion) }
                                    .focusable()
                                    .testTag("tv_ai_suggested_question_$idx")
                            ) {
                                Text(
                                    text = suggestion,
                                    color = if (chipFocused) Color(0xFF10131C) else Color(0xFFE4E6F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Floating App Creation Hover Card (for $start ... app-tools;)
            RnaAppCreationHoverCardOverlay(
                context = context,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

private inline fun triggerVoiceRecognitionOrPermission(onTrigger: () -> Unit) {
    onTrigger()
}
