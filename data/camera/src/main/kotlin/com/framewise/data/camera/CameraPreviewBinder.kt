package com.framewise.data.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

/**
 * Wires the live CameraX preview surface to a Compose `PreviewView`.
 *
 * This is deliberately **not** part of `domain.repository.CameraRepository`:
 * `PreviewView`/`LifecycleOwner` are Android UI/framework types, and the
 * `domain` module is a pure-Kotlin module that cannot depend on them.
 * Wrapping them in a fake "platform-agnostic" abstraction would only add
 * indirection with no real testability benefit, since surface binding is
 * inherently UI wiring, verified visually rather than by unit test.
 *
 * `feature:camerapreview` depends on this module directly for that reason
 * alone; all other camera control state still flows through
 * `CameraRepository` so the ViewModel's decision logic stays unit-testable.
 */
interface CameraPreviewBinder {
    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView)
    fun unbind()
}
