package com.liquidfuran.furan.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.liquidfuran.furan.receiver.AdminReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceAdminManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devicePolicyManager: DevicePolicyManager,
    private val packageManager: PackageManager
) {
    private val adminComponent = ComponentName(context, AdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    /**
     * Suspends all launchable apps NOT in [allowlist].
     * Gracefully no-ops if device owner is not active.
     */
    suspend fun suspendAllExcept(allowlist: Set<String>) = withContext(Dispatchers.IO) {
        if (!isDeviceOwner) return@withContext

        val toSuspend = getAllLaunchablePackages()
            .filter { it !in allowlist && it != context.packageName }
            .toTypedArray()

        if (toSuspend.isNotEmpty()) {
            runCatching {
                devicePolicyManager.setPackagesSuspended(adminComponent, toSuspend, true)
            }
            // Ignore NameNotFoundException etc. — some privileged packages silently fail
        }
    }

    /**
     * Unsuspends all packages previously suspended by this app.
     */
    suspend fun unsuspendAll() = withContext(Dispatchers.IO) {
        if (!isDeviceOwner) return@withContext

        val all = getAllLaunchablePackages()
            .filter { it != context.packageName }
            .toTypedArray()

        if (all.isNotEmpty()) {
            runCatching {
                devicePolicyManager.setPackagesSuspended(adminComponent, all, false)
            }
        }
    }

    private fun getAllLaunchablePackages(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .distinct()
    }
}
