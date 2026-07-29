package com.framewise.domain.model

/**
 * User-chosen shooting mode (a manual toggle, distinct from [SceneType]
 * which is ML Kit's auto-detected scene label shown only as an informational
 * badge/tip). The mode changes which composition rules
 * [com.framewise.domain.usecase.AnalyzeCompositionUseCase] applies and how
 * it auto-picks a primary subject when the user hasn't tapped one - it does
 * NOT change [GridType], which stays a separate, independent user choice.
 */
enum class ShootingMode {
    AUTO,
    PORTRAIT,
    ANIMAL,
    LANDSCAPE,
}
