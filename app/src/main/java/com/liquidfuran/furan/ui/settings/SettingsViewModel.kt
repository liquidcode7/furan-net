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
    val orderedAllowlist: List<AppInfo> = emptyList(),
    val otherApps: List<AppInfo> = emptyList(),
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
        prefsRepository.allowlist,
        prefsRepository.allowlistOrder
    ) { config, schedEnabled, schedule, allowlist, order ->
        val allApps = appRepository.getAppsWithAllowlist(allowlist)
        val allowlistMap = allApps.filter { it.isAllowlisted }.associateBy { it.packageName }
        val orderedAllowlist = (order.mapNotNull { allowlistMap[it] } +
                allApps.filter { it.isAllowlisted && it.packageName !in order })
        val otherApps = allApps.filter { !it.isAllowlisted }
        SettingsUiState(
            ntfyConfig = config,
            scheduleEnabled = schedEnabled,
            weekSchedule = schedule,
            orderedAllowlist = orderedAllowlist,
            otherApps = otherApps,
            allowlist = allowlist,
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
            val currentOrder = prefsRepository.allowlistOrder.first().toMutableList()
            if (enabled) {
                current.add(packageName)
                if (packageName !in currentOrder) currentOrder.add(packageName)
            } else {
                current.remove(packageName)
                currentOrder.remove(packageName)
            }
            prefsRepository.setAllowlist(current)
            prefsRepository.setAllowlistOrder(currentOrder)
        }
    }

    fun moveApp(packageName: String, up: Boolean) {
        viewModelScope.launch {
            val allowlist = prefsRepository.allowlist.first()
            val order = prefsRepository.allowlistOrder.first().toMutableList()
            // Ensure all allowlisted apps are in the order list
            allowlist.forEach { if (it !in order) order.add(it) }
            val idx = order.indexOf(packageName)
            if (idx < 0) return@launch
            val targetIdx = if (up) idx - 1 else idx + 1
            if (targetIdx < 0 || targetIdx >= order.size) return@launch
            order.removeAt(idx)
            order.add(targetIdx, packageName)
            prefsRepository.setAllowlistOrder(order)
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
