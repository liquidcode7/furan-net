package com.liquidfuran.furan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.liquidfuran.furan.ui.theme.FuranColors
import com.liquidfuran.furan.ui.theme.JetBrainsMonoFamily
import com.liquidfuran.furan.ui.theme.OrbitronFamily
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun FuranClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = LocalDateTime.now()
        }
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE  dd MMM yyyy") }

    Column(modifier = modifier) {
        Text(
            text = now.format(timeFormatter),
            style = TextStyle(
                fontFamily = OrbitronFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 72.sp,
                color = FuranColors.White,
                letterSpacing = (-1).sp
            )
        )
        Text(
            text = now.format(dateFormatter).uppercase(),
            style = TextStyle(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = FuranColors.Cyan.copy(alpha = 0.75f),
                letterSpacing = 2.sp
            )
        )
    }
}
