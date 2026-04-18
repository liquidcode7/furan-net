package com.liquidfuran.furan.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liquidfuran.furan.model.AppInfo
import com.liquidfuran.furan.model.DaySchedule
import com.liquidfuran.furan.model.NtfyConfig
import androidx.compose.ui.platform.LocalContext
import com.liquidfuran.furan.ui.theme.FuranColors
import com.liquidfuran.furan.ui.theme.JetBrainsMonoFamily
import com.liquidfuran.furan.ui.theme.OrbitronFamily
import com.liquidfuran.furan.util.AppIconModel
import com.liquidfuran.furan.util.QrCodeUtil
import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Local edit buffers for ntfy config
    var serverUrl by remember(state.ntfyConfig.serverUrl) { mutableStateOf(state.ntfyConfig.serverUrl) }
    var requestTopic by remember(state.ntfyConfig.requestTopic) { mutableStateOf(state.ntfyConfig.requestTopic) }
    var approvalTopic by remember(state.ntfyConfig.approvalTopic) { mutableStateOf(state.ntfyConfig.approvalTopic) }
    var secret by remember(state.ntfyConfig.sharedSecret) { mutableStateOf(state.ntfyConfig.sharedSecret) }
    var secretVisible by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FuranColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── Top bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = FuranColors.Cyan)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SETTINGS",
                    style = TextStyle(
                        fontFamily = OrbitronFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = FuranColors.White,
                        letterSpacing = 3.sp
                    )
                )
            }
            HorizontalDivider(color = FuranColors.Cyan.copy(alpha = 0.2f))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ══ DEVICE ADMIN ══════════════════════════════════════════════
                item { SectionHeader("DEVICE ADMIN") }
                item {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                SettingsLabel("Device Owner")
                                SettingsSubtext(
                                    if (state.isDeviceOwner) "Active — package suspension enabled"
                                    else "Not active — run the ADB command below once"
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (state.isDeviceOwner) FuranColors.Cyan else FuranColors.Magenta,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                        if (!state.isDeviceOwner) {
                            Spacer(Modifier.height(10.dp))
                            CodeBlock(
                                text = state.deviceOwnerCommand,
                                onCopy = { viewModel.copyToClipboard(state.deviceOwnerCommand, "ADB command") }
                            )
                        }
                    }
                }

                // ══ NTFY CONFIG ═══════════════════════════════════════════════
                item { Spacer(Modifier.height(8.dp)); SectionHeader("NTFY CONFIGURATION") }
                item {
                    SettingsCard {
                        FuranTextField("Server URL", serverUrl, "https://ntfy.example.com") { serverUrl = it }
                        Spacer(Modifier.height(8.dp))
                        FuranTextField("Request Topic", requestTopic, "furan-unlock") { requestTopic = it }
                        Spacer(Modifier.height(8.dp))
                        FuranTextField("Approval Topic", approvalTopic, "furan-approved") { approvalTopic = it }
                        Spacer(Modifier.height(8.dp))

                        // Secret field with visibility toggle + regenerate
                        SettingsLabel("Shared Secret (HMAC)")
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = secret,
                                onValueChange = { secret = it },
                                textStyle = TextStyle(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 12.sp,
                                    color = FuranColors.White
                                ),
                                visualTransformation = if (secretVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                                cursorBrush = SolidColor(FuranColors.Cyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(FuranColors.Navy, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    secret = viewModel.generateNewSecret()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "Regenerate", tint = FuranColors.Violet, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { viewModel.copyToClipboard(secret, "FURAN secret") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = FuranColors.Cyan.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FuranButton("Save", onClick = {
                                viewModel.saveNtfyConfig(NtfyConfig(serverUrl, requestTopic, approvalTopic, secret))
                            })
                            FuranButton(
                                text = if (showQr) "Hide QR" else "Show QR",
                                onClick = { showQr = !showQr },
                                isPrimary = false
                            )
                        }

                        // QR code for wife to scan
                        if (showQr && secret.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            val qrPayload = "furan:secret:$secret"
                            val qrBitmap: Bitmap = remember(secret) {
                                QrCodeUtil.generateQrBitmap(qrPayload, 256)
                            }
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Secret QR Code",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .border(1.dp, FuranColors.Cyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                SettingsSubtext("Scan with wife's ntfy automation to share secret")
                            }
                        }
                    }
                }

                // ══ SCHEDULE ══════════════════════════════════════════════════
                item { Spacer(Modifier.height(8.dp)); SectionHeader("SCHEDULE") }
                item {
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SettingsLabel("Auto Dumb Mode Schedule")
                            Switch(
                                checked = state.scheduleEnabled,
                                onCheckedChange = { viewModel.setScheduleEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = FuranColors.Background,
                                    checkedTrackColor = FuranColors.Cyan
                                )
                            )
                        }
                    }
                }
                if (state.scheduleEnabled) {
                    items(DayOfWeek.entries) { day ->
                        DayScheduleRow(
                            day = day,
                            schedule = state.weekSchedule.forDay(day),
                            onUpdate = { viewModel.updateDaySchedule(day, it) }
                        )
                    }
                }

                // ══ ALLOWLIST ════════════════════════════════════════════════
                item { Spacer(Modifier.height(8.dp)); SectionHeader("ALLOWLIST") }
                item {
                    SettingsSubtext(
                        "Apps visible and launchable in Dumb Mode. Allowlisted apps are also pinned to the top of the Smart Mode drawer.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(state.allApps, key = { it.packageName }) { app ->
                    AllowlistRow(
                        app = app,
                        onToggle = { viewModel.toggleAllowlist(app.packageName, it) }
                    )
                }

                // ══ ABOUT ════════════════════════════════════════════════════
                item { Spacer(Modifier.height(8.dp)); SectionHeader("ABOUT") }
                item {
                    SettingsCard {
                        SettingsLabel("FURAN v1.0.0")
                        Spacer(Modifier.height(2.dp))
                        SettingsSubtext("lf · tetrafuranose")
                        Spacer(Modifier.height(8.dp))
                        SettingsSubtext("No analytics. No cloud. No Google.")
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ── Day schedule row ───────────────────────────────────────────────────────────

@Composable
private fun DayScheduleRow(
    day: DayOfWeek,
    schedule: DaySchedule,
    onUpdate: (DaySchedule) -> Unit
) {
    SettingsCard(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = TextStyle(
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = FuranColors.Cyan
                ),
                modifier = Modifier.width(40.dp)
            )
            Switch(
                checked = schedule.enabled,
                onCheckedChange = { onUpdate(schedule.copy(enabled = it)) },
                modifier = Modifier.padding(horizontal = 8.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FuranColors.Background,
                    checkedTrackColor = FuranColors.Cyan
                )
            )
            if (schedule.enabled) {
                Spacer(Modifier.width(4.dp))
                TimeChip(
                    label = "from",
                    hour = schedule.startHour,
                    minute = schedule.startMinute,
                    onTimeChanged = { h, m -> onUpdate(schedule.copy(startHour = h, startMinute = m)) }
                )
                Spacer(Modifier.width(8.dp))
                TimeChip(
                    label = "to",
                    hour = schedule.endHour,
                    minute = schedule.endMinute,
                    onTimeChanged = { h, m -> onUpdate(schedule.copy(endHour = h, endMinute = m)) }
                )
            }
        }
    }
}

@Composable
private fun TimeChip(
    label: String,
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FuranColors.Navy)
            .clickable { showPicker = true }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label %02d:%02d".format(hour, minute),
            style = TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = FuranColors.White.copy(alpha = 0.8f)
            )
        )
    }
    if (showPicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showPicker = false },
            onConfirm = { h, m -> onTimeChanged(h, m); showPicker = false }
        )
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FuranColors.Navy,
        title = {
            Text("Set Time", fontFamily = OrbitronFamily, color = FuranColors.Cyan, fontSize = 14.sp)
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberPicker("Hour", hour, 0, 23) { hour = it }
                Text(":", color = FuranColors.White, fontFamily = OrbitronFamily, fontSize = 20.sp)
                NumberPicker("Minute", minute, 0, 59, step = 5) { minute = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("OK", color = FuranColors.Cyan, fontFamily = JetBrainsMonoFamily)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FuranColors.White.copy(alpha = 0.5f), fontFamily = JetBrainsMonoFamily)
            }
        }
    )
}

