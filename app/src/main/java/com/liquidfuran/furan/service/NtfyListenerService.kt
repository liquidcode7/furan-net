package com.liquidfuran.furan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.ApprovalResult
import com.liquidfuran.furan.data.NtfyRepository
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.model.SigilState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NtfyListenerService : Service() {
    @Inject lateinit var ntfyRepository: NtfyRepository
    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var deviceAdminManager: DeviceAdminManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "furan_ntfy_listener"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startListening()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startListening() {
        serviceScope.launch {
            try {
                val config = prefsRepository.ntfyConfig.first()
                if (!config.isConfigured) {
                    Log.w("NtfyListenerService", "ntfy not configured — stopping")
                    prefsRepository.setSigilState(SigilState.DENIED)
                    stopSelf()
                    return@launch
                }

                prefsRepository.setSigilState(SigilState.WAITING)

                ntfyRepository.listenForApproval(config).collect { result ->
                    when (result) {
                        is ApprovalResult.Approved -> {
                            Log.i("NtfyListenerService", "Approval received — switching to Smart mode")
                            deviceAdminManager.unsuspendAll()
                            prefsRepository.setMode(FuranMode.SMART)
                            prefsRepository.setSigilState(SigilState.APPROVED)
                            stopSelf()
                        }
                        is ApprovalResult.Denied -> {
                            // A well-formed but invalid APPROVE message (bad HMAC/expired).
                            // Flash DENIED state briefly, then return to WAITING — the channel
                            // stays open so a corrected message can still arrive.
                            Log.w("NtfyListenerService", "Denied: ${result.reason}")
                            prefsRepository.setSigilState(SigilState.DENIED)
                            kotlinx.coroutines.delay(3_000)
                            prefsRepository.setSigilState(SigilState.WAITING)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NtfyListenerService", "SSE listener error", e)
                prefsRepository.setSigilState(SigilState.DENIED)
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FURAN Unlock Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Waiting for unlock approval"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FURAN")
            .setContentText("Waiting for unlock approval…")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
