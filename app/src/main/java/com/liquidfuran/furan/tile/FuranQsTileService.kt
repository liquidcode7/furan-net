package com.liquidfuran.furan.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.NtfyRepository
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.service.NtfyListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FuranQsTileService : TileService() {
    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var deviceAdminManager: DeviceAdminManager
    @Inject lateinit var ntfyRepository: NtfyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val mode = prefsRepository.mode.first()
            updateTile(mode)
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val mode = prefsRepository.mode.first()
            when (mode) {
                FuranMode.SMART -> {
                    // Immediately engage dumb mode
                    val allowlist = prefsRepository.allowlist.first()
                    deviceAdminManager.suspendAllExcept(allowlist)
                    prefsRepository.setMode(FuranMode.DUMB)
                    prefsRepository.setSigilState(SigilState.IDLE)
                    updateTile(FuranMode.DUMB)
                    Log.i("QsTile", "Smart → Dumb via QS tile")
                }
                FuranMode.DUMB -> {
                    // Initiate ntfy unlock request
                    val config = prefsRepository.ntfyConfig.first()
                    if (!config.isConfigured) {
                        Log.w("QsTile", "ntfy not configured — cannot request unlock")
                        return@launch
                    }
                    ntfyRepository.publishUnlockRequest(config)
                    prefsRepository.setSigilState(SigilState.REQUESTING)
                    val serviceIntent = Intent(this@FuranQsTileService, NtfyListenerService::class.java)
                    startForegroundService(serviceIntent)
                    Log.i("QsTile", "Unlock request sent via QS tile")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun updateTile(mode: FuranMode) {
        qsTile?.apply {
            state = if (mode == FuranMode.SMART) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (mode == FuranMode.SMART) "FURAN: Smart" else "FURAN: Locked"
            updateTile()
        }
    }
}