@Composable
private fun NumberPicker(label: String, value: Int, min: Int, max: Int, step: Int = 1, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp, color = FuranColors.White.copy(alpha = 0.5f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { if (value - step >= min) onValueChange(value - step) }) {
                Text("−", color = FuranColors.Cyan)
            }
            Text(
                text = "%02d".format(value),
                fontFamily = OrbitronFamily,
                fontSize = 18.sp,
                color = FuranColors.White,
                modifier = Modifier.width(36.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(onClick = { if (value + step <= max) onValueChange(value + step) }) {
                Text("+", color = FuranColors.Cyan)
            }
        }
    }
}

@Composable
private fun AllowlistRow(app: AppInfo, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(AppIconModel(app.packageName))
                .build(),
            contentDescription = app.name,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.name,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                color = FuranColors.White
            )
            Text(
                text = app.packageName,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = FuranColors.White.copy(alpha = 0.35f)
            )
        }
        Switch(
            checked = app.isAllowlisted,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FuranColors.Background,
                checkedTrackColor = FuranColors.Cyan
            )
        )
    }
}

// ── Re-usable components ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = FuranColors.Cyan.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        ),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(FuranColors.Navy)
            .border(1.dp, FuranColors.Cyan.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = FuranColors.White
    )
}

@Composable
private fun SettingsSubtext(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = JetBrainsMonoFamily,
        fontSize = 11.sp,
        color = FuranColors.White.copy(alpha = 0.45f),
        modifier = modifier
    )
}

@Composable
private fun FuranTextField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    SettingsLabel(label)
    Spacer(Modifier.height(4.dp))
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, color = FuranColors.White),
        cursorBrush = SolidColor(FuranColors.Cyan),
        singleLine = true,
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FuranColors.Background.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, color = FuranColors.White.copy(alpha = 0.25f))
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FuranButton(text: String, onClick: () -> Unit, isPrimary: Boolean = true) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPrimary) FuranColors.Cyan.copy(alpha = 0.15f) else FuranColors.Violet.copy(alpha = 0.1f))
            .border(
                1.dp,
                if (isPrimary) FuranColors.Cyan.copy(alpha = 0.5f) else FuranColors.Violet.copy(alpha = 0.4f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            color = if (isPrimary) FuranColors.Cyan else FuranColors.Violet
        )
    }
}

@Composable
private fun CodeBlock(text: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(FuranColors.Background)
            .border(1.dp, FuranColors.Cyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = FuranColors.Cyan.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, "Copy", tint = FuranColors.Cyan.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        }
    }
}
