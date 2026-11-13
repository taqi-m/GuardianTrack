package com.guardiantrack.core.database.converter

import androidx.room.TypeConverter

class DatabaseConverters {
    @TypeConverter
    fun fromStringSet(set: Set<String>?): String? {
        return set?.joinToString(separator = ",")
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
    }
}
