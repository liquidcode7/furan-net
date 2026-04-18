package com.liquidfuran.furan.ui.dumb

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liquidfuran.furan.data.AppRepository
import com.liquidfuran.furan.data.NtfyRepository
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.AppInfo
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.service.NtfyListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DumbUiState(
    val allowlistApps: List<AppInfo> = emptyList(),
    val sigilState: SigilState = SigilState.IDLE,
    val isNtfyConfigured: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DumbModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PrefsRepository,
    private val appRepository: AppRepository,
    private val ntfyRepository: NtfyRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    // _errorMessage is included in the combine so updating it triggers a UI re-emission
    val uiState: StateFlow<DumbUiState> = combine(
        prefsRepository.sigilState,
        prefsRepository.ntfyConfig,
        prefsRepository.allowlist,
        _errorMessage
    ) { sigilState, config, allowlist, errorMsg ->
        val apps = appRepository.getAllowlistedApps(allowlist)
        DumbUiState(
            allowlistApps = apps,
            sigilState = sigilState,
            isNtfyConfigured = config.isConfigured,
            errorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DumbUiState()
    )

    fun onSigilTapped() {
        viewModelScope.launch {
            val config = prefsRepository.ntfyConfig.first()
            if (!config.isConfigured) {
                _errorMessage.value = "ntfy not configured — open Settings"
                return@launch
            }

            val currentState = prefsRepository.sigilState.first()
            if (currentState == SigilState.WAITING || currentState == SigilState.REQUESTING) return@launch

            prefsRepository.setSigilState(SigilState.REQUESTING)

            ntfyRepository.publishUnlockRequest(config).fold(
                onSuccess = {
                    val serviceIntent = Intent(context, NtfyListenerService::class.java)
                    context.startForegroundService(serviceIntent)
                },
                onFailure = { e ->
                    prefsRepository.setSigilState(SigilState.DENIED)
                    _errorMessage.value = "Failed to send request: ${e.message}"
                }
            )
        }
    }

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
