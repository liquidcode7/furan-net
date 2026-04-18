package com.liquidfuran.furan.ui.theme

import androidx.compose.ui.graphics.Color

object FuranColors {
    val Background = Color(0xFF04050F)   // Deep navy-black
    val Navy       = Color(0xFF070918)   // Slightly lighter navy
    val Cyan       = Color(0xFF00E5FF)   // Neon cyan — primary accent
    val Violet     = Color(0xFF9B5CFF)   // Violet — secondary accent
    val Magenta    = Color(0xFFFF3EF5)   // Magenta — error/denied state
    val White      = Color(0xFFE8F0FF)   // Off-white text
    val GridLine   = Color(0xFF00E5FF).copy(alpha = 0.018f)

    // Derived
    val CyanDim    = Cyan.copy(alpha = 0.4f)
    val VioletDim  = Violet.copy(alpha = 0.4f)
    val CyanGlow   = Cyan.copy(alpha = 0.15f)
    val VioletGlow = Violet.copy(alpha = 0.1f)
}
