package com.liquidfuran.furan.ui.dumb

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.ui.components.*
import com.liquidfuran.furan.ui.theme.FuranColors
import com.liquidfuran.furan.ui.theme.JetBrainsMonoFamily
import com.liquidfuran.furan.ui.theme.OrbitronFamily

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DumbModeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: DumbModeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FuranColors.Background)
    ) {
        // Background geometry layer
        FuranBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // ── Status bar row ─────────────────────────────────────────────────
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
                Text(
                    text = when (state.sigilState) {
                        SigilState.WAITING -> "AWAITING APPROVAL"
                        SigilState.APPROVED -> "UNLOCKING"
                        SigilState.DENIED -> "DENIED"
                        SigilState.REQUESTING -> "REQUESTING"
                        SigilState.IDLE -> "LOCKED"
                    },
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = when (state.sigilState) {
                            SigilState.DENIED -> FuranColors.Magenta
                            SigilState.APPROVED -> FuranColors.Cyan
                            SigilState.WAITING, SigilState.REQUESTING -> FuranColors.Violet
                            SigilState.IDLE -> FuranColors.White.copy(alpha = 0.4f)
                        },
                        letterSpacing = 2.sp
                    )
                )
            }

            // ── Clock ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            FuranClock()

            // ── Gradient divider ───────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                FuranColors.Cyan.copy(alpha = 0f),
                                FuranColors.Cyan.copy(alpha = 0.6f),
                                FuranColors.Violet.copy(alpha = 0.6f),
                                FuranColors.Violet.copy(alpha = 0f)
                            )
                        )
                    )
            )
            Spacer(Modifier.height(24.dp))

            // ── Sigil ──────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VexSigil(
                    state = state.sigilState,
                    onClick = { viewModel.onSigilTapped() },
                    modifier = Modifier.size(180.dp)
                )
            }

            // State hint
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = when (state.sigilState) {
                        SigilState.IDLE -> if (state.isNtfyConfigured) "tap to request unlock" else "configure ntfy in settings"
                        SigilState.REQUESTING -> "sending request…"
                        SigilState.WAITING -> "waiting for approval…"
                        SigilState.APPROVED -> "approved"
                        SigilState.DENIED -> "denied — try again"
                    },
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = FuranColors.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── App grid ───────────────────────────────────────────────────────
            if (state.allowlistApps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(FuranColors.GridLine)
                )
                AppGrid(
                    apps = state.allowlistApps,
                    onAppClick = { viewModel.launchApp(it.packageName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // ── Footer — long-press to open settings ───────────────────────────
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
                    ),
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onNavigateToSettings
                    )
                )
            }
        }

        // Error snackbar
        state.errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = FuranColors.Cyan)
                    }
                },
                containerColor = FuranColors.Navy
            ) {
                Text(msg, color = FuranColors.White, fontFamily = JetBrainsMonoFamily)
            }
        }
    }
}
