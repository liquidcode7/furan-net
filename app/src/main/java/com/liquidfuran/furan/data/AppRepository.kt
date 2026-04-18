package com.liquidfuran.furan.data

import android.content.Intent
import android.content.pm.PackageManager
import com.liquidfuran.furan.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val packageManager: PackageManager
) {
    suspend fun getAllLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    isAllowlisted = false
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun getAppsWithAllowlist(allowlist: Set<String>): List<AppInfo> =
        withContext(Dispatchers.IO) {
            getAllLaunchableApps().map { app ->
                app.copy(isAllowlisted = app.packageName in allowlist)
            }.sortedWith(
                compareByDescending<AppInfo> { it.isAllowlisted }.thenBy { it.name.lowercase() }
            )
        }

    suspend fun getAllowlistedApps(allowlist: Set<String>): List<AppInfo> =
        getAppsWithAllowlist(allowlist).filter { it.isAllowlisted }

    fun getAppLabel(packageName: String): String? = runCatching {
        packageManager.getApplicationInfo(packageName, 0)
            .let { packageManager.getApplicationLabel(it).toString() }
    }.getOrNull()
}
