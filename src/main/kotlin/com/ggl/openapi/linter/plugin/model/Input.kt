package com.ggl.openapi.linter.plugin.model

data class Input(
    val linter: Linter,
    val thresholds: Set<Threshold> = emptySet()
)
