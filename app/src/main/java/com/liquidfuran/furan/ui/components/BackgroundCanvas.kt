package com.liquidfuran.furan.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.liquidfuran.furan.ui.theme.FuranColors

/**
 * Full-bleed background geometry layer.
 * Draws the Vex-network inspired line work behind all screen content.
 */
@Composable
fun FuranBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawDiagonalSlabs()
        drawPlatformLines()
        drawRisingDiagonals()
        drawVerticalColumns()
        drawCornerBrackets()
        drawDotGrid()
    }
}

private fun DrawScope.drawDiagonalSlabs() {
    val cyan4 = FuranColors.Cyan.copy(alpha = 0.04f)
    val strokeW = 1f
    val step = 80f
    var x = -size.height  // start off-screen left to cover the sweep
    while (x < size.width + size.height) {
        drawLine(
            color = cyan4,
            start = Offset(x, 0f),
            end = Offset(x + size.height * 0.6f, size.height),
            strokeWidth = strokeW
        )
        x += step
    }
}

private fun DrawScope.drawPlatformLines() {
    val cyan5 = FuranColors.Cyan.copy(alpha = 0.05f)
    val violet5 = FuranColors.Violet.copy(alpha = 0.04f)
    // Two floating horizontal platforms at 40% and 65% height
    val y1 = size.height * 0.4f
    val y2 = size.height * 0.65f
    val margin = size.width * 0.1f

    drawLine(color = cyan5, start = Offset(margin, y1), end = Offset(size.width * 0.7f, y1), strokeWidth = 1f)
    drawLine(color = violet5, start = Offset(size.width * 0.3f, y2), end = Offset(size.width - margin, y2), strokeWidth = 1f)
}

private fun DrawScope.drawRisingDiagonals() {
    val cyan4 = FuranColors.Cyan.copy(alpha = 0.04f)
    // Two lines rising from bottom-left
    drawLine(
        color = cyan4,
        start = Offset(0f, size.height),
        end = Offset(size.width * 0.35f, size.height * 0.3f),
        strokeWidth = 1f
    )
    drawLine(
        color = cyan4,
        start = Offset(0f, size.height),
        end = Offset(size.width * 0.55f, size.height * 0.5f),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawVerticalColumns() {
    val cyan4 = FuranColors.Cyan.copy(alpha = 0.035f)
    val violet4 = FuranColors.Violet.copy(alpha = 0.03f)
    val rightEdge = size.width * 0.88f
    val step = 18f
    var x = rightEdge
    var toggle = true
    while (x < size.width) {
        drawLine(
            color = if (toggle) cyan4 else violet4,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += step
        toggle = !toggle
    }
}

private fun DrawScope.drawCornerBrackets() {
    val cyan6 = FuranColors.Cyan.copy(alpha = 0.25f)
    val violet6 = FuranColors.Violet.copy(alpha = 0.25f)
    val len = 28f
    val pad = 20f
    val sw = 1.5f

    // Top-left bracket
    drawLine(color = cyan6, start = Offset(pad, pad), end = Offset(pad + len, pad), strokeWidth = sw)
    drawLine(color = cyan6, start = Offset(pad, pad), end = Offset(pad, pad + len), strokeWidth = sw)

    // Bottom-right bracket
    val bx = size.width - pad
    val by = size.height - pad
    drawLine(color = violet6, start = Offset(bx - len, by), end = Offset(bx, by), strokeWidth = sw)
    drawLine(color = violet6, start = Offset(bx, by - len), end = Offset(bx, by), strokeWidth = sw)
}

private fun DrawScope.drawDotGrid() {
    val cyan12 = FuranColors.Cyan.copy(alpha = 0.12f)
    val gridSize = 18f
    val cols = 5
    val rows = 5
    val startX = size.width - (cols + 1) * gridSize
    val startY = 60f
    for (col in 0 until cols) {
        for (row in 0 until rows) {
            drawCircle(
                color = cyan12,
                radius = 1.2f,
                center = Offset(startX + col * gridSize, startY + row * gridSize)
            )
        }
    }
}
