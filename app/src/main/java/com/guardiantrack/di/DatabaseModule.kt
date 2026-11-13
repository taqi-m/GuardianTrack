package com.guardiantrack.di

import android.content.Context
import androidx.room.Room
import com.guardiantrack.core.database.GuardianDatabase
import com.guardiantrack.core.database.dao.GeofenceDao
import com.guardiantrack.core.database.dao.GeofenceEventDao
import com.guardiantrack.core.database.dao.LocationDao
import com.guardiantrack.core.database.dao.TelemetryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGuardianDatabase(
        @ApplicationContext context: Context
    ): GuardianDatabase {
        return Room.databaseBuilder(
            context,
            GuardianDatabase::class.java,
            GuardianDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: GuardianDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideGeofenceDao(database: GuardianDatabase): GeofenceDao = database.geofenceDao()

    @Provides
    @Singleton
    fun provideGeofenceEventDao(database: GuardianDatabase): GeofenceEventDao = database.geofenceEventDao()

    @Provides
    @Singleton
    fun provideTelemetryDao(database: GuardianDatabase): TelemetryDao = database.telemetryDao()
}
