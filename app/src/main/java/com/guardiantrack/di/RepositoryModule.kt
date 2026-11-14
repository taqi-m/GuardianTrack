package com.guardiantrack.di

import com.guardiantrack.core.data.repository.GeofenceRepositoryImpl
import com.guardiantrack.core.data.repository.LocationRepositoryImpl
import com.guardiantrack.core.data.repository.OemSurvivalRepositoryImpl
import com.guardiantrack.core.data.repository.SyncRepositoryImpl
import com.guardiantrack.core.data.repository.TelemetryRepositoryImpl
import com.guardiantrack.core.domain.repository.GeofenceRepository
import com.guardiantrack.core.domain.repository.LocationRepository
import com.guardiantrack.core.domain.repository.OemSurvivalRepository
import com.guardiantrack.core.domain.repository.SyncRepository
import com.guardiantrack.core.domain.repository.TelemetryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindGeofenceRepository(
        impl: GeofenceRepositoryImpl
    ): GeofenceRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(
        impl: TelemetryRepositoryImpl
    ): TelemetryRepository

    @Binds
    @Singleton
    abstract fun bindOemSurvivalRepository(
        impl: OemSurvivalRepositoryImpl
    ): OemSurvivalRepository
}
