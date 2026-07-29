package com.framewise.core.common

import javax.inject.Qualifier

/**
 * Qualifiers for injecting [kotlinx.coroutines.CoroutineDispatcher]s.
 *
 * [Analysis] is intentionally separate from [Default]: CameraX frame analysis
 * and ML inference must never share a pool with generic background work, so
 * a slow analyzer can't starve unrelated coroutines (or vice versa).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Main

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Default

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Analysis
