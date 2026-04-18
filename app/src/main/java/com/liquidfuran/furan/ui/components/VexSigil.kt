package com.liquidfuran.furan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.ui.theme.FuranColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VexSigil(
    state: SigilState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Rotation for WAITING ---
    val infiniteTransition = rememberInfiniteTransition(label = "sigil")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // --- Pulse for APPROVED ---
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // --- Shake for DENIED ---
    var shakeCounter by remember { mutableIntStateOf(0) }
    val shakeOffset by animateFloatAsState(
        targetValue = if (state == SigilState.DENIED) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 500
            0f at 0
            -12f at 60
            12f at 120
            -10f at 180
            10f at 240
            -6f at 320
            6f at 380
            0f at 500
        },
        label = "shake"
    )

    val activeRotation = if (state == SigilState.WAITING) rotationAngle else 0f
    val activeScale = if (state == SigilState.APPROVED) pulseScale else 1f
    val activeTx = if (state == SigilState.DENIED) shakeOffset else 0f

    // Color tints per state
    val primaryColor = when (state) {
        SigilState.DENIED -> FuranColors.Magenta
        SigilState.APPROVED -> FuranColors.Cyan
        else -> FuranColors.Cyan
    }
    val secondaryColor = when (state) {
        SigilState.DENIED -> FuranColors.Magenta.copy(alpha = 0.6f)
        else -> FuranColors.Violet
    }
    val glowAlpha = when (state) {
        SigilState.APPROVED -> 0.35f
        SigilState.WAITING -> 0.2f
        SigilState.IDLE -> 0.1f
        else -> 0.15f
    }

    Canvas(
        modifier = modifier
            .scale(activeScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        val cx = size.width / 2f + activeTx
        val cy = size.height / 2f
        val r = size.minDimension / 2f * 0.92f

        rotate(activeRotation, pivot = Offset(size.width / 2f, cy)) {
            translate(activeTx, 0f) {
                drawGlow(cx - activeTx, cy, r, primaryColor, glowAlpha)
                drawOuterHexagon(cx - activeTx, cy, r, primaryColor)
                drawMidHexagon(cx - activeTx, cy, r * 0.72f, primaryColor)
                drawTriangles(cx - activeTx, cy, r * 0.78f, primaryColor, secondaryColor)
                drawTickMarks(cx - activeTx, cy, r, primaryColor, secondaryColor)
                drawCenterRing(cx - activeTx, cy, r * 0.18f, primaryColor)
                drawCenterDot(cx - activeTx, cy, r * 0.06f, secondaryColor)
            }
        }
    }
}

// ── Glow halo ───────────────────────────────────────────────────────────────

private fun DrawScope.drawGlow(cx: Float, cy: Float, r: Float, color: Color, alpha: Float) {
    for (i in 3 downTo 1) {
        drawCircle(
            color = color.copy(alpha = alpha / i),
            radius = r * (0.5f + i * 0.12f),
            center = Offset(cx, cy)
        )
    }
}

// ── Hexagons ─────────────────────────────────────────────────────────────────

private fun hexPath(cx: Float, cy: Float, r: Float, rotationDeg: Float = -30f): Path {
    val path = Path()
    for (i in 0..5) {
        val angle = (i * 60 + rotationDeg) * PI.toFloat() / 180f
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawOuterHexagon(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(
        path = hexPath(cx, cy, r, -30f),
        color = color.copy(alpha = 0.28f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawMidHexagon(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(
        path = hexPath(cx, cy, r, 0f),
        color = color.copy(alpha = 0.18f),
        style = Stroke(width = 1f)
    )
}

// ── Triangles → 6-point star ─────────────────────────────────────────────────

private fun trianglePath(cx: Float, cy: Float, r: Float, pointUp: Boolean): Path {
    val path = Path()
    val offsetAngle = if (pointUp) -90f else 90f
    for (i in 0..2) {
        val angle = (i * 120 + offsetAngle) * PI.toFloat() / 180f
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawTriangles(
    cx: Float, cy: Float, r: Float,
    cyanColor: Color, violetColor: Color
) {
    // Upward triangle — cyan fill + stroke
    drawPath(trianglePath(cx, cy, r, true), cyanColor.copy(alpha = 0.06f))
    drawPath(trianglePath(cx, cy, r, true), cyanColor.copy(alpha = 0.45f), style = Stroke(1.5f))

    // Downward triangle — violet fill + stroke
    drawPath(trianglePath(cx, cy, r, false), violetColor.copy(alpha = 0.06f))
    drawPath(trianglePath(cx, cy, r, false), violetColor.copy(alpha = 0.45f), style = Stroke(1.5f))
}

// ── Tick marks at hex vertices ────────────────────────────────────────────────

private fun DrawScope.drawTickMarks(
    cx: Float, cy: Float, r: Float,
    cyanColor: Color, violetColor: Color
) {
    for (i in 0..5) {
        val angle = (i * 60 - 30) * PI.toFloat() / 180f
        val innerR = r * 0.88f
        val outerR = r * 1.0f
        val x1 = cx + innerR * cos(angle)
        val y1 = cy + innerR * sin(angle)
        val x2 = cx + outerR * cos(angle)
        val y2 = cy + outerR * sin(angle)
        val color = if (i % 2 == 0) cyanColor.copy(alpha = 0.7f) else violetColor.copy(alpha = 0.7f)
        drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

// ── Center ring + dot ────────────────────────────────────────────────────────

private fun DrawScope.drawCenterRing(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(1f)
    )
}

private fun DrawScope.drawCenterDot(cx: Float, cy: Float, r: Float, color: Color) {
    // Outer glow
    drawCircle(color = color.copy(alpha = 0.25f), radius = r * 2.5f, center = Offset(cx, cy))
    drawCircle(color = color.copy(alpha = 0.5f), radius = r * 1.5f, center = Offset(cx, cy))
    // Core
    drawCircle(color = color, radius = r, center = Offset(cx, cy))
}
