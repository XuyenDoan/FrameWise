package com.framewise.data.vision.di

import com.framewise.data.vision.VisionRepositoryImpl
import com.framewise.domain.repository.PoseRepository
import com.framewise.domain.repository.VisionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VisionModule {
    @Binds
    @Singleton
    abstract fun bindVisionRepository(impl: VisionRepositoryImpl): VisionRepository

    @Binds
    @Singleton
    abstract fun bindPoseRepository(impl: VisionRepositoryImpl): PoseRepository
}
