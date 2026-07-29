package com.framewise.data.camera.di

import com.framewise.data.camera.CameraFrameProvider
import com.framewise.data.camera.CameraPreviewBinder
import com.framewise.data.camera.CameraXController
import com.framewise.domain.repository.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(controller: CameraXController): CameraRepository

    @Binds
    @Singleton
    abstract fun bindCameraPreviewBinder(controller: CameraXController): CameraPreviewBinder

    @Binds
    @Singleton
    abstract fun bindCameraFrameProvider(controller: CameraXController): CameraFrameProvider
}
