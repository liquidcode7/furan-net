package com.liquidfuran.furan.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.FuranMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var deviceAdminManager: DeviceAdminManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in listOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.LOCKED_BOOT_COMPLETED"
            )
        ) return

        Log.i("BootReceiver", "Boot completed — restoring FURAN mode")
        val pendingResult = goAsync()

        scope.launch {
            try {
                val mode = prefsRepository.mode.first()
                val allowlist = prefsRepository.allowlist.first()

                when (mode) {
                    FuranMode.DUMB -> {
                        // Re-apply suspension on reboot
                        deviceAdminManager.suspendAllExcept(allowlist)
                        Log.i("BootReceiver", "Dumb mode restored — packages suspended")
                    }
                    FuranMode.SMART -> {
                        // Unsuspend all on smart mode boot
                        deviceAdminManager.unsuspendAll()
                        Log.i("BootReceiver", "Smart mode restored — packages unsuspended")
                    }
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to restore mode on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
