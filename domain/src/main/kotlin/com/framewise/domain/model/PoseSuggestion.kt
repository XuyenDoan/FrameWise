package com.framewise.domain.model

/**
 * Deliberately small set: only what's geometrically derivable with
 * reasonable confidence from shoulder/nose landmark positions alone (see
 * [com.framewise.domain.usecase.AnalyzePoseUseCase]). Full pose coaching
 * ("xoay vai", "nhìn sang trái/phải") would need reliable depth/rotation
 * estimation this MVP heuristic doesn't attempt.
 */
enum class PoseSuggestion {
    RELAX_SHOULDERS,
    RAISE_CHIN,
    NONE,
}
