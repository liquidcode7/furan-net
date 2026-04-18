package com.liquidfuran.furan.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.ui.dumb.DumbModeScreen
import com.liquidfuran.furan.ui.smart.SmartModeScreen
import com.liquidfuran.furan.ui.settings.SettingsScreen
import com.liquidfuran.furan.ui.theme.FuranColors
import com.liquidfuran.furan.ui.theme.FuranTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on isn't needed — but prevent screenshots in dumb mode
        // (uncomment if desired):
        // window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            FuranTheme {
                FuranNavHost(viewModel = viewModel)
            }
        }
    }

}

@Composable
private fun FuranNavHost(viewModel: LauncherViewModel) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    // In settings: back returns to launcher
    BackHandler(enabled = showSettings) { showSettings = false }
    // At root: always consume — launchers never exit on back
    BackHandler(enabled = !showSettings) { /* absorb */ }

    AnimatedContent(
        targetState = showSettings,
        modifier = Modifier
            .fillMaxSize()
            .background(FuranColors.Background),
        transitionSpec = {
            if (targetState) {
                // Entering settings: slide up
                slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it / 4 } + fadeOut()
            } else {
                // Exiting settings: slide down
                slideInVertically { -it / 4 } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
            }
        },
        label = "settings_nav"
    ) { inSettings ->
        if (inSettings) {
            SettingsScreen(onNavigateBack = { showSettings = false })
        } else {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    if (targetState == FuranMode.SMART) {
                        // Unlocking: fade in bright
                        fadeIn(initialAlpha = 0f) togetherWith fadeOut()
                    } else {
                        // Locking: crossfade back to dumb
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "mode_switch"
            ) { currentMode ->
                when (currentMode) {
                    FuranMode.DUMB -> DumbModeScreen(
                        onNavigateToSettings = { showSettings = true }
                    )
                    FuranMode.SMART -> SmartModeScreen(
                        onNavigateToSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
