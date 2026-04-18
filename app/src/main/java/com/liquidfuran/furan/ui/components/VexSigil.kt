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
    // --- Rotation for WAITING (infinite) ---
    val infiniteTransition = rememberInfiniteTransition(label = "sigil")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // --- Pulse for APPROVED (infinite) ---
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
    // Use Animatable so the sequence runs once and returns to 0 correctly.
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == SigilState.DENIED) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0f at 0
                    -14f at 60
                    14f at 120
                    -10f at 200
                    10f at 280
                    -6f at 360
                    6f at 420
                    0f at 500
                }
            )
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    val activeRotation = if (state == SigilState.WAITING) rotationAngle else 0f
    val activeScale = if (state == SigilState.APPROVED) pulseScale else 1f
    val activeTx = shakeOffset.value

    val primaryColor = when (state) {
        SigilState.DENIED -> FuranColors.Magenta
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
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f * 0.92f

        rotate(activeRotation, pivot = Offset(cx, cy)) {
            translate(activeTx, 0f) {
                drawGlow(cx, cy, r, primaryColor, glowAlpha)
                drawOuterHexagon(cx, cy, r, primaryColor)
                drawMidHexagon(cx, cy, r * 0.72f, primaryColor)
                drawTriangles(cx, cy, r * 0.78f, primaryColor, secondaryColor)
                drawTickMarks(cx, cy, r, primaryColor, secondaryColor)
                drawCenterRing(cx, cy, r * 0.18f, primaryColor)
                drawCenterDot(cx, cy, r * 0.06f, secondaryColor)
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
    drawPath(hexPath(cx, cy, r, -30f), color = color.copy(alpha = 0.28f), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawMidHexagon(cx: Float, cy: Float, r: Float, color: Color) {
    drawPath(hexPath(cx, cy, r, 0f), color = color.copy(alpha = 0.18f), style = Stroke(width = 1f))
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

private fun DrawScope.drawTriangles(cx: Float, cy: Float, r: Float, cyanColor: Color, violetColor: Color) {
    drawPath(trianglePath(cx, cy, r, true), cyanColor.copy(alpha = 0.06f))
    drawPath(trianglePath(cx, cy, r, true), cyanColor.copy(alpha = 0.45f), style = Stroke(1.5f))
    drawPath(trianglePath(cx, cy, r, false), violetColor.copy(alpha = 0.06f))
    drawPath(trianglePath(cx, cy, r, false), violetColor.copy(alpha = 0.45f), style = Stroke(1.5f))
}

// ── Tick marks at hex vertices ────────────────────────────────────────────────

private fun DrawScope.drawTickMarks(cx: Float, cy: Float, r: Float, cyanColor: Color, violetColor: Color) {
    for (i in 0..5) {
        val angle = (i * 60 - 30) * PI.toFloat() / 180f
        val x1 = cx + r * 0.88f * cos(angle)
        val y1 = cy + r * 0.88f * sin(angle)
        val x2 = cx + r * cos(angle)
        val y2 = cy + r * sin(angle)
        val color = if (i % 2 == 0) cyanColor.copy(alpha = 0.7f) else violetColor.copy(alpha = 0.7f)
        drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

// ── Center ring + dot ────────────────────────────────────────────────────────

private fun DrawScope.drawCenterRing(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.3f), radius = r, center = Offset(cx, cy), style = Stroke(1f))
}

private fun DrawScope.drawCenterDot(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.25f), radius = r * 2.5f, center = Offset(cx, cy))
    drawCircle(color = color.copy(alpha = 0.5f), radius = r * 1.5f, center = Offset(cx, cy))
    drawCircle(color = color, radius = r, center = Offset(cx, cy))
}
