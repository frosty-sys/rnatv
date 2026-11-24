package com.example.ui.tv

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.RnaHomeInterceptorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.random.Random

enum class RnaPhoneControllerMode(val id: String, val label: String) {
    REMOTE("REMOTE", "TV Remote"),
    XBOX_GAMEPAD("XBOX_GAMEPAD", "Xbox Controller");

    companion object {
        fun fromId(id: String?): RnaPhoneControllerMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: REMOTE
        }
    }
}

data class ConnectedPhoneController(
    val deviceId: String,
    val deviceName: String,
    val ipAddress: String,
    val slot: Int, // 1 = Xbox Controller 1 (Player 1), 2 = Xbox Controller 2 (Player 2)
    val mode: RnaPhoneControllerMode,
    val connectedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val lastInputSummary: String = "Connected"
) {
    val controllerName: String get() = deviceName
    val slotIndex: Int get() = slot
    val virtualDeviceId: Int
        get() = if (slot == 1) RnaControllerNetworkBridge.SPOOFED_XBOX_DEVICE_ID_SLOT_1
        else RnaControllerNetworkBridge.SPOOFED_XBOX_DEVICE_ID_SLOT_2
}

data class TvExportedAppInfo(
    val packageName: String,
    val title: String,
    val iconBase64: String,
    val isRnaWebApp: Boolean = false
)

typealias ExportedTvAppInfo = TvExportedAppInfo

/**
 * Full Xbox Wireless Controller state for a single controller slot (Slot 1 or Slot 2).
 * Maps every physical Xbox button & analog axis to Android's official InputDevice constants:
 * - A Button: KEYCODE_BUTTON_A (bit 0)
 * - B Button: KEYCODE_BUTTON_B (bit 1)
 * - X Button: KEYCODE_BUTTON_X (bit 2)
 * - Y Button: KEYCODE_BUTTON_Y (bit 3)
 * - LB (Left Bumper): KEYCODE_BUTTON_L1 (bit 4)
 * - RB (Right Bumper): KEYCODE_BUTTON_R1 (bit 5)
 * - Left Stick Click: KEYCODE_BUTTON_THUMBL (bit 6)
 * - Right Stick Click: KEYCODE_BUTTON_THUMBR (bit 7)
 * - Start Button: KEYCODE_BUTTON_START (bit 8)
 * - Back/View Button: KEYCODE_BUTTON_SELECT (bit 9)
 * - Xbox Guide (RNA) Button: KEYCODE_BUTTON_MODE (bit 10)
 * - D-Pad Up/Down/Left/Right: KEYCODE_DPAD_* (bits 11..14) + AXIS_HAT_X / AXIS_HAT_Y
 * - Left Analog Stick: AXIS_X / AXIS_Y
 * - Right Analog Stick: AXIS_Z / AXIS_RZ
 * - LT / RT Analog Triggers: AXIS_LTRIGGER / AXIS_RTRIGGER
 */
data class XboxGamepadFrame(
    val slot: Int = 1,
    val buttonBitmask: Int = 0,
    val axisX: Float = 0f,
    val axisY: Float = 0f,
    val axisZ: Float = 0f,
    val axisRZ: Float = 0f,
    val axisLTrigger: Float = 0f,
    val axisRTrigger: Float = 0f,
    val axisHatX: Float = 0f,
    val axisHatY: Float = 0f
) {
    fun isButtonDown(bitIndex: Int): Boolean = (buttonBitmask and (1 shl bitIndex)) != 0
}

object XboxButtonBits {
    const val BIT_A = 0
    const val BIT_B = 1
    const val BIT_X = 2
    const val BIT_Y = 3
    const val BIT_LB = 4
    const val BIT_RB = 5
    const val BIT_THUMBL = 6
    const val BIT_THUMBR = 7
    const val BIT_START = 8
    const val BIT_SELECT = 9
    const val BIT_MODE = 10
    const val BIT_DPAD_UP = 11
    const val BIT_DPAD_DOWN = 12
    const val BIT_DPAD_LEFT = 13
    const val BIT_DPAD_RIGHT = 14
    const val BIT_LT = 15
    const val BIT_RT = 16

    val bitToKeyCode: Map<Int, Int> = mapOf(
        BIT_A to KeyEvent.KEYCODE_BUTTON_A,
        BIT_B to KeyEvent.KEYCODE_BUTTON_B,
        BIT_X to KeyEvent.KEYCODE_BUTTON_X,
        BIT_Y to KeyEvent.KEYCODE_BUTTON_Y,
        BIT_LB to KeyEvent.KEYCODE_BUTTON_L1,
        BIT_RB to KeyEvent.KEYCODE_BUTTON_R1,
        BIT_THUMBL to KeyEvent.KEYCODE_BUTTON_THUMBL,
        BIT_THUMBR to KeyEvent.KEYCODE_BUTTON_THUMBR,
        BIT_START to KeyEvent.KEYCODE_BUTTON_START,
        BIT_SELECT to KeyEvent.KEYCODE_BUTTON_SELECT,
        BIT_MODE to KeyEvent.KEYCODE_BUTTON_MODE,
        BIT_DPAD_UP to KeyEvent.KEYCODE_DPAD_UP,
        BIT_DPAD_DOWN to KeyEvent.KEYCODE_DPAD_DOWN,
        BIT_DPAD_LEFT to KeyEvent.KEYCODE_DPAD_LEFT,
        BIT_DPAD_RIGHT to KeyEvent.KEYCODE_DPAD_RIGHT,
        BIT_LT to KeyEvent.KEYCODE_BUTTON_L2,
        BIT_RT to KeyEvent.KEYCODE_BUTTON_R2
    )

    // Generic hardware gamepad buttons (BUTTON_1 .. BUTTON_16) recognized by PPSSPP, RetroArch, and TV Quick Actions Pro
    val bitToGenericButtonKeyCode: Map<Int, Int> = mapOf(
        BIT_A to KeyEvent.KEYCODE_BUTTON_1,
        BIT_B to KeyEvent.KEYCODE_BUTTON_2,
        BIT_X to KeyEvent.KEYCODE_BUTTON_3,
        BIT_Y to KeyEvent.KEYCODE_BUTTON_4,
        BIT_LB to KeyEvent.KEYCODE_BUTTON_5,
        BIT_RB to KeyEvent.KEYCODE_BUTTON_6,
        BIT_LT to KeyEvent.KEYCODE_BUTTON_7,
        BIT_RT to KeyEvent.KEYCODE_BUTTON_8,
        BIT_SELECT to KeyEvent.KEYCODE_BUTTON_9,
        BIT_START to KeyEvent.KEYCODE_BUTTON_10,
        BIT_THUMBL to KeyEvent.KEYCODE_BUTTON_11,
        BIT_THUMBR to KeyEvent.KEYCODE_BUTTON_12,
        BIT_DPAD_UP to KeyEvent.KEYCODE_BUTTON_13,
        BIT_DPAD_DOWN to KeyEvent.KEYCODE_BUTTON_14,
        BIT_DPAD_LEFT to KeyEvent.KEYCODE_BUTTON_15,
        BIT_DPAD_RIGHT to KeyEvent.KEYCODE_BUTTON_16
    )
}

/**
 * Central Network Bridge & Xbox Controller Spoofer for RNA TV & Mobile.
 * Supports:
 * - Up to 2 simultaneous phones connected over the same Wi-Fi network or mobile hotspot.
 * - 4-digit PIN pairing for Controller 1 and a separate 4-digit PIN for Controller 2.
 * - Automatic zero-PIN reconnection when a previously paired phone opens the Controller screen on the same network.
 * - Zero-lag UDP gamepad streaming + Android KeyEvent/MotionEvent Xbox Controller spoofing (Device IDs 1001 & 1002).
 * - Exporting TV app names, package names, and icon logos to the phone so the phone can add TV apps as remote buttons.
 * - Sending text from the phone's Keyboard button to whatever text box is currently open on the TV (ignored if none open).
 */
object RnaControllerNetworkBridge {

    const val TCP_PORT = 49210
    const val UDP_PORT = 49211
    const val MAX_CONTROLLERS = 2

    // Distinct spoofed Android InputDevice IDs for Player 1 and Player 2 Xbox Wireless Controllers
    const val SPOOFED_XBOX_DEVICE_ID_SLOT_1 = 1001
    const val SPOOFED_XBOX_DEVICE_ID_SLOT_2 = 1002

    private const val PREF_TRUSTED_CONTROLLERS_JSON = "rna_tv_trusted_phone_controllers_v1"
    private const val PREF_PHONE_DEVICE_ID = "rna_phone_controller_device_id_v1"
    private const val PREF_PHONE_CONTROLLER_NAME = "rna_phone_controller_custom_name_v1"
    private const val PREF_PHONE_LAST_TV_IP = "rna_phone_controller_last_tv_ip_v1"
    private const val PREF_PHONE_SHORTCUT_APPS_JSON = "rna_phone_controller_shortcut_apps_v1"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    // ── TV Server StateFlows ──
    private val _pinCodeSlot1 = MutableStateFlow(generateRandom4DigitPin())
    val pinCodeSlot1: StateFlow<String> = _pinCodeSlot1.asStateFlow()

    private val _pinCodeSlot2 = MutableStateFlow(generateRandom4DigitPin())
    val pinCodeSlot2: StateFlow<String> = _pinCodeSlot2.asStateFlow()

    private val _isSlot2PairingOpen = MutableStateFlow(false)
    val isSlot2PairingOpen: StateFlow<Boolean> = _isSlot2PairingOpen.asStateFlow()
    val isSecondSlotPinVisible: StateFlow<Boolean> get() = isSlot2PairingOpen

