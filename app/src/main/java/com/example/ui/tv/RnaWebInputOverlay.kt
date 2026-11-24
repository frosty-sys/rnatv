package com.example.ui.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.WebContainerActivity
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary

enum class RnaCursorDesign(val id: String, val label: String) {
    CYBER_RING("CYBER_RING", "Cyber Mouse"),
    CLASSIC_ARROW("CLASSIC_ARROW", "Classic Arrow"),
    NEON_CROSSHAIR("NEON_CROSSHAIR", "Tactical Crosshair"),
    MINIMAL_ORB("MINIMAL_ORB", "Minimal Glass Orb");

    companion object {
        fun fromId(id: String?): RnaCursorDesign =
            entries.firstOrNull { it.id == id } ?: CYBER_RING
    }
}

enum class RnaScrollWheelDesign(val id: String, val label: String) {
    GLASS_PILL_WHEEL("GLASS_PILL_WHEEL", "Glass Scroll Wheel"),
    DUAL_CHEVRON_DIAL("DUAL_CHEVRON_DIAL", "Rotary Chevron Dial"),
    CYBER_TRACK_BAR("CYBER_TRACK_BAR", "Cyber Dual-Axis Track"),
    MINIMAL_CAPSULE("MINIMAL_CAPSULE", "Minimal Tonal Scroller");

    companion object {
        fun fromId(id: String?): RnaScrollWheelDesign =
            entries.firstOrNull { it.id == id } ?: GLASS_PILL_WHEEL
    }
}

/**
 * Computes the next input mode when the user presses and holds OK:
 * - When defaultBaseMode == MODE_DPAD:
 *     DPAD -> (Hold OK) -> MOUSE -> (Hold OK) -> SCROLL -> (Hold OK) -> DPAD
 * - When defaultBaseMode == MODE_MOUSE:
 *     MOUSE -> (Hold OK) -> DPAD -> (Hold OK) -> SCROLL -> (Hold OK) -> MOUSE
 */
fun advanceInputModeOnHoldOk(currentMode: Int, defaultBaseMode: Int): Int {
    return if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) {
        when (currentMode) {
            WebContainerActivity.MODE_MOUSE -> WebContainerActivity.MODE_DPAD
            WebContainerActivity.MODE_DPAD -> WebContainerActivity.MODE_SCROLL
            else -> WebContainerActivity.MODE_MOUSE
        }
    } else {
        when (currentMode) {
            WebContainerActivity.MODE_DPAD -> WebContainerActivity.MODE_MOUSE
            WebContainerActivity.MODE_MOUSE -> WebContainerActivity.MODE_SCROLL
            else -> WebContainerActivity.MODE_DPAD
        }
    }
}

/**
 * Computes the previous input mode when the user presses Back:
 * Returns null if already at the base mode (meaning Back should navigate web history or exit).
 * - When defaultBaseMode == MODE_DPAD:
 *     SCROLL -> (Back) -> MOUSE -> (Back) -> DPAD -> (Back) -> null
 * - When defaultBaseMode == MODE_MOUSE:
 *     SCROLL -> (Back) -> DPAD -> (Back) -> MOUSE -> (Back) -> null
 */
fun stepBackInputModeOnBack(currentMode: Int, defaultBaseMode: Int): Int? {
    return if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) {
        when (currentMode) {
            WebContainerActivity.MODE_SCROLL -> WebContainerActivity.MODE_DPAD
            WebContainerActivity.MODE_DPAD -> WebContainerActivity.MODE_MOUSE
            else -> null
        }
    } else {
        when (currentMode) {
            WebContainerActivity.MODE_SCROLL -> WebContainerActivity.MODE_MOUSE
            WebContainerActivity.MODE_MOUSE -> WebContainerActivity.MODE_DPAD
            else -> null
        }
    }
}

/**
 * Renders the active Virtual Mouse Cursor when in `MODE_MOUSE`,
 * and renders the interactive Scroll Wheel HUD when in `MODE_SCROLL`.
 */
