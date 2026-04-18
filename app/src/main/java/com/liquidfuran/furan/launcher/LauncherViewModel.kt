package com.liquidfuran.furan.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.FuranMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    prefsRepository: PrefsRepository
) : ViewModel() {

    val mode: StateFlow<FuranMode> = prefsRepository.mode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FuranMode.DUMB
    )
}
