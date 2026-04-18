package com.liquidfuran.furan.ui.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.ui.components.AppGrid
import com.liquidfuran.furan.ui.components.FuranBackground
import com.liquidfuran.furan.ui.components.FuranClock
import com.liquidfuran.furan.ui.components.VexSigil
import com.liquidfuran.furan.ui.theme.FuranColors
import com.liquidfuran.furan.ui.theme.JetBrainsMonoFamily
import com.liquidfuran.furan.ui.theme.OrbitronFamily

@Composable
fun SmartModeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: SmartModeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FuranColors.Background)
    ) {
        FuranBackground(modifier = Modifier.fillMaxSize())

        // Cyan glow blobs in smart mode (more intense)
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = 80.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(FuranColors.Cyan.copy(alpha = 0.06f), FuranColors.Background.copy(alpha = 0f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = (-100).dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(FuranColors.Violet.copy(alpha = 0.07f), FuranColors.Background.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // ── Status bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FURAN",
                    style = TextStyle(
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FuranColors.Cyan,
                        letterSpacing = 4.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SMART",
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = FuranColors.Cyan.copy(alpha = 0.8f),
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = FuranColors.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Clock ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            FuranClock()

            Spacer(Modifier.height(12.dp))

            // ── Sigil (glowing, tap to lock) ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VexSigil(
                    state = SigilState.APPROVED,
                    onClick = { viewModel.engageDumbMode() },
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "tap sigil to lock",
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = FuranColors.White.copy(alpha = 0.3f),
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Search bar ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(FuranColors.Navy)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = FuranColors.Cyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 14.sp,
                            color = FuranColors.White
                        ),
                        cursorBrush = SolidColor(FuranColors.Cyan),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "search apps…",
                                    style = TextStyle(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 14.sp,
                                        color = FuranColors.White.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── App grid ───────────────────────────────────────────────────────
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FuranColors.Cyan, modifier = Modifier.size(32.dp))
                }
            } else {
                AppGrid(
                    apps = state.filteredApps,
                    onAppClick = { viewModel.launchApp(it.packageName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // ── Footer ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "lf · tetrafuranose",
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = FuranColors.White.copy(alpha = 0.2f),
                        letterSpacing = 2.sp
                    )
                )
            }
        }
    }
}
