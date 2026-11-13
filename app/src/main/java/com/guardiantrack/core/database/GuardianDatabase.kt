package com.guardiantrack.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.guardiantrack.core.database.converter.DatabaseConverters
import com.guardiantrack.core.database.dao.GeofenceDao
import com.guardiantrack.core.database.dao.GeofenceEventDao
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.dao.TelemetryDao
import com.guardiantrack.core.database.entity.GeofenceEntity
import com.guardiantrack.core.database.entity.GeofenceEventEntity
import com.guardiantrack.core.database.entity.LocationEntity
import com.guardiantrack.core.database.entity.TelemetryLogEntity

@Database(
    entities = [
        LocationEntity::class,
        GeofenceEntity::class,
        GeofenceEventEntity::class,
        TelemetryLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun geofenceEventDao(): GeofenceEventDao
    abstract fun telemetryDao(): TelemetryDao

    companion object {
        const val DATABASE_NAME = "guardian_track.db"
    }
}
