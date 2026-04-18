package com.liquidfuran.furan.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * FURAN typography.
 *
 * Default: system fonts so the project builds immediately.
 *
 * To use the real typefaces, download the TTF files and place them in
 * app/src/main/res/font/, then swap the definitions below:
 *
 *   Orbitron (OFL): https://fonts.google.com/specimen/Orbitron
 *     → orbitron_regular.ttf, orbitron_bold.ttf, orbitron_extrabold.ttf
 *
 *   JetBrains Mono (OFL): https://www.jetbrains.com/lp/mono/
 *     → jetbrainsmono_regular.ttf, jetbrainsmono_medium.ttf
 *
 * With the files in res/font/, replace the definitions with:
 *
 *   val OrbitronFamily = FontFamily(
 *       Font(R.font.orbitron_regular, FontWeight.Normal),
 *       Font(R.font.orbitron_bold, FontWeight.Bold),
 *       Font(R.font.orbitron_extrabold, FontWeight.ExtraBold)
 *   )
 *
 *   val JetBrainsMonoFamily = FontFamily(
 *       Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
 *       Font(R.font.jetbrainsmono_medium, FontWeight.Medium)
 *   )
 */

// System font fallbacks — replace with real TTFs as described above
val OrbitronFamily: FontFamily = FontFamily.Default       // → Orbitron Bold
val JetBrainsMonoFamily: FontFamily = FontFamily.Monospace // → JetBrains Mono
