package com.framewise.data.sensor.di

import com.framewise.data.sensor.HorizonSensorController
import com.framewise.domain.repository.SensorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {
    @Binds
    @Singleton
    abstract fun bindSensorRepository(controller: HorizonSensorController): SensorRepository
}
