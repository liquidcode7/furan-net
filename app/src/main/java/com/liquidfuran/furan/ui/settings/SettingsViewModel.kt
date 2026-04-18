package com.liquidfuran.furan.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.AppRepository
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.AppInfo
import com.liquidfuran.furan.model.DaySchedule
import com.liquidfuran.furan.model.NtfyConfig
import com.liquidfuran.furan.model.WeekSchedule
import com.liquidfuran.furan.util.HmacUtil
import com.liquidfuran.furan.worker.ScheduleWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class SettingsUiState(
    val ntfyConfig: NtfyConfig = NtfyConfig(),
    val scheduleEnabled: Boolean = false,
    val weekSchedule: WeekSchedule = WeekSchedule(),
    val allApps: List<AppInfo> = emptyList(),
    val allowlist: Set<String> = emptySet(),
    val isDeviceOwner: Boolean = false,
    val deviceOwnerCommand: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PrefsRepository,
    private val appRepository: AppRepository,
    private val deviceAdminManager: DeviceAdminManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepository.ntfyConfig,
        prefsRepository.scheduleEnabled,
        prefsRepository.weekSchedule,
        prefsRepository.allowlist
    ) { config, schedEnabled, schedule, allowlist ->
        object {
            val config = config; val schedEnabled = schedEnabled
            val schedule = schedule; val allowlist = allowlist
        }
    }.map { data ->
        val apps = appRepository.getAppsWithAllowlist(data.allowlist)
        SettingsUiState(
            ntfyConfig = data.config,
            scheduleEnabled = data.schedEnabled,
            weekSchedule = data.schedule,
            allApps = apps,
            allowlist = data.allowlist,
            isDeviceOwner = deviceAdminManager.isDeviceOwner,
            deviceOwnerCommand = "adb shell dpm set-device-owner ${context.packageName}/.receiver.AdminReceiver"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun saveNtfyConfig(config: NtfyConfig) {
        viewModelScope.launch { prefsRepository.setNtfyConfig(config) }
    }

    fun generateNewSecret(): String = HmacUtil.generateSecret()

    fun toggleAllowlist(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = prefsRepository.allowlist.first().toMutableSet()
            if (enabled) current.add(packageName) else current.remove(packageName)
            prefsRepository.setAllowlist(current)
        }
    }

    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepository.setScheduleEnabled(enabled)
            val schedule = prefsRepository.weekSchedule.first()
            if (enabled) {
                ScheduleWorker.scheduleNext(context, schedule)
            } else {
                ScheduleWorker.cancelSchedule(context)
            }
        }
    }

    fun updateDaySchedule(day: DayOfWeek, schedule: DaySchedule) {
        viewModelScope.launch {
            val current = prefsRepository.weekSchedule.first()
            val updated = current.withDay(day, schedule)
            prefsRepository.setWeekSchedule(updated)
            if (prefsRepository.scheduleEnabled.first()) {
                ScheduleWorker.scheduleNext(context, updated)
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "FURAN") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