    private val _lastActionStatus = MutableStateFlow("")
    val lastActionStatus: StateFlow<String> = _lastActionStatus.asStateFlow()

    private val _connectedControllers = MutableStateFlow<List<ConnectedPhoneController>>(emptyList())
    val connectedControllers: StateFlow<List<ConnectedPhoneController>> = _connectedControllers.asStateFlow()

    private val _tvLocalIp = MutableStateFlow("Detecting...")
    val tvLocalIp: StateFlow<String> = _tvLocalIp.asStateFlow()

    private val _gamepadStateSlot1 = MutableStateFlow(XboxGamepadFrame(slot = 1))
    val gamepadStateSlot1: StateFlow<XboxGamepadFrame> = _gamepadStateSlot1.asStateFlow()

    private val _gamepadStateSlot2 = MutableStateFlow(XboxGamepadFrame(slot = 2))
    val gamepadStateSlot2: StateFlow<XboxGamepadFrame> = _gamepadStateSlot2.asStateFlow()

    // ── Phone Client StateFlows ──
    private val _phoneConnectedState = MutableStateFlow(false)
    val phoneConnectedState: StateFlow<Boolean> = _phoneConnectedState.asStateFlow()

    private val _phoneAssignedSlot = MutableStateFlow(1)
    val phoneAssignedSlot: StateFlow<Int> = _phoneAssignedSlot.asStateFlow()

    private val _phoneConnectionStatusMessage = MutableStateFlow("")
    val phoneConnectionStatusMessage: StateFlow<String> = _phoneConnectionStatusMessage.asStateFlow()

    private val _phoneTvVolumePercent = MutableStateFlow(50)
    val phoneTvVolumePercent: StateFlow<Int> = _phoneTvVolumePercent.asStateFlow()

    private val _phoneAvailableTvApps = MutableStateFlow<List<TvExportedAppInfo>>(emptyList())
    val phoneAvailableTvApps: StateFlow<List<TvExportedAppInfo>> = _phoneAvailableTvApps.asStateFlow()

    @Volatile
    private var activePhoneTvIp: String = ""

    @Volatile
    private var activePhoneDeviceId: String = ""

    @Volatile
    private var activePhoneButtonBitmask: Int = 0

    // Tracks whichever Activity in RNA is currently in RESUMED state (or null when an external app is in the foreground)
    @Volatile
    var activeForegroundActivityRef: WeakReference<Activity>? = null

    // ── TV Action Callbacks (wired by GoogleTvLauncherScreen / MainActivity / TvBrowserTab) ──
    var appContextRef: WeakReference<Context>? = null
    var activeWebViewRef: WeakReference<WebView>? = null
    var onRemoteNavigateHome: (() -> Unit)? = null
    var onRemoteOpenRecentApps: (() -> Unit)? = null
    var onRemoteOpenTaskbar: (() -> Unit)? = null
    var onRemoteOpenAiBar: (() -> Unit)? = null
    var onRemoteLaunchAppByPackage: ((String) -> Unit)? = null
    var onRemoteInjectComposeText: ((String) -> Boolean)? = null

    @Volatile
    private var exportedTvAppsCache: List<TvExportedAppInfo> = emptyList()

    @Volatile
    private var isServerRunning = false
    private var tcpServerSocket: ServerSocket? = null
    private var udpServerSocket: DatagramSocket? = null

    private val previousSlotFrames = ConcurrentHashMap<Int, XboxGamepadFrame>()
    private val lastAnalogNavTimestamp = ConcurrentHashMap<Int, Long>()

    private fun generateRandom4DigitPin(): String {
        val n = Random.nextInt(1000, 9999)
        return n.toString()
    }

    fun regeneratePinSlot1() {
        _pinCodeSlot1.value = generateRandom4DigitPin()
    }

    fun regeneratePinForSlot(context: Context, slot: Int) {
        if (slot == 2) {
            _pinCodeSlot2.value = generateRandom4DigitPin()
            _isSlot2PairingOpen.value = true
        } else {
            _pinCodeSlot1.value = generateRandom4DigitPin()
        }
    }

    fun enableSecondControllerSlot(context: Context) {
        openSlot2PairingWithNewPin()
    }

    fun openSlot2PairingWithNewPin() {
        _pinCodeSlot2.value = generateRandom4DigitPin()
        _isSlot2PairingOpen.value = true
    }

    fun closeSlot2Pairing() {
        _isSlot2PairingOpen.value = false
    }

    fun refreshTvIpAddresses(): String = refreshLocalIpAddress()

    fun refreshLocalIpAddress(): String {
        val ip = detectBestLocalIpv4()
        _tvLocalIp.value = ip
        return ip
    }

