package com.liquidfuran.furan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.model.NtfyConfig
import com.liquidfuran.furan.model.SigilState
import com.liquidfuran.furan.model.WeekSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_MODE = stringPreferencesKey("furan_mode")
        private val KEY_NTFY_SERVER = stringPreferencesKey("ntfy_server")
        private val KEY_NTFY_REQUEST_TOPIC = stringPreferencesKey("ntfy_request_topic")
        private val KEY_NTFY_APPROVAL_TOPIC = stringPreferencesKey("ntfy_approval_topic")
        private val KEY_NTFY_SECRET = stringPreferencesKey("ntfy_secret")
        private val KEY_SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        private val KEY_SCHEDULE_JSON = stringPreferencesKey("schedule_json")
        private val KEY_ALLOWLIST = stringSetPreferencesKey("allowlist_packages")
        private val KEY_ALLOWLIST_ORDER = stringPreferencesKey("allowlist_order")
        private val KEY_SIGIL_STATE = stringPreferencesKey("sigil_state")

        val DEFAULT_ALLOWLIST = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.messaging",
            "com.google.android.apps.messaging",
            "com.google.android.apps.maps",
            "com.android.camera2",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "app.organicmaps",
            "net.osmand",
            "com.tailscale.ipn.android"
        )
    }

    private fun <T> Flow<T>.withDefault(default: T): Flow<T> =
        catch { if (it is IOException) emit(default) else throw it }

    val mode: Flow<FuranMode> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs ->
            when (prefs[KEY_MODE]) {
                FuranMode.SMART.name -> FuranMode.SMART
                else -> FuranMode.DUMB
            }
        }

    val ntfyConfig: Flow<NtfyConfig> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs ->
            NtfyConfig(
                serverUrl = prefs[KEY_NTFY_SERVER] ?: "",
                requestTopic = prefs[KEY_NTFY_REQUEST_TOPIC] ?: "furan-unlock",
                approvalTopic = prefs[KEY_NTFY_APPROVAL_TOPIC] ?: "furan-approved",
                sharedSecret = prefs[KEY_NTFY_SECRET] ?: ""
            )
        }

    val scheduleEnabled: Flow<Boolean> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs -> prefs[KEY_SCHEDULE_ENABLED] ?: false }

    val weekSchedule: Flow<WeekSchedule> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs ->
            prefs[KEY_SCHEDULE_JSON]?.let {
                runCatching { Json.decodeFromString<WeekSchedule>(it) }.getOrNull()
            } ?: WeekSchedule()
        }

    val allowlist: Flow<Set<String>> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs -> prefs[KEY_ALLOWLIST] ?: DEFAULT_ALLOWLIST }

    val allowlistOrder: Flow<List<String>> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs ->
            prefs[KEY_ALLOWLIST_ORDER]?.let { json ->
                runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
            } ?: emptyList()
        }

    val sigilState: Flow<SigilState> = dataStore.data
        .withDefault(emptyPreferences())
        .map { prefs ->
            when (prefs[KEY_SIGIL_STATE]) {
                SigilState.WAITING.name -> SigilState.WAITING
                SigilState.APPROVED.name -> SigilState.APPROVED
                SigilState.DENIED.name -> SigilState.DENIED
                SigilState.REQUESTING.name -> SigilState.REQUESTING
                else -> SigilState.IDLE
            }
        }

    suspend fun setMode(mode: FuranMode) {
        dataStore.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setNtfyConfig(config: NtfyConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_NTFY_SERVER] = config.serverUrl
            prefs[KEY_NTFY_REQUEST_TOPIC] = config.requestTopic
            prefs[KEY_NTFY_APPROVAL_TOPIC] = config.approvalTopic
            prefs[KEY_NTFY_SECRET] = config.sharedSecret
        }
    }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SCHEDULE_ENABLED] = enabled }
    }

    suspend fun setWeekSchedule(schedule: WeekSchedule) {
        dataStore.edit { it[KEY_SCHEDULE_JSON] = Json.encodeToString(schedule) }
    }

    suspend fun setAllowlist(packages: Set<String>) {
        dataStore.edit { it[KEY_ALLOWLIST] = packages }
    }

    suspend fun setAllowlistOrder(order: List<String>) {
        dataStore.edit { it[KEY_ALLOWLIST_ORDER] = Json.encodeToString(order) }
    }

    suspend fun setSigilState(state: SigilState) {
        dataStore.edit { it[KEY_SIGIL_STATE] = state.name }
    }

    // Convenience one-shot reads (for receivers / workers that can't observe flows)
    suspend fun getCurrentMode(): FuranMode = mode.first()
    suspend fun getCurrentAllowlist(): Set<String> = allowlist.first()
}
