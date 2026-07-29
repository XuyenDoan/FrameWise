package com.framewise.data.vision

import com.framewise.domain.model.SceneType
import com.google.mlkit.vision.label.ImageLabel

/**
 * Maps ML Kit's default Image Labeling output (a generic ~400-label
 * ImageNet-derived set, not scene-specific) onto our [SceneType] via
 * keyword matching. This is a best-effort heuristic, not a trained scene
 * classifier: it will misclassify or fall back to [SceneType.UNKNOWN] for
 * anything that doesn't clearly match. A dedicated TFLite/custom scene
 * model (per the Phase 1 roadmap's "on-device AI" note) would be needed
 * for real accuracy - swapping it in only requires changing this file,
 * since the rest of the app only depends on [SceneType].
 */
internal object SceneClassifier {

    fun classify(labels: List<ImageLabel>): SceneType {
        val texts = labels.map { it.text.lowercase() }

        return when {
            texts.any { it.contains("food") || it.contains("dish") || it.contains("meal") || it.contains("cuisine") } ->
                SceneType.FOOD
            texts.any { it.contains("dog") || it.contains("cat") || it.contains("pet") } ->
                SceneType.PET
            texts.any { it.contains("flower") || it.contains("petal") || it.contains("blossom") } ->
                SceneType.FLOWER
            texts.any { it.contains("building") || it.contains("architecture") || it.contains("tower") || it.contains("bridge") } ->
                SceneType.ARCHITECTURE
            texts.any { it.contains("person") || it.contains("face") || it.contains("human") } ->
                SceneType.PORTRAIT
            texts.any { it.contains("sky") || it.contains("mountain") || it.contains("beach") || it.contains("nature") || it.contains("cloud") || it.contains("horizon") } ->
                SceneType.LANDSCAPE
            texts.any { it.contains("street") || it.contains("road") || it.contains("car") || it.contains("vehicle") } ->
                SceneType.STREET
            else -> SceneType.UNKNOWN
        }
    }
}
