package com.liquidfuran.furan.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class DaySchedule(
    val enabled: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0
)

@Serializable
data class WeekSchedule(
    val monday: DaySchedule = DaySchedule(),
    val tuesday: DaySchedule = DaySchedule(),
    val wednesday: DaySchedule = DaySchedule(),
    val thursday: DaySchedule = DaySchedule(),
    val friday: DaySchedule = DaySchedule(),
    val saturday: DaySchedule = DaySchedule(),
    val sunday: DaySchedule = DaySchedule()
) {
    fun forDay(day: DayOfWeek): DaySchedule = when (day) {
        DayOfWeek.MONDAY -> monday
        DayOfWeek.TUESDAY -> tuesday
        DayOfWeek.WEDNESDAY -> wednesday
        DayOfWeek.THURSDAY -> thursday
        DayOfWeek.FRIDAY -> friday
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }

    fun withDay(day: DayOfWeek, schedule: DaySchedule): WeekSchedule = when (day) {
        DayOfWeek.MONDAY -> copy(monday = schedule)
        DayOfWeek.TUESDAY -> copy(tuesday = schedule)
        DayOfWeek.WEDNESDAY -> copy(wednesday = schedule)
        DayOfWeek.THURSDAY -> copy(thursday = schedule)
        DayOfWeek.FRIDAY -> copy(friday = schedule)
        DayOfWeek.SATURDAY -> copy(saturday = schedule)
        DayOfWeek.SUNDAY -> copy(sunday = schedule)
    }
}