    private fun detectBestLocalIpv4(): String {
        return try {
            val candidates = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            candidates.add(host)
                        }
                    }
                }
            }
            candidates.firstOrNull { it.startsWith("192.168.") }
                ?: candidates.firstOrNull()
                ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    /**
     * Updates the cached list of TV apps (with names, packageNames, and compact Base64 icons)
     * so any phone connecting to the TV immediately receives the TV's app catalog.
     */
    fun updateExportedTvAppsFromUnifiedList(apps: List<UnifiedLauncherApp>) {
        scope.launch {
            val exported = apps.take(40).map { app ->
                val iconB64 = encodeAppIconToCompactBase64(app)
                TvExportedAppInfo(
                    packageName = app.packageName,
                    title = app.title,
                    iconBase64 = iconB64,
                    isRnaWebApp = app.isRnaWebApp
                )
            }
            exportedTvAppsCache = exported
        }
    }

    private fun encodeAppIconToCompactBase64(app: UnifiedLauncherApp): String {
        return try {
            if (!app.customLogoUri.isNullOrBlank() && app.customLogoUri.startsWith("data:image")) {
                return app.customLogoUri
            }
            val bmp: Bitmap? = app.cachedBitmap?.asAndroidBitmap()
                ?: app.nativeIcon?.toBitmap(64, 64)
            if (bmp != null) {
                val scaled = Bitmap.createScaledBitmap(bmp, 56, 56, true)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 85, baos)
                "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Starts the TV-side TCP & UDP Controller Server if not already running.
     */
    fun ensureTvControllerServerStarted(context: Context) {
        appContextRef = WeakReference(context.applicationContext)
        refreshLocalIpAddress()
        if (isServerRunning) return
        isServerRunning = true

        // 1. Start UDP Discovery & Zero-Lag Gamepad Input Listener
        scope.launch {
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(UDP_PORT))
                }
                udpServerSocket = socket
                val buf = ByteArray(2048)
                while (isServerRunning && !socket.isClosed) {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    val senderIp = packet.address?.hostAddress ?: "127.0.0.1"
                    handleIncomingUdpPacket(socket, packet, senderIp, msg)
                }
            } catch (_: Exception) {
                isServerRunning = false
            }
        }

        // 2. Start TCP Pairing, Auto-Reconnect, App Catalog Sync & Text Input Server
        scope.launch {
            try {
                val server = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(TCP_PORT))
                }
                tcpServerSocket = server
                while (isServerRunning && !server.isClosed) {
                    val client = server.accept()
                    scope.launch {
                        handleTcpClient(client)
                    }
                }
            } catch (_: Exception) {
                isServerRunning = false
            }
        }
    }

    private fun handleIncomingUdpPacket(
        socket: DatagramSocket,
        packet: DatagramPacket,
        senderIp: String,
        msg: String
    ) {
        when {
            msg.startsWith("RNA_DISCOVER") -> {
                // Reply immediately so the phone discovers the TV IP automatically on Wi-Fi or Hotspot
                val localIp = refreshLocalIpAddress()
                val reply = "RNA_TV_HERE|$localIp|$TCP_PORT|$UDP_PORT"
                val replyBytes = reply.toByteArray(Charsets.UTF_8)
                val outPacket = DatagramPacket(replyBytes, replyBytes.size, packet.address, packet.port)
                runCatching { socket.send(outPacket) }
            }

            msg.startsWith("GP|") -> {
                // Zero-lag Gamepad frame: GP|<deviceId>|<slot>|<bitmask>|<lx>|<ly>|<rx>|<ry>|<lt>|<rt>|<hatX>|<hatY>
                val parts = msg.split("|")
                if (parts.size >= 12) {
                    val deviceId = parts[1]
                    val slot = parts[2].toIntOrNull()?.coerceIn(1, 2) ?: 1
                    val bitmask = parts[3].toIntOrNull() ?: 0
                    val lx = parts[4].toFloatOrNull() ?: 0f
                    val ly = parts[5].toFloatOrNull() ?: 0f
                    val rx = parts[6].toFloatOrNull() ?: 0f
                    val ry = parts[7].toFloatOrNull() ?: 0f
                    val lt = parts[8].toFloatOrNull() ?: 0f
                    val rt = parts[9].toFloatOrNull() ?: 0f
                    val hatX = parts[10].toFloatOrNull() ?: 0f
                    val hatY = parts[11].toFloatOrNull() ?: 0f

                    val frame = XboxGamepadFrame(
                        slot = slot,
                        buttonBitmask = bitmask,
                        axisX = lx.coerceIn(-1f, 1f),
                        axisY = ly.coerceIn(-1f, 1f),
                        axisZ = rx.coerceIn(-1f, 1f),
                        axisRZ = ry.coerceIn(-1f, 1f),
                        axisLTrigger = lt.coerceIn(0f, 1f),
                        axisRTrigger = rt.coerceIn(0f, 1f),
                        axisHatX = hatX.coerceIn(-1f, 1f),
                        axisHatY = hatY.coerceIn(-1f, 1f)
                    )
                    touchControllerActivity(deviceId, senderIp, "Xbox Gamepad #$slot Active")
                    dispatchSpoofedXboxGamepadFrameOnTv(frame)
                }
            }

            msg.startsWith("RM|") -> {
                // Fast Remote Command over UDP: RM|<deviceId>|<command>|<arg>
                val parts = msg.split("|", limit = 4)
                if (parts.size >= 3) {
                    val deviceId = parts[1]
                    val cmd = parts[2]
                    val arg = parts.getOrNull(3).orEmpty()
                    touchControllerActivity(deviceId, senderIp, "Remote: $cmd")
                    executeRemoteCommandOnTv(cmd, arg)
                }
            }
        }
    }

    private fun handleTcpClient(client: Socket) {
        try {
            client.soTimeout = 12000
            val senderIp = client.inetAddress?.hostAddress ?: "127.0.0.1"
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val writer = PrintWriter(client.getOutputStream(), true)

            val line = reader.readLine()?.trim() ?: run {
                client.close()
                return
            }

            val req = JSONObject(line)
            val type = req.optString("type")

            when (type) {
                "PAIR_WITH_PIN" -> {
                    val pin = req.optString("pin").trim()
                    val deviceId = req.optString("deviceId").ifBlank { UUID.randomUUID().toString() }
                    val deviceName = req.optString("deviceName").ifBlank { "RNA Phone" }.take(24)
                    val mode = RnaPhoneControllerMode.fromId(req.optString("mode"))

                    val assignedSlot = validatePinAndAssignSlot(pin, deviceId, deviceName, senderIp, mode)
                    if (assignedSlot != null) {
                        saveTrustedController(deviceId, deviceName)
                        val resp = buildSuccessHandshakeJson(assignedSlot)
                        writer.println(resp.toString())
                    } else {
                        val err = JSONObject().apply {
                            put("status", "ERROR")
                            put("message", if (_connectedControllers.value.size >= MAX_CONTROLLERS) {
                                "Maximum of 2 controllers already connected to TV."
                            } else {
                                "Invalid 4-digit code. Check the RNA Controller code in TV Devices."
                            })
                        }
                        writer.println(err.toString())
                    }
                }

                "AUTO_RECONNECT" -> {
                    val deviceId = req.optString("deviceId")
                    val deviceName = req.optString("deviceName").ifBlank { "RNA Phone" }.take(24)
                    val mode = RnaPhoneControllerMode.fromId(req.optString("mode"))

                    if (isTrustedController(deviceId)) {
                        val assignedSlot = assignOrReuseSlotForTrustedDevice(deviceId, deviceName, senderIp, mode)
                        if (assignedSlot != null) {
                            val resp = buildSuccessHandshakeJson(assignedSlot)
                            writer.println(resp.toString())
                        } else {
                            writer.println(JSONObject().apply {
                                put("status", "ERROR")
                                put("message", "2 controllers are already connected to TV.")
                            }.toString())
                        }
                    } else {
                        writer.println(JSONObject().apply {
                            put("status", "NEEDS_PIN")
                            put("message", "Enter the 4-digit RNA Controller code shown on TV.")
                        }.toString())
                    }
                }

                "UPDATE_CONTROLLER_INFO" -> {
                    val deviceId = req.optString("deviceId")
                    val deviceName = req.optString("deviceName").ifBlank { "RNA Phone" }.take(24)
                    val mode = RnaPhoneControllerMode.fromId(req.optString("mode"))
                    updateControllerNameAndMode(deviceId, deviceName, mode)
                    saveTrustedController(deviceId, deviceName)
                    val slot = _connectedControllers.value.firstOrNull { it.deviceId == deviceId }?.slot ?: 1
                    writer.println(JSONObject().apply {
                        put("status", "OK")
                        put("slot", slot)
                    }.toString())
                }

                "SEND_TEXT" -> {
                    val deviceId = req.optString("deviceId")
                    val text = req.optString("text")
                    touchControllerActivity(deviceId, senderIp, "Keyboard Text")
                    val injected = injectRemoteTextIfTextBoxOpen(text)
                    writer.println(JSONObject().apply {
                        put("status", if (injected) "SENT" else "IGNORED_NO_TEXTBOX")
                    }.toString())
                }

                "LAUNCH_APP" -> {
                    val deviceId = req.optString("deviceId")
                    val pkg = req.optString("packageName")
                    touchControllerActivity(deviceId, senderIp, "Launch App")
                    executeRemoteCommandOnTv("LAUNCH_APP", pkg)
                    writer.println(JSONObject().apply { put("status", "OK") }.toString())
                }

                "REMOTE_CMD" -> {
                    val deviceId = req.optString("deviceId")
                    val cmd = req.optString("cmd")
                    val arg = req.optString("arg")
                    touchControllerActivity(deviceId, senderIp, cmd)
                    executeRemoteCommandOnTv(cmd, arg)
                    writer.println(JSONObject().apply { put("status", "OK") }.toString())
                }

                "SUBMIT_GEMINI_API_KEY" -> {
                    val deviceId = req.optString("deviceId")
                    val apiKey = req.optString("apiKey").trim()
                    val modelId = req.optString("modelId").trim()
                    touchControllerActivity(deviceId, senderIp, "Submit Gemini API Key")
                    val ctx = appContextRef?.get()
                    if (ctx != null) {
                        RnaFreeAiRouter.saveUserGeminiApiKey(
                            context = ctx,
                            apiKey = apiKey,
                            autoActivate = apiKey.isNotBlank()
                        )
                        if (modelId.isNotBlank()) {
                            RnaFreeAiRouter.setSelectedGeminiModel(ctx, modelId)
                        }
                        mainHandler.post {
                            val msg = if (apiKey.isNotBlank()) {
                                "Google AI API Key received from Phone & activated!"
                            } else {
                                "Cleared custom API Key — switched to Free AI Fallback Chain."
                            }
                            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                    writer.println(JSONObject().apply { put("status", "OK") }.toString())
                }

                "DISCONNECT" -> {
                    val deviceId = req.optString("deviceId")
                    disconnectController(deviceId)
                    writer.println(JSONObject().apply { put("status", "OK") }.toString())
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { client.close() }
        }
    }

    private fun validatePinAndAssignSlot(
        pin: String,
        deviceId: String,
        deviceName: String,
        ipAddress: String,
        mode: RnaPhoneControllerMode
    ): Int? {
        val current = _connectedControllers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.deviceId == deviceId }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            current[existingIdx] = existing.copy(
                deviceName = deviceName,
                ipAddress = ipAddress,
                mode = mode,
                lastActiveAt = System.currentTimeMillis()
            )
            _connectedControllers.value = current
            return existing.slot
        }

        // Check if PIN matches Slot 1, Slot 2, or encoded TV IP octet
        val cleanPin = pin.trim()
        val tvOctet = _tvLocalIp.value.substringAfterLast(".", "").toIntOrNull()
        val pinOctet = cleanPin.takeLast(3).toIntOrNull()
        val matchesEncodedOctet = cleanPin.length == 4 && tvOctet != null && pinOctet == tvOctet
        val matchesSlot1 = (cleanPin == _pinCodeSlot1.value) || (matchesEncodedOctet && cleanPin.first() in '1'..'4')
        val matchesSlot2 = (cleanPin == _pinCodeSlot2.value) || (matchesEncodedOctet && cleanPin.first() in '5'..'9')
        if (!matchesSlot1 && !matchesSlot2 && !matchesEncodedOctet) return null

        if (current.size >= MAX_CONTROLLERS) return null

        val targetSlot = when {
            matchesSlot2 && current.none { it.slot == 2 } -> 2
            current.none { it.slot == 1 } -> 1
            current.none { it.slot == 2 } -> 2
            else -> return null
        }

        current.add(
            ConnectedPhoneController(
                deviceId = deviceId,
                deviceName = deviceName,
                ipAddress = ipAddress,
                slot = targetSlot,
                mode = mode
            )
        )
        _connectedControllers.value = current.sortedBy { it.slot }
        notifyWebViewGamepadConnected(targetSlot, true)
        return targetSlot
    }

    private fun assignOrReuseSlotForTrustedDevice(
        deviceId: String,
        deviceName: String,
        ipAddress: String,
        mode: RnaPhoneControllerMode
    ): Int? {
        val current = _connectedControllers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.deviceId == deviceId }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            current[existingIdx] = existing.copy(
                deviceName = deviceName,
                ipAddress = ipAddress,
                mode = mode,
                lastActiveAt = System.currentTimeMillis()
            )
            _connectedControllers.value = current
            notifyWebViewGamepadConnected(existing.slot, true)
            return existing.slot
        }

        if (current.size >= MAX_CONTROLLERS) return null
        val slot = if (current.none { it.slot == 1 }) 1 else 2
        current.add(
            ConnectedPhoneController(
                deviceId = deviceId,
                deviceName = deviceName,
                ipAddress = ipAddress,
                slot = slot,
                mode = mode
            )
        )
        _connectedControllers.value = current.sortedBy { it.slot }
        notifyWebViewGamepadConnected(slot, true)
        return slot
    }

    fun updateControllerNameAndMode(
        deviceId: String,
        deviceName: String,
        mode: RnaPhoneControllerMode
    ) {
        val current = _connectedControllers.value.toMutableList()
        val idx = current.indexOfFirst { it.deviceId == deviceId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(
                deviceName = deviceName.ifBlank { current[idx].deviceName }.take(24),
                mode = mode,
                lastActiveAt = System.currentTimeMillis()
            )
            _connectedControllers.value = current
        }
    }

    fun disconnectController(deviceId: String) {
        val current = _connectedControllers.value
        val removed = current.firstOrNull { it.deviceId == deviceId }
        _connectedControllers.value = current.filterNot { it.deviceId == deviceId }
        if (removed != null) {
            notifyWebViewGamepadConnected(removed.slot, false)
        }
    }

    private fun touchControllerActivity(deviceId: String, ipAddress: String, summary: String) {
        _lastActionStatus.value = summary
        val current = _connectedControllers.value.toMutableList()
        val idx = current.indexOfFirst { it.deviceId == deviceId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(
                ipAddress = ipAddress,
                lastActiveAt = System.currentTimeMillis(),
                lastInputSummary = summary
            )
            _connectedControllers.value = current
        }
    }

    private fun buildSuccessHandshakeJson(assignedSlot: Int): JSONObject {
        val appsArr = JSONArray()
        exportedTvAppsCache.forEach { app ->
            appsArr.put(
                JSONObject().apply {
                    put("packageName", app.packageName)
                    put("title", app.title)
                    put("iconBase64", app.iconBase64)
                    put("isRnaWebApp", app.isRnaWebApp)
                }
            )
        }
        val currentVolumePercent = getTvCurrentVolumePercent()
        return JSONObject().apply {
            put("status", "OK")
            put("slot", assignedSlot)
            put("udpPort", UDP_PORT)
            put("volumePercent", currentVolumePercent)
            put("apps", appsArr)
        }
    }

    private fun getTvCurrentVolumePercent(): Int {
        val ctx = appContextRef?.get() ?: return 50
        return try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            ((cur.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            50
        }
    }

    private fun saveTrustedController(deviceId: String, deviceName: String) {
        val ctx = appContextRef?.get() ?: return
        val prefs = ctx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val map = loadTrustedControllersMap(prefs).toMutableMap()
        map[deviceId] = deviceName
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(PREF_TRUSTED_CONTROLLERS_JSON, obj.toString()).apply()
    }

    private fun isTrustedController(deviceId: String): Boolean {
        if (deviceId.isBlank()) return false
        val ctx = appContextRef?.get() ?: return false
        val prefs = ctx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        return loadTrustedControllersMap(prefs).containsKey(deviceId)
    }

    private fun loadTrustedControllersMap(prefs: SharedPreferences): Map<String, String> {
        val raw = prefs.getString(PREF_TRUSTED_CONTROLLERS_JSON, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val out = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optString(k, "RNA Phone")
            }
            out
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // TV REMOTE COMMAND EXECUTION (D-Pad, Volume Slider, Recents, Taskbar, AI, Apps)
    // ══════════════════════════════════════════════════════════════════════════════

    fun executeRemoteCommandOnTv(cmd: String, arg: String = "") {
        mainHandler.post {
            val ctx = appContextRef?.get()
            val fgAct = activeForegroundActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
            val service = RnaHomeInterceptorService.instance
            when (cmd) {
                "DPAD_UP" -> dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_UP)
                "DPAD_DOWN" -> dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_DOWN)
                "DPAD_LEFT" -> dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_LEFT)
                "DPAD_RIGHT" -> dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT)
                "DPAD_OK" -> dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_CENTER)
                "BACK" -> dispatchTvKeyCode(KeyEvent.KEYCODE_BACK)
                "HOME" -> {
                    KeyRemapperBridge.notifyNavigateToHomeTabInMainActivity()
                    if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                        onRemoteNavigateHome?.invoke()
                    } else if (service != null) {
                        service.launchRnaTvHomeFromService()
                    } else {
                        onRemoteNavigateHome?.invoke()
                    }
                }
                "RECENT_APPS" -> {
                    if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                        onRemoteOpenRecentApps?.invoke()
                    } else if (service != null) {
                        service.launchRecentAppsOverlayFromService()
                    } else {
                        onRemoteOpenRecentApps?.invoke()
                    }
                }
                "TASKBAR" -> {
                    if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                        onRemoteOpenTaskbar?.invoke()
                    } else if (service != null) {
                        service.launchTaskbarOverlayFromService()
                    } else {
                        onRemoteOpenTaskbar?.invoke()
                    }
                }
                "RNA_AI_BAR" -> {
                    if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                        onRemoteOpenAiBar?.invoke()
                    } else if (service != null) {
                        service.launchAiBarOverlayFromService()
                    } else {
                        onRemoteOpenAiBar?.invoke()
                    }
                }
                "SET_VOLUME" -> {
                    val pct = arg.toIntOrNull()?.coerceIn(0, 100) ?: return@post
                    if (ctx != null) {
                        try {
                            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val targetVol = ((pct / 100f) * max).toInt().coerceIn(0, max)
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        } catch (_: Exception) {
                        }
                    }
                }
                "LAUNCH_APP" -> {
                    if (arg.isNotBlank()) {
                        if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                            onRemoteLaunchAppByPackage?.invoke(arg)
                        } else if (service != null && service.launchAppPackageFromService(arg)) {
                            // Launched directly from privileged AccessibilityService context over external app
                        } else {
                            onRemoteLaunchAppByPackage?.invoke(arg)
                        }
                    }
                }
            }
        }
    }

    /**
     * Remaps a virtual keyCode into a real physical Android button press:
     * 1. If any RNA Activity (`MainActivity`, `WebContainerActivity`, `RnaAiActivity`, `RnaQuickOverlayActivity`,
     *    `RnaAssistantOverlayActivity`) is currently RESUMED in the foreground, dispatches genuine `KeyEvent`s
     *    directly to that foreground window (plus mapping Xbox A -> DPAD_CENTER and Xbox B/Select -> BACK).
     * 2. If an external Android app (YouTube, Netflix, Settings, games, etc.) is in the foreground,
     *    attempts low-level `InputManager.injectInputEvent` + background `input keyevent <keyCode>`, AND
     *    executes the real hardware action through `RnaHomeInterceptorService` (spatial focus search,
     *    `ACTION_FOCUS`/`ACTION_CLICK`, `GLOBAL_ACTION_BACK`, media keys, and gesture fallback).
     */
    private fun dispatchTvKeyCode(
        keyCode: Int,
        source: Int = InputDevice.SOURCE_DPAD or InputDevice.SOURCE_KEYBOARD,
        deviceId: Int = -1
    ) {
        val fgAct: Activity? = activeForegroundActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, deviceId, 0, KeyEvent.FLAG_FROM_SYSTEM, source)
        val up = KeyEvent(now, now + 18, KeyEvent.ACTION_UP, keyCode, 0, 0, deviceId, 0, KeyEvent.FLAG_FROM_SYSTEM, source)

        if (fgAct != null) {
            dispatchKeyPairToActivityWithRemoteFallback(fgAct, down, up)
            return
        }

        // External app is in the foreground:
        // 1. Try RNA Keyboard IME hardware KeyEvent injection (works in PPSSPP, RetroArch, TV Quick Actions Pro)
        com.example.RnaTvInputMethodService.instance?.injectHardwareKeyEvent(keyCode, isGamepad = false)
        // 2. Try InputManager reflection + non-blocking shell `input keyevent`
        injectInputEventViaInputManager(down)
        injectInputEventViaInputManager(up)
        dispatchShellInputKeyEventAsync(keyCode)

        // 3. Execute system-wide Accessibility hardware action / focus / click / back over the external app
        val handledByService = RnaHomeInterceptorService.instance
            ?.dispatchPhysicalKeyOrGamepadActionOverExternalApp(keyCode) == true
        if (!handledByService) {
            val cmdName = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> "d-pad.up"
                KeyEvent.KEYCODE_DPAD_DOWN -> "d-pad.down"
                KeyEvent.KEYCODE_DPAD_LEFT -> "d-pad.left"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "d-pad.right"
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A -> "d-pad.ok"
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_SELECT -> "d-pad.back"
                KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BUTTON_MODE -> "d-pad.home"
                else -> ""
            }
            if (cmdName.isNotEmpty()) {
                RnaAutonomousAgentController.onPerformGlobalKeyOrAction?.invoke(cmdName)
            }
        }
    }

    private fun dispatchKeyPairToActivityWithRemoteFallback(
        act: Activity,
        down: KeyEvent,
        up: KeyEvent
    ) {
        runCatching {
            val handledDown = act.window?.superDispatchKeyEvent(down) ?: false
            if (!handledDown) {
                act.dispatchKeyEvent(down)
            }
            val handledUp = act.window?.superDispatchKeyEvent(up) ?: false
            if (!handledUp) {
                act.dispatchKeyEvent(up)
            }

            // Also remap Xbox gamepad buttons to real Android remote buttons when inside standard UI views
            val mappedRemoteKey = when (down.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_1 -> KeyEvent.KEYCODE_DPAD_CENTER
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_2, KeyEvent.KEYCODE_BUTTON_SELECT -> KeyEvent.KEYCODE_BACK
                else -> null
            }
            if (mappedRemoteKey != null && !handledDown) {
                val now = SystemClock.uptimeMillis()
                val rDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, mappedRemoteKey, 0, 0, down.deviceId, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_DPAD)
                val rUp = KeyEvent(now, now + 15, KeyEvent.ACTION_UP, mappedRemoteKey, 0, 0, up.deviceId, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_DPAD)
                val rHandledDown = act.window?.superDispatchKeyEvent(rDown) ?: false
                if (!rHandledDown) act.dispatchKeyEvent(rDown)
                val rHandledUp = act.window?.superDispatchKeyEvent(rUp) ?: false
                if (!rHandledUp) act.dispatchKeyEvent(rUp)
            }
        }
    }

    private fun injectInputEventViaInputManager(event: android.view.InputEvent): Boolean {
        return try {
            val imClass = Class.forName("android.hardware.input.InputManager")
            val getInstance = imClass.getMethod("getInstance")
            val im = getInstance.invoke(null) ?: return false
            val injectMethod = imClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
            injectMethod.invoke(im, event, 0) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun dispatchShellInputKeyEventAsync(keyCode: Int) {
        scope.launch {
            runCatching {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "input keyevent $keyCode"))
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // KEYBOARD TEXT INJECTION (ONLY WHEN AN OPEN TEXT BOX IS DETECTED ON TV)
    // ══════════════════════════════════════════════════════════════════════════════

    fun injectRemoteTextIfTextBoxOpen(text: String): Boolean {
        if (text.isEmpty()) return false
        var wasHandled = false
        mainHandler.post {
            // 0. Check if RNA Keyboard IME has an active input connection
            if (com.example.RnaTvInputMethodService.instance?.commitRemoteText(text) == true) {
                wasHandled = true
                return@post
            }

            // 1. Check if a Compose text field inside RNA (e.g., Search / RNA AI) is actively focused
            val composeHandled = onRemoteInjectComposeText?.invoke(text) == true
            if (composeHandled) {
                wasHandled = true
                return@post
            }

            // 2. Check if an Android Accessibility editable node is focused
            val accTyped = RnaAutonomousAgentController.onPerformAccessibilityType?.invoke(text) == true
            if (accTyped) {
                wasHandled = true
                return@post
            }

            // 3. Check if an HTML <input>, <textarea>, or contentEditable is focused inside the active TV Browser WebView
            val webView = activeWebViewRef?.get()
            if (webView != null) {
                val escaped = JSONObject.quote(text)
                val js = """
                    (function() {
                        var el = document.activeElement;
                        if (!el) return false;
                        var tag = (el.tagName || '').toLowerCase();
                        var isInput = (tag === 'input' || tag === 'textarea' || el.isContentEditable);
                        if (!isInput) return false;
                        if (el.isContentEditable) {
                            el.textContent = $escaped;
                        } else {
                            var nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                                tag === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype,
                                'value'
                            );
                            if (nativeInputValueSetter && nativeInputValueSetter.set) {
                                nativeInputValueSetter.set.call(el, $escaped);
                            } else {
                                el.value = $escaped;
                            }
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                        return true;
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js, null)
                wasHandled = true
            }
            // If no text box is detected as open on TV, text is ignored per user specification!
        }
        return true
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // ANDROID XBOX CONTROLLER SPOOFER (PLAYER 1 = DEVICE 1001, PLAYER 2 = DEVICE 1002)
    // ══════════════════════════════════════════════════════════════════════════════

    fun dispatchSpoofedXboxGamepadFrameOnTv(rawFrame: XboxGamepadFrame) {
        val slot = rawFrame.slot.coerceIn(1, 2)
        // Ensure analog triggers LT/RT also set BIT_LT (KEYCODE_BUTTON_L2) and BIT_RT (KEYCODE_BUTTON_R2)
        var effectiveMask = rawFrame.buttonBitmask
        if (rawFrame.axisLTrigger > 0.45f) {
            effectiveMask = effectiveMask or (1 shl XboxButtonBits.BIT_LT)
        }
        if (rawFrame.axisRTrigger > 0.45f) {
            effectiveMask = effectiveMask or (1 shl XboxButtonBits.BIT_RT)
        }
        val newFrame = rawFrame.copy(slot = slot, buttonBitmask = effectiveMask)

        if (slot == 1) {
            _gamepadStateSlot1.value = newFrame
        } else {
            _gamepadStateSlot2.value = newFrame
        }

        val prevFrame = previousSlotFrames.put(slot, newFrame) ?: XboxGamepadFrame(slot = slot)
        val spoofedDeviceId = if (slot == 1) SPOOFED_XBOX_DEVICE_ID_SLOT_1 else SPOOFED_XBOX_DEVICE_ID_SLOT_2

        mainHandler.post {
            val ctx = appContextRef?.get()
            val fgAct = activeForegroundActivityRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
            val service = RnaHomeInterceptorService.instance
            val ime = com.example.RnaTvInputMethodService.instance
            val now = SystemClock.uptimeMillis()

            // 1. Diff all 17 Xbox buttons and emit BOTH their custom/Xbox keycode AND generic BUTTON_1..16 keycode!
            for ((bitIndex, defaultKeyCode) in XboxButtonBits.bitToKeyCode) {
                val wasDown = prevFrame.isButtonDown(bitIndex)
                val isDown = newFrame.isButtonDown(bitIndex)
                if (wasDown != isDown) {
                    val action = if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP

                    if (defaultKeyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
                        if (isDown) {
                            KeyRemapperBridge.notifyNavigateToHomeTabInMainActivity()
                            if (fgAct != null && fgAct.javaClass.simpleName == "MainActivity") {
                                onRemoteNavigateHome?.invoke()
                            } else if (service != null) {
                                service.launchRnaTvHomeFromService()
                            } else {
                                onRemoteNavigateHome?.invoke()
                            }
                        }
                        continue
                    }

                    val effectiveKeyCodes = RnaKeyLinksRegistry.getEffectiveKeyCodesForBit(ctx, bitIndex)
                    for (keyCode in effectiveKeyCodes) {
                        val source = if (keyCode in listOf(
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_DPAD_RIGHT
                            )
                        ) {
                            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD or InputDevice.SOURCE_JOYSTICK
                        } else {
                            InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_KEYBOARD
                        }

                        val keyEvent = KeyEvent(
                            now,
                            now,
                            action,
                            keyCode,
                            0,
                            0,
                            spoofedDeviceId,
                            0,
                            KeyEvent.FLAG_FROM_SYSTEM,
                            source
                        )

                        if (fgAct != null) {
                            runCatching {
                                val handled = fgAct.window?.superDispatchKeyEvent(keyEvent) ?: false
                                if (!handled) {
                                    fgAct.dispatchKeyEvent(keyEvent)
                                }
                                // Also remap primary Xbox A/B buttons to Android TV remote buttons for UI navigation
                                if (keyCode == effectiveKeyCodes.first()) {
                                    val mappedRemoteKey = when (defaultKeyCode) {
                                        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
                                        KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_SELECT -> KeyEvent.KEYCODE_BACK
                                        else -> null
                                    }
                                    if (mappedRemoteKey != null && !handled) {
                                        val remoteEv = KeyEvent(
                                            now,
                                            now,
                                            action,
                                            mappedRemoteKey,
                                            0,
                                            0,
                                            spoofedDeviceId,
                                            0,
                                            KeyEvent.FLAG_FROM_SYSTEM,
                                            InputDevice.SOURCE_DPAD
                                        )
                                        val rHandled = fgAct.window?.superDispatchKeyEvent(remoteEv) ?: false
                                        if (!rHandled) {
                                            fgAct.dispatchKeyEvent(remoteEv)
                                        }
                                    }
                                }
                            }
                        } else {
                            // External app (PPSSPP, RetroArch, TV Quick Actions Pro, YouTube, etc.):
                            // 1. Optional Keyboard IME hardware KeyEvent injection (when enabled in Control Panel / Key Link Editor)
                            if (isDown) {
                                ime?.injectHardwareKeyEvent(keyCode, isGamepad = true)
                            }
                            // 2. Low-level InputManager + shell keyevent injection
                            injectInputEventViaInputManager(keyEvent)
                            if (isDown) {
                                dispatchShellInputKeyEventAsync(keyCode)
                            }
                            // 3. Execute universal AccessibilityService action once per button press (on the primary keycode)
                            if (isDown && keyCode == effectiveKeyCodes.first()) {
                                service?.dispatchPhysicalKeyOrGamepadActionOverExternalApp(defaultKeyCode)
                            }
                        }
                    }
                }
            }

            // 2. Dispatch genuine Android Joystick MotionEvent when any Analog Stick, Trigger, or Hat Axis changes
            val axesChanged = abs(newFrame.axisX - prevFrame.axisX) > 0.005f ||
                abs(newFrame.axisY - prevFrame.axisY) > 0.005f ||
                abs(newFrame.axisZ - prevFrame.axisZ) > 0.005f ||
                abs(newFrame.axisRZ - prevFrame.axisRZ) > 0.005f ||
                abs(newFrame.axisLTrigger - prevFrame.axisLTrigger) > 0.005f ||
                abs(newFrame.axisRTrigger - prevFrame.axisRTrigger) > 0.005f ||
                abs(newFrame.axisHatX - prevFrame.axisHatX) > 0.005f ||
                abs(newFrame.axisHatY - prevFrame.axisHatY) > 0.005f

            if (axesChanged) {
                val props = arrayOf(
                    MotionEvent.PointerProperties().apply {
                        id = 0
                        toolType = MotionEvent.TOOL_TYPE_UNKNOWN
                    }
                )
                val coords = arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = newFrame.axisX
                        y = newFrame.axisY
                        setAxisValue(MotionEvent.AXIS_X, newFrame.axisX)
                        setAxisValue(MotionEvent.AXIS_Y, newFrame.axisY)
                        setAxisValue(MotionEvent.AXIS_Z, newFrame.axisZ)
                        setAxisValue(MotionEvent.AXIS_RZ, newFrame.axisRZ)
                        setAxisValue(MotionEvent.AXIS_LTRIGGER, newFrame.axisLTrigger)
                        setAxisValue(MotionEvent.AXIS_RTRIGGER, newFrame.axisRTrigger)
                        setAxisValue(MotionEvent.AXIS_HAT_X, newFrame.axisHatX)
                        setAxisValue(MotionEvent.AXIS_HAT_Y, newFrame.axisHatY)
                    }
                )
                val motionEvent = MotionEvent.obtain(
                    now,
                    now,
                    MotionEvent.ACTION_MOVE,
                    1,
                    props,
                    coords,
                    0,
                    0,
                    1.0f,
                    1.0f,
                    spoofedDeviceId,
                    0,
                    InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_GAMEPAD,
                    0
                )
                if (fgAct != null) {
                    runCatching {
                        val handled = fgAct.window?.superDispatchGenericMotionEvent(motionEvent) ?: false
                        if (!handled) {
                            fgAct.dispatchGenericMotionEvent(motionEvent)
                        }
                    }
                } else {
                    injectInputEventViaInputManager(motionEvent)
                }
                motionEvent.recycle()

                // Allow smooth D-Pad focus navigation via Left Analog Stick and gesture scrolling via Right Analog Stick across ALL apps
                val lastNav = lastAnalogNavTimestamp[slot] ?: 0L
                if (now - lastNav > 190L) {
                    when {
                        newFrame.axisY < -0.52f || newFrame.axisHatY < -0.5f -> {
                            lastAnalogNavTimestamp[slot] = now
                            dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_UP, InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD, spoofedDeviceId)
                        }
                        newFrame.axisY > 0.52f || newFrame.axisHatY > 0.5f -> {
                            lastAnalogNavTimestamp[slot] = now
                            dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_DOWN, InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD, spoofedDeviceId)
                        }
                        newFrame.axisX < -0.52f || newFrame.axisHatX < -0.5f -> {
                            lastAnalogNavTimestamp[slot] = now
                            dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_LEFT, InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD, spoofedDeviceId)
                        }
                        newFrame.axisX > 0.52f || newFrame.axisHatX > 0.5f -> {
                            lastAnalogNavTimestamp[slot] = now
                            dispatchTvKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT, InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_DPAD, spoofedDeviceId)
                        }
                        fgAct == null && (abs(newFrame.axisZ) > 0.45f || abs(newFrame.axisRZ) > 0.45f) -> {
                            lastAnalogNavTimestamp[slot] = now
                            service?.dispatchAnalogStickOverExternalApp(
                                newFrame.axisX,
                                newFrame.axisY,
                                newFrame.axisZ,
                                newFrame.axisRZ
                            )
                        }
                    }
                }
            }

            // 3. Sync both Xbox Controllers into the active WebView's W3C navigator.getGamepads() API
            syncXboxGamepadFrameToWebView(newFrame)
        }
    }

    private fun notifyWebViewGamepadConnected(slot: Int, connected: Boolean) {
        mainHandler.post {
            val wv = activeWebViewRef?.get() ?: return@post
            val idx = (slot - 1).coerceIn(0, 1)
            val js = """
                (function() {
                    if (window.__rnaUpdateGamepadConnection) {
                        window.__rnaUpdateGamepadConnection($idx, $connected);
                    }
                })();
            """.trimIndent()
            runCatching { wv.evaluateJavascript(js, null) }
        }
    }

    private fun syncXboxGamepadFrameToWebView(frame: XboxGamepadFrame) {
        val wv = activeWebViewRef?.get() ?: return
        val idx = (frame.slot - 1).coerceIn(0, 1)
        val js = """
            (function() {
                if (window.__rnaUpdateXboxFrame) {
                    window.__rnaUpdateXboxFrame(
                        $idx,
                        ${frame.buttonBitmask},
                        ${frame.axisX},
                        ${frame.axisY},
                        ${frame.axisZ},
                        ${frame.axisRZ},
                        ${frame.axisLTrigger},
                        ${frame.axisRTrigger},
                        ${frame.axisHatX},
                        ${frame.axisHatY}
                    );
                }
            })();
        """.trimIndent()
        runCatching { wv.evaluateJavascript(js, null) }
    }

    /**
     * JavaScript injected into TvBrowserTab & WebContainerActivity so HTML5 / Cloud Gaming / 2-Player Web Games
     * immediately recognize 1 or 2 physical Microsoft Xbox Wireless Controllers (`Vendor: 045e Product: 02fd`).
     */
    fun buildXboxGamepadSpoofJs(): String {
        return """
            (function() {
                if (window.__rnaXboxGamepadSpooferInstalled) return;
                window.__rnaXboxGamepadSpooferInstalled = true;

                function createVirtualXboxPad(index) {
                    var btns = [];
                    for (var i = 0; i < 17; i++) {
                        btns.push({ pressed: false, touched: false, value: 0.0 });
                    }
                    return {
                        id: index === 0
                            ? "Xbox Wireless Controller (STANDARD GAMEPAD Vendor: 045e Product: 02fd)"
                            : "Xbox Wireless Controller #2 (STANDARD GAMEPAD Vendor: 045e Product: 02fd)",
                        index: index,
                        connected: false,
                        timestamp: performance.now(),
                        mapping: "standard",
                        axes: [0.0, 0.0, 0.0, 0.0],
                        buttons: btns
                    };
                }

                var virtualPads = [createVirtualXboxPad(0), createVirtualXboxPad(1)];

                window.__rnaUpdateGamepadConnection = function(index, isConnected) {
                    var pad = virtualPads[index];
                    if (!pad) return;
                    var changed = (pad.connected !== isConnected);
                    pad.connected = isConnected;
                    pad.timestamp = performance.now();
                    if (changed) {
                        try {
                            var ev = new Event(isConnected ? 'gamepadconnected' : 'gamepaddisconnected');
                            ev.gamepad = pad;
                            window.dispatchEvent(ev);
                        } catch (e) {}
                    }
                };

                window.__rnaUpdateXboxFrame = function(index, mask, lx, ly, rx, ry, lt, rt, hatX, hatY) {
                    var pad = virtualPads[index];
                    if (!pad) return;
                    if (!pad.connected) {
                        window.__rnaUpdateGamepadConnection(index, true);
                    }
                    pad.timestamp = performance.now();
                    pad.axes[0] = lx;
                    pad.axes[1] = ly;
                    pad.axes[2] = rx;
                    pad.axes[3] = ry;

                    function setBtn(btnIdx, pressed, val) {
                        pad.buttons[btnIdx].pressed = pressed;
                        pad.buttons[btnIdx].touched = pressed;
                        pad.buttons[btnIdx].value = val !== undefined ? val : (pressed ? 1.0 : 0.0);
                    }

                    setBtn(0, (mask & (1 << 0)) !== 0); // A
                    setBtn(1, (mask & (1 << 1)) !== 0); // B
                    setBtn(2, (mask & (1 << 2)) !== 0); // X
                    setBtn(3, (mask & (1 << 3)) !== 0); // Y
                    setBtn(4, (mask & (1 << 4)) !== 0); // LB
                    setBtn(5, (mask & (1 << 5)) !== 0); // RB
                    setBtn(6, lt > 0.15, lt);           // LT Analog
                    setBtn(7, rt > 0.15, rt);           // RT Analog
                    setBtn(8, (mask & (1 << 9)) !== 0); // Back / View (SELECT)
                    setBtn(9, (mask & (1 << 8)) !== 0); // Start / Menu
                    setBtn(10, (mask & (1 << 6)) !== 0); // L3 (THUMBL)
                    setBtn(11, (mask & (1 << 7)) !== 0); // R3 (THUMBR)
                    setBtn(12, (mask & (1 << 11)) !== 0 || hatY < -0.5); // D-Pad Up
                    setBtn(13, (mask & (1 << 12)) !== 0 || hatY > 0.5);  // D-Pad Down
                    setBtn(14, (mask & (1 << 13)) !== 0 || hatX < -0.5); // D-Pad Left
                    setBtn(15, (mask & (1 << 14)) !== 0 || hatX > 0.5);  // D-Pad Right
                    setBtn(16, (mask & (1 << 10)) !== 0); // Guide (RNA Button)
                };

                var origGetGamepads = navigator.getGamepads ? navigator.getGamepads.bind(navigator) : null;
                navigator.getGamepads = function() {
                    var nativePads = origGetGamepads ? Array.from(origGetGamepads() || []) : [];
                    var out = [null, null, null, null];
                    if (virtualPads[0].connected) out[0] = virtualPads[0];
                    else if (nativePads[0]) out[0] = nativePads[0];

                    if (virtualPads[1].connected) out[1] = virtualPads[1];
                    else if (nativePads[1]) out[1] = nativePads[1];

                    return out;
                };
            })();
        """.trimIndent()
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PHONE-SIDE CLIENT ENGINE (DISCOVERY, AUTO-RECONNECT, 4-DIGIT PIN & ZERO-LAG UDP)
    // ══════════════════════════════════════════════════════════════════════════════

    private var clientUdpSocket: DatagramSocket? = null

    fun getOrCreatePhoneDeviceId(prefs: SharedPreferences): String {
        val existing = prefs.getString(PREF_PHONE_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = "rna_phone_" + UUID.randomUUID().toString().take(8)
        prefs.edit().putString(PREF_PHONE_DEVICE_ID, created).apply()
        return created
    }

    fun getSavedPhoneControllerName(prefs: SharedPreferences): String {
        return prefs.getString(PREF_PHONE_CONTROLLER_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: android.os.Build.MODEL.take(16).ifBlank { "RNA Phone" }
    }

    fun savePhoneControllerName(prefs: SharedPreferences, name: String) {
        prefs.edit().putString(PREF_PHONE_CONTROLLER_NAME, name.trim().take(24)).apply()
    }

    fun getSavedTvIp(prefs: SharedPreferences): String? {
        return prefs.getString(PREF_PHONE_LAST_TV_IP, null)?.takeIf { it.isNotBlank() }
    }

    fun saveLastTvIp(prefs: SharedPreferences, ip: String) {
        prefs.edit().putString(PREF_PHONE_LAST_TV_IP, ip).apply()
    }

    fun loadPhoneShortcutApps(prefs: SharedPreferences): List<TvExportedAppInfo> {
        val raw = prefs.getString(PREF_PHONE_SHORTCUT_APPS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<TvExportedAppInfo>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    TvExportedAppInfo(
                        packageName = o.optString("packageName"),
                        title = o.optString("title"),
                        iconBase64 = o.optString("iconBase64"),
                        isRnaWebApp = o.optBoolean("isRnaWebApp", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun savePhoneShortcutApps(prefs: SharedPreferences, apps: List<TvExportedAppInfo>) {
        val arr = JSONArray()
        apps.forEach { app ->
            arr.put(
                JSONObject().apply {
                    put("packageName", app.packageName)
                    put("title", app.title)
                    put("iconBase64", app.iconBase64)
                    put("isRnaWebApp", app.isRnaWebApp)
                }
            )
        }
        prefs.edit().putString(PREF_PHONE_SHORTCUT_APPS_JSON, arr.toString()).apply()
    }

    private fun getAllLocalSubnetPrefixes(): List<String> {
        val prefixes = mutableSetOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                if (!nif.isUp || nif.isLoopback) continue
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        val prefix = host.substringBeforeLast(".", "")
                        if (prefix.isNotEmpty() && prefix != "127.0.0") {
                            prefixes.add(prefix)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        // Also include standard Android Wi-Fi Hotspot, Wi-Fi Direct, and home router subnets
        prefixes.addAll(
            listOf(
                "192.168.43",
                "192.168.49",
                "192.168.1",
                "192.168.0",
                "192.168.2",
                "10.0.0",
                "172.20.10"
            )
        )
        return prefixes.toList()
    }

    /**
     * Discovers the RNA TV on the same Wi-Fi network, phone hotspot, or local loopback.
     * When a 4-digit PIN is provided (`pinHint`), the last 3 digits encode the TV's exact IP host octet
     * (`001..254`), allowing direct connection to `<subnet>.<octet>` even when routers block UDP broadcasts!
     */
    suspend fun discoverTvIpOnNetwork(savedIp: String?, pinHint: String? = null): String? = withContext(Dispatchers.IO) {
        val allSubnets = getAllLocalSubnetPrefixes()

        // 0. If the user entered a 4-digit PIN, extract the encoded last IP octet (last 3 digits) and probe it FIRST!
        val cleanPin = pinHint?.trim().orEmpty()
        val encodedOctet = if (cleanPin.length == 4) cleanPin.takeLast(3).toIntOrNull()?.takeIf { it in 1..254 } else null
        if (encodedOctet != null) {
            val octetCandidates = allSubnets.map { "$it.$encodedOctet" } + listOf("127.0.0.1", "10.0.2.2")
            val hitFromPin = coroutineScope {
                octetCandidates.distinct().map { ip ->
                    async {
                        if (isTvListeningAtIp(ip, timeoutMs = 850)) ip else null
                    }
                }.awaitAll().firstOrNull { it != null }
            }
            if (hitFromPin != null) return@withContext hitFromPin
        }

        // 1. If we have a saved IP (or loopback if testing on same device), probe it next
        val quickCandidates = listOfNotNull(savedIp, "127.0.0.1", "10.0.2.2").distinct()
        for (ip in quickCandidates) {
            if (isTvListeningAtIp(ip, timeoutMs = 400)) {
                return@withContext ip
            }
        }

        // 2. Broadcast UDP discovery packet on Wi-Fi & Hotspot broadcast addresses
        try {
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.soTimeout = 500
                val payload = "RNA_DISCOVER".toByteArray(Charsets.UTF_8)
                val broadcastTargets = (listOf("255.255.255.255") + allSubnets.map { "$it.255" }).distinct()
                for (bcast in broadcastTargets) {
                    runCatching {
                        val pkt = DatagramPacket(payload, payload.size, InetAddress.getByName(bcast), UDP_PORT)
                        sock.send(pkt)
                    }
                }
                val buf = ByteArray(512)
                val recv = DatagramPacket(buf, buf.size)
                sock.receive(recv)
                val text = String(recv.data, 0, recv.length, Charsets.UTF_8)
                if (text.startsWith("RNA_TV_HERE|")) {
                    val discoveredIp = recv.address?.hostAddress ?: text.split("|").getOrNull(1)
                    if (!discoveredIp.isNullOrBlank()) return@withContext discoveredIp
                }
            }
        } catch (_: Exception) {
        }

        // 3. Parallel fast TCP probe across all active local /24 subnets
        for (subnetPrefix in allSubnets.take(3)) {
            val found = coroutineScope {
                (1..254).chunked(42).firstNotNullOfOrNull { batch ->
                    batch.map { host ->
                        async {
                            val candidate = "$subnetPrefix.$host"
                            if (isTvListeningAtIp(candidate, timeoutMs = 260)) candidate else null
                        }
                    }.awaitAll().firstOrNull { it != null }
                }
            }
            if (found != null) return@withContext found
        }
        null
    }

    private fun isTvListeningAtIp(ip: String, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, TCP_PORT), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    data class PhoneHandshakeResult(
        val success: Boolean,
        val tvIp: String = "",
        val assignedSlot: Int = 1,
        val volumePercent: Int = 50,
        val tvApps: List<TvExportedAppInfo> = emptyList(),
        val errorMessage: String = "",
        val needsPin: Boolean = false
    )

    suspend fun attemptPhoneAutoReconnect(
        prefs: SharedPreferences,
        mode: RnaPhoneControllerMode
    ): PhoneHandshakeResult = withContext(Dispatchers.IO) {
        val savedIp = getSavedTvIp(prefs)
        val tvIp = discoverTvIpOnNetwork(savedIp, pinHint = null)
            ?: return@withContext PhoneHandshakeResult(
                success = false,
                needsPin = true,
                errorMessage = "Enter the 4-digit code from TV Devices > RNA Controller."
            )

        val deviceId = getOrCreatePhoneDeviceId(prefs)
        val deviceName = getSavedPhoneControllerName(prefs)
        val req = JSONObject().apply {
            put("type", "AUTO_RECONNECT")
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("mode", mode.id)
        }
        sendTcpHandshakeRequest(prefs, tvIp, req)
    }

    suspend fun pairPhoneWith4DigitPin(
        prefs: SharedPreferences,
        pin: String,
        controllerName: String,
        mode: RnaPhoneControllerMode
    ): PhoneHandshakeResult = withContext(Dispatchers.IO) {
        savePhoneControllerName(prefs, controllerName)
        val savedIp = getSavedTvIp(prefs)
        val tvIp = discoverTvIpOnNetwork(savedIp, pinHint = pin)
            ?: return@withContext PhoneHandshakeResult(
                success = false,
                errorMessage = "Could not find RNA TV on Wi-Fi / Hotspot. Make sure TV Devices > RNA Controller is open on the same network."
            )

        val deviceId = getOrCreatePhoneDeviceId(prefs)
        val req = JSONObject().apply {
            put("type", "PAIR_WITH_PIN")
            put("pin", pin.trim())
            put("deviceId", deviceId)
            put("deviceName", controllerName.trim().ifBlank { "RNA Phone" })
            put("mode", mode.id)
        }
        sendTcpHandshakeRequest(prefs, tvIp, req)
    }

    private fun sendTcpHandshakeRequest(
        prefs: SharedPreferences,
        tvIp: String,
        req: JSONObject
    ): PhoneHandshakeResult {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(tvIp, TCP_PORT), 2500)
                socket.soTimeout = 4000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                writer.println(req.toString())
                val rawResp = reader.readLine() ?: return PhoneHandshakeResult(
                    success = false,
                    errorMessage = "Empty response from TV."
                )
                val json = JSONObject(rawResp)
                when (json.optString("status")) {
                    "OK" -> {
                        saveLastTvIp(prefs, tvIp)
                        val slot = json.optInt("slot", 1)
                        val vol = json.optInt("volumePercent", 50)
                        val appsArr = json.optJSONArray("apps") ?: JSONArray()
                        val parsedApps = mutableListOf<TvExportedAppInfo>()
                        for (i in 0 until appsArr.length()) {
                            val o = appsArr.optJSONObject(i) ?: continue
                            parsedApps.add(
                                TvExportedAppInfo(
                                    packageName = o.optString("packageName"),
                                    title = o.optString("title"),
                                    iconBase64 = o.optString("iconBase64"),
                                    isRnaWebApp = o.optBoolean("isRnaWebApp", false)
                                )
                            )
                        }
                        PhoneHandshakeResult(
                            success = true,
                            tvIp = tvIp,
                            assignedSlot = slot,
                            volumePercent = vol,
                            tvApps = parsedApps
                        )
                    }
                    "NEEDS_PIN" -> PhoneHandshakeResult(
                        success = false,
                        tvIp = tvIp,
                        needsPin = true,
                        errorMessage = json.optString("message", "Enter 4-digit PIN.")
                    )
                    else -> PhoneHandshakeResult(
                        success = false,
                        tvIp = tvIp,
                        errorMessage = json.optString("message", "Connection refused.")
                    )
                }
            }
        } catch (e: Exception) {
            PhoneHandshakeResult(
                success = false,
                errorMessage = "Could not connect to TV (${e.localizedMessage ?: "Network error"})."
            )
        }
    }

    /**
     * Streams a zero-lag UDP Xbox Gamepad frame from Phone to TV.
     */
    fun sendPhoneGamepadFrameUdp(
        tvIp: String,
        deviceId: String,
        frame: XboxGamepadFrame
    ) {
        if (tvIp.isBlank()) return
        scope.launch {
            try {
                val sock = clientUdpSocket ?: DatagramSocket().also { clientUdpSocket = it }
                val msg = buildString {
                    append("GP|")
                    append(deviceId).append("|")
                    append(frame.slot).append("|")
                    append(frame.buttonBitmask).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisX)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisY)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisZ)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisRZ)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisLTrigger)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisRTrigger)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisHatX)).append("|")
                    append(String.format(java.util.Locale.US, "%.3f", frame.axisHatY))
                }
                val bytes = msg.toByteArray(Charsets.UTF_8)
                val pkt = DatagramPacket(bytes, bytes.size, InetAddress.getByName(tvIp), UDP_PORT)
                sock.send(pkt)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Sends a fast remote command (D-Pad, Home, Back, Volume, Recents, Taskbar, AI Bar) from Phone to TV.
     */
    fun sendPhoneRemoteCommand(
        tvIp: String,
        deviceId: String,
        cmd: String,
        arg: String = ""
    ) {
        if (tvIp.isBlank()) return
        scope.launch {
            try {
                val sock = clientUdpSocket ?: DatagramSocket().also { clientUdpSocket = it }
                val msg = "RM|$deviceId|$cmd|$arg"
                val bytes = msg.toByteArray(Charsets.UTF_8)
                val pkt = DatagramPacket(bytes, bytes.size, InetAddress.getByName(tvIp), UDP_PORT)
                sock.send(pkt)
            } catch (_: Exception) {
            }
        }
    }

    fun sendPhoneTcpControlPacket(
        tvIp: String,
        payload: JSONObject,
        onResult: ((JSONObject?) -> Unit)? = null
    ) {
        if (tvIp.isBlank()) return
        scope.launch {
            val resp = try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(tvIp, TCP_PORT), 2000)
                    s.soTimeout = 2500
                    val writer = PrintWriter(s.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                    writer.println(payload.toString())
                    val line = reader.readLine()
                    if (!line.isNullOrBlank()) JSONObject(line) else null
                }
            } catch (_: Exception) {
                null
            }
            if (onResult != null) {
                mainHandler.post { onResult(resp) }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // PHONE UI CONVENIENCE HELPERS (USED BY RnaPhoneControllerScreen.kt)
    // ══════════════════════════════════════════════════════════════════════════════

    private fun getPhonePrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("rna_controller_prefs", Context.MODE_PRIVATE)
    }

    fun getSavedPhoneControllerName(context: Context): String {
        return getSavedPhoneControllerName(getPhonePrefs(context))
    }

    fun attemptPhoneAutoReconnect(context: Context) {
        val prefs = getPhonePrefs(context)
        activePhoneDeviceId = getOrCreatePhoneDeviceId(prefs)
        scope.launch {
            val res = attemptPhoneAutoReconnect(prefs, RnaPhoneControllerMode.REMOTE)
            if (res.success) {
                activePhoneTvIp = res.tvIp
                _phoneAssignedSlot.value = res.assignedSlot
                _phoneTvVolumePercent.value = res.volumePercent
                if (res.tvApps.isNotEmpty()) {
                    _phoneAvailableTvApps.value = res.tvApps
                } else if (exportedTvAppsCache.isNotEmpty()) {
                    _phoneAvailableTvApps.value = exportedTvAppsCache
                }
                _phoneConnectedState.value = true
                _phoneConnectionStatusMessage.value = "Auto-connected to RNA TV (${res.tvIp})"
            }
        }
    }

    fun pairPhoneWithPin(
        context: Context,
        pinCode: String,
        controllerName: String,
        mode: String,
        optionalTargetIp: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val prefs = getPhonePrefs(context)
        activePhoneDeviceId = getOrCreatePhoneDeviceId(prefs)
        savePhoneControllerName(prefs, controllerName)
        if (!optionalTargetIp.isNullOrBlank()) {
            saveLastTvIp(prefs, optionalTargetIp.trim())
        }
        val resolvedMode = if (mode == "CONTROLLER") RnaPhoneControllerMode.XBOX_GAMEPAD else RnaPhoneControllerMode.REMOTE
        scope.launch {
            // Ensure local server is also started if pairing on the same device
            ensureTvControllerServerStarted(context)
            val res = pairPhoneWith4DigitPin(prefs, pinCode, controllerName, resolvedMode)
            if (res.success) {
                activePhoneTvIp = res.tvIp
                _phoneAssignedSlot.value = res.assignedSlot
                _phoneTvVolumePercent.value = res.volumePercent
                if (res.tvApps.isNotEmpty()) {
                    _phoneAvailableTvApps.value = res.tvApps
                } else if (exportedTvAppsCache.isNotEmpty()) {
                    _phoneAvailableTvApps.value = exportedTvAppsCache
                }
                _phoneConnectedState.value = true
                _phoneConnectionStatusMessage.value = "Connected as Player ${res.assignedSlot}"
                mainHandler.post { onComplete(true, "Connected") }
            } else {
                _phoneConnectionStatusMessage.value = res.errorMessage
                mainHandler.post { onComplete(false, res.errorMessage) }
            }
        }
    }

    fun updatePhoneModeAndName(
        context: Context,
        controllerName: String,
        mode: String
    ) {
        val prefs = getPhonePrefs(context)
        savePhoneControllerName(prefs, controllerName)
        val devId = activePhoneDeviceId.ifBlank { getOrCreatePhoneDeviceId(prefs) }
        val resolvedMode = if (mode == "CONTROLLER") RnaPhoneControllerMode.XBOX_GAMEPAD else RnaPhoneControllerMode.REMOTE
        val tvIp = activePhoneTvIp.ifBlank { getSavedTvIp(prefs).orEmpty() }
        if (tvIp.isNotBlank()) {
            sendPhoneTcpControlPacket(
                tvIp = tvIp,
                payload = JSONObject().apply {
                    put("type", "UPDATE_CONTROLLER_INFO")
                    put("deviceId", devId)
                    put("deviceName", controllerName)
                    put("mode", resolvedMode.id)
                }
            )
        }
    }

    fun disconnectPhoneFromTv() {
        val tvIp = activePhoneTvIp
        val devId = activePhoneDeviceId
        if (tvIp.isNotBlank() && devId.isNotBlank()) {
            sendPhoneTcpControlPacket(
                tvIp = tvIp,
                payload = JSONObject().apply {
                    put("type", "DISCONNECT")
                    put("deviceId", devId)
                }
            )
        }
        _phoneConnectedState.value = false
        _phoneConnectionStatusMessage.value = "Disconnected from TV"
    }

    fun sendRemoteCommandFromPhone(cmd: String, arg: String = "") {
        val tvIp = activePhoneTvIp.ifBlank { "127.0.0.1" }
        val devId = activePhoneDeviceId.ifBlank { "rna_phone_local" }
        sendPhoneRemoteCommand(tvIp, devId, cmd, arg)
    }

    fun sendVolumeSliderFromPhone(volumePercent: Int) {
        _phoneTvVolumePercent.value = volumePercent.coerceIn(0, 100)
        sendRemoteCommandFromPhone("SET_VOLUME", volumePercent.coerceIn(0, 100).toString())
    }

    fun sendLaunchTvAppFromPhone(packageName: String) {
        if (packageName.isBlank()) return
        sendRemoteCommandFromPhone("LAUNCH_APP", packageName)
    }

    fun sendTextToTvOpenTextBox(text: String) {
        if (text.isEmpty()) return
        val tvIp = activePhoneTvIp.ifBlank { "127.0.0.1" }
        val devId = activePhoneDeviceId.ifBlank { "rna_phone_local" }
        sendPhoneTcpControlPacket(
            tvIp = tvIp,
            payload = JSONObject().apply {
                put("type", "SEND_TEXT")
                put("deviceId", devId)
                put("text", text)
            }
        )
    }

    fun sendGeminiApiKeyFromPhone(apiKey: String, modelId: String = "gemini-2.5-flash") {
        val tvIp = activePhoneTvIp.ifBlank { "127.0.0.1" }
        val devId = activePhoneDeviceId.ifBlank { "rna_phone_local" }
        sendPhoneTcpControlPacket(
            tvIp = tvIp,
            payload = JSONObject().apply {
                put("type", "SUBMIT_GEMINI_API_KEY")
                put("deviceId", devId)
                put("apiKey", apiKey.trim())
                put("modelId", modelId.trim())
            }
        )
    }

    fun sendXboxButtonEventImmediate(keyCode: Int, isDown: Boolean) {
        val bitIndex = XboxButtonBits.bitToKeyCode.entries.firstOrNull { it.value == keyCode }?.key ?: return
        activePhoneButtonBitmask = if (isDown) {
            activePhoneButtonBitmask or (1 shl bitIndex)
        } else {
            activePhoneButtonBitmask and (1 shl bitIndex).inv()
        }
    }

    fun sendXboxGamepadFrameUdp(
        lx: Float,
        ly: Float,
        rx: Float,
        ry: Float,
        lt: Float,
        rt: Float,
        hatX: Float,
        hatY: Float,
        buttonsMask: Int
    ) {
        val tvIp = activePhoneTvIp.ifBlank { "127.0.0.1" }
        val devId = activePhoneDeviceId.ifBlank { "rna_phone_local" }
        val slot = _phoneAssignedSlot.value.coerceIn(1, 2)
        val combinedMask = buttonsMask or activePhoneButtonBitmask
        sendPhoneGamepadFrameUdp(
            tvIp = tvIp,
            deviceId = devId,
            frame = XboxGamepadFrame(
                slot = slot,
                buttonBitmask = combinedMask,
                axisX = lx,
                axisY = ly,
                axisZ = rx,
                axisRZ = ry,
                axisLTrigger = lt,
                axisRTrigger = rt,
                axisHatX = hatX,
                axisHatY = hatY
            )
        )
    }
}
