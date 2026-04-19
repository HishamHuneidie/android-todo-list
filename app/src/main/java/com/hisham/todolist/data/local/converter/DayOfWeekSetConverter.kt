package com.hisham.todolist.data.local.converter

import androidx.room.TypeConverter
import java.time.DayOfWeek

class DayOfWeekSetConverter {

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String =
        days.sortedBy { it.value }.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekSet(rawValue: String): Set<DayOfWeek> =
        rawValue
            .takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { DayOfWeek.valueOf(it) }
            ?.toSet()
            ?: emptySet()
}
