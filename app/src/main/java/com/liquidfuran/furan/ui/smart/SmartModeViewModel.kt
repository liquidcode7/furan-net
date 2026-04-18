package com.liquidfuran.furan.ui.smart

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.AppRepository
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.AppInfo
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.model.SigilState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class SmartModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsRepository: PrefsRepository,
    private val appRepository: AppRepository,
    private val deviceAdminManager: DeviceAdminManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<SmartUiState> = combine(
        _allApps,
        _searchQuery,
        _isLoading
    ) { apps, query, loading ->
        val filtered = if (query.isBlank()) {
            apps
        } else {
            apps.filter { it.name.contains(query, ignoreCase = true) }
        }
        SmartUiState(
            allApps = apps,
            filteredApps = filtered,
            searchQuery = query,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmartUiState()
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val allowlist = prefsRepository.allowlist.first()
            _allApps.value = appRepository.getAppsWithAllowlistSorted(allowlist)
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun engageDumbMode() {
        viewModelScope.launch {
            val allowlist = prefsRepository.allowlist.first()
            deviceAdminManager.suspendAllExcept(allowlist)
            prefsRepository.setMode(FuranMode.DUMB)
            prefsRepository.setSigilState(SigilState.IDLE)
        }
    }
}
