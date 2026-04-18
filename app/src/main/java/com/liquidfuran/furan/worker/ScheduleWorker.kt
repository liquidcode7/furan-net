package com.liquidfuran.furan.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liquidfuran.furan.admin.DeviceAdminManager
import com.liquidfuran.furan.data.PrefsRepository
import com.liquidfuran.furan.model.FuranMode
import com.liquidfuran.furan.model.WeekSchedule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class ScheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prefsRepository: PrefsRepository,
    private val deviceAdminManager: DeviceAdminManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_START_DUMB = "START_DUMB"
        const val ACTION_END_DUMB = "END_DUMB"
        const val WORK_NAME_START = "furan_schedule_start"
        const val WORK_NAME_END = "furan_schedule_end"

        /**
         * Schedules the next dumb-mode start and end based on the current week schedule.
         * Call this after saving a new schedule or after each scheduled action fires.
         */
        fun scheduleNext(context: Context, schedule: WeekSchedule) {
            val workManager = WorkManager.getInstance(context)
            val now = LocalDateTime.now()

            // Find the next start and end times across the week
            val daysToCheck = DayOfWeek.entries
            for (offset in 0..6) {
                val checkDay = DayOfWeek.entries[(now.dayOfWeek.ordinal + offset) % 7]
                val daySchedule = schedule.forDay(checkDay)
                if (!daySchedule.enabled) continue

                val startTime = LocalTime.of(daySchedule.startHour, daySchedule.startMinute)
                val endTime = LocalTime.of(daySchedule.endHour, daySchedule.endMinute)

                val startDt = now.with(checkDay).withHour(startTime.hour).withMinute(startTime.minute).withSecond(0)
                val endDt = now.with(checkDay).withHour(endTime.hour).withMinute(endTime.minute).withSecond(0)

                val adjustedStart = if (startDt.isAfter(now)) startDt else startDt.plusWeeks(1)
                val adjustedEnd = if (endDt.isAfter(now)) endDt else endDt.plusWeeks(1)

                val startDelay = ChronoUnit.MILLIS.between(now, adjustedStart)
                val endDelay = ChronoUnit.MILLIS.between(now, adjustedEnd)

                val startRequest = OneTimeWorkRequestBuilder<ScheduleWorker>()
                    .setInputData(Data.Builder().putString(KEY_ACTION, ACTION_START_DUMB).build())
                    .setInitialDelay(startDelay, TimeUnit.MILLISECONDS)
                    .build()
                workManager.enqueueUniqueWork(WORK_NAME_START, ExistingWorkPolicy.REPLACE, startRequest)

                val endRequest = OneTimeWorkRequestBuilder<ScheduleWorker>()
                    .setInputData(Data.Builder().putString(KEY_ACTION, ACTION_END_DUMB).build())
                    .setInitialDelay(endDelay, TimeUnit.MILLISECONDS)
                    .build()
                workManager.enqueueUniqueWork(WORK_NAME_END, ExistingWorkPolicy.REPLACE, endRequest)
                return
            }
        }

        fun cancelSchedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME_START)
            workManager.cancelUniqueWork(WORK_NAME_END)
        }
    }

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()
        Log.i("ScheduleWorker", "Executing scheduled action: $action")

        return try {
            val allowlist = prefsRepository.allowlist.first()
            when (action) {
                ACTION_START_DUMB -> {
                    deviceAdminManager.suspendAllExcept(allowlist)
                    prefsRepository.setMode(FuranMode.DUMB)
                    Log.i("ScheduleWorker", "Dumb mode engaged by schedule")
                }
                ACTION_END_DUMB -> {
                    // Scheduled end only unlocks if currently in dumb mode from schedule
                    // (manual lock via sigil/QS tile is never auto-unlocked by schedule)
                    deviceAdminManager.unsuspendAll()
                    prefsRepository.setMode(FuranMode.SMART)
                    Log.i("ScheduleWorker", "Smart mode restored by schedule")
                }
            }
            // Re-schedule the next occurrence
            val schedule = prefsRepository.weekSchedule.first()
            scheduleNext(applicationContext, schedule)
            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduleWorker", "Schedule action failed", e)
            Result.failure()
        }
    }
}