@Composable
fun RnaWebInputOverlay(
    inputMode: Int,
    defaultBaseMode: Int,
    cursorX: Float,
    cursorY: Float,
    isCursorPressed: Boolean,
    cursorDesign: RnaCursorDesign,
    scrollWheelDesign: RnaScrollWheelDesign,
    mouseSpeed: Float,
    modeBannerVisible: Boolean,
    activeScrollDirection: Int = 0, // KeyEvent.KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT or 0
    onScrollStepClick: (dx: Int, dy: Int) -> Unit = { _, _ -> }
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Virtual Mouse Cursor Overlay (Shown ONLY when inputMode == MODE_MOUSE)
        AnimatedVisibility(
            visible = inputMode == WebContainerActivity.MODE_MOUSE,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("rna_virtual_mouse_canvas")
            ) {
                when (cursorDesign) {
                    RnaCursorDesign.CLASSIC_ARROW -> {
                        val path = Path().apply {
                            moveTo(cursorX, cursorY)
                            lineTo(cursorX + 24f, cursorY + 18f)
                            lineTo(cursorX + 12f, cursorY + 19f)
                            lineTo(cursorX + 17f, cursorY + 31f)
                            lineTo(cursorX + 10f, cursorY + 33f)
                            lineTo(cursorX + 5f, cursorY + 21f)
                            lineTo(cursorX, cursorY + 26f)
                            close()
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF0A0C14),
                            style = Stroke(width = 5f)
                        )
                        drawPath(
                            path = path,
                            color = if (isCursorPressed) CyanPrimary else Color.White
                        )
                        drawCircle(
                            color = CyanPrimary.copy(alpha = if (isCursorPressed) 0.55f else 0.28f),
                            radius = if (isCursorPressed) 22f else 9f,
                            center = Offset(cursorX + 3f, cursorY + 3f)
                        )
                    }

                    RnaCursorDesign.CYBER_RING -> {
                        val outerRadius = if (isCursorPressed) 16f else 20f
                        drawCircle(
                            color = Color(0xCC0D101A),
                            radius = outerRadius + 2f,
                            center = Offset(cursorX, cursorY),
                            style = Stroke(width = 5f)
                        )
                        drawCircle(
                            color = CyanPrimary,
                            radius = outerRadius,
                            center = Offset(cursorX, cursorY),
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = if (isCursorPressed) EmeraldSuccess else Color.White,
                            radius = if (isCursorPressed) 6f else 4f,
                            center = Offset(cursorX, cursorY)
                        )
                    }

                    RnaCursorDesign.NEON_CROSSHAIR -> {
                        val arm = 18f
                        val gap = 5f
                        val c = if (isCursorPressed) EmeraldSuccess else CyanPrimary
                        // Dark shadow backing
                        drawLine(Color.Black, Offset(cursorX - arm, cursorY), Offset(cursorX - gap, cursorY), strokeWidth = 5f)
                        drawLine(Color.Black, Offset(cursorX + gap, cursorY), Offset(cursorX + arm, cursorY), strokeWidth = 5f)
                        drawLine(Color.Black, Offset(cursorX, cursorY - arm), Offset(cursorX, cursorY - gap), strokeWidth = 5f)
                        drawLine(Color.Black, Offset(cursorX, cursorY + gap), Offset(cursorX, cursorY + arm), strokeWidth = 5f)
                        // Foreground crosshair
                        drawLine(c, Offset(cursorX - arm, cursorY), Offset(cursorX - gap, cursorY), strokeWidth = 2.6f)
                        drawLine(c, Offset(cursorX + gap, cursorY), Offset(cursorX + arm, cursorY), strokeWidth = 2.6f)
                        drawLine(c, Offset(cursorX, cursorY - arm), Offset(cursorX, cursorY - gap), strokeWidth = 2.6f)
                        drawLine(c, Offset(cursorX, cursorY + gap), Offset(cursorX, cursorY + arm), strokeWidth = 2.6f)
                        drawCircle(Color.White, radius = 3f, center = Offset(cursorX, cursorY))
                    }

                    RnaCursorDesign.MINIMAL_ORB -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    CyanPrimary.copy(alpha = 0.85f),
                                    VioletSecondary.copy(alpha = 0.0f)
                                ),
                                center = Offset(cursorX, cursorY),
                                radius = if (isCursorPressed) 26f else 18f
                            ),
                            radius = if (isCursorPressed) 26f else 18f,
                            center = Offset(cursorX, cursorY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = Offset(cursorX, cursorY)
                        )
                    }
                }
            }
        }

        // 2. Visible Scroll Wheel Indicator Overlay (Shown ONLY when inputMode == MODE_SCROLL)
        AnimatedVisibility(
            visible = inputMode == WebContainerActivity.MODE_SCROLL,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180)),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.85f, animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
                .testTag("rna_scroll_wheel_overlay")
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "ScrollWheelPulse")
            val wheelPulse by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ScrollTickOffset"
            )

            when (scrollWheelDesign) {
                RnaScrollWheelDesign.GLASS_PILL_WHEEL -> {
                    Surface(
                        shape = RoundedCornerShape(36.dp),
                        color = Color(0xEE151824),
                        border = BorderStroke(2.dp, CyanPrimary),
                        shadowElevation = 18.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF232738),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onScrollStepClick(0, -260) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Scroll Up",
                                        tint = CyanPrimary
                                    )
                                }
                            }

                            // Center Animated Scroll Wheel Graphic
                            Canvas(modifier = Modifier.width(28.dp).height(64.dp)) {
                                drawRoundRect(
                                    color = Color(0xFF252A3D),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                    size = size
                                )
                                drawRoundRect(
                                    color = CyanPrimary.copy(alpha = 0.6f),
                                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                    size = size,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Animated inner scroll wheel notch
                                val centerY = size.height / 2f + wheelPulse
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(size.width * 0.28f, centerY - 10f),
                                    size = Size(size.width * 0.44f, 20f),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF232738),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onScrollStepClick(0, 260) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Scroll Down",
                                        tint = CyanPrimary
                                    )
                                }
                            }

                            Text(
                                text = "SCROLL",
                                color = CyanPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                RnaScrollWheelDesign.DUAL_CHEVRON_DIAL -> {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xEE161926),
                        border = BorderStroke(2.5.dp, VioletSecondary),
                        shadowElevation = 20.dp,
                        modifier = Modifier.size(132.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Scroll Up",
                                tint = VioletSecondary,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .size(26.dp)
                                    .clickable { onScrollStepClick(0, -260) }
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll Down",
                                tint = VioletSecondary,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                                    .size(26.dp)
                                    .clickable { onScrollStepClick(0, 260) }
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Scroll Left",
                                tint = Color(0xFFC6C5D0),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 8.dp)
                                    .size(22.dp)
                                    .clickable { onScrollStepClick(-220, 0) }
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Scroll Right",
                                tint = Color(0xFFC6C5D0),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .size(22.dp)
                                    .clickable { onScrollStepClick(220, 0) }
                            )
                            Surface(
                                shape = CircleShape,
                                color = VioletSecondary.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, VioletSecondary),
                                modifier = Modifier
                                    .size(52.dp)
                                    .align(Alignment.Center)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "WHEEL",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                RnaScrollWheelDesign.CYBER_TRACK_BAR -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xEE10141F),
                        border = BorderStroke(1.5.dp, EmeraldSuccess),
                        shadowElevation = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TRACK",
                                color = EmeraldSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .width(12.dp)
                                    .height(110.dp)
                                    .background(Color(0xFF1F2637), RoundedCornerShape(6.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(34.dp)
                                        .align(Alignment.Center)
                                        .background(EmeraldSuccess, RoundedCornerShape(6.dp))
                                )
                            }
                            Text(
                                text = "D-PAD",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                RnaScrollWheelDesign.MINIMAL_CAPSULE -> {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xDD1E202B),
                        border = BorderStroke(1.5.dp, AmberAccent),
                        shadowElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Scroll Active",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Use D-Pad ↑↓",
                                    color = AmberAccent,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bottom-Center Mode Status Pill (Clearly explains the 3-stage cycle & Back behavior)
        AnimatedVisibility(
            visible = modeBannerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        ) {
            val statusDescription = if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) {
                when (inputMode) {
                    WebContainerActivity.MODE_MOUSE -> "Mouse Mode (Default • ${String.format("%.1f", mouseSpeed)}x) • Hold OK for D-Pad Controls"
                    WebContainerActivity.MODE_DPAD -> "D-Pad Controls • Hold OK for Scroll Wheel • Press Back for Mouse"
                    else -> "Scroll Wheel Active • Press Back for D-Pad • Hold OK for Mouse"
                }
            } else {
                when (inputMode) {
                    WebContainerActivity.MODE_DPAD -> "D-Pad Controls (Default) • Hold OK for Mouse Cursor"
                    WebContainerActivity.MODE_MOUSE -> "Mouse Cursor Active (${String.format("%.1f", mouseSpeed)}x) • Hold OK for Scroll Wheel • Back for D-Pad"
                    else -> "Scroll Wheel Active • Press Back for Mouse • Back twice for D-Pad"
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xEE181A24),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.55f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (inputMode) {
                            WebContainerActivity.MODE_MOUSE -> Icons.Default.Mouse
                            WebContainerActivity.MODE_SCROLL -> Icons.Default.SwapVert
                            else -> Icons.Default.Tv
                        },
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = statusDescription,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
