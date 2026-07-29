package com.framewise.domain.model

enum class LensFacing {
    BACK,
    FRONT,
    ;

    fun opposite(): LensFacing = if (this == BACK) FRONT else BACK
}
