package com.ggl.openapi.linter.plugin.model.validation

import com.ggl.openapi.linter.plugin.model.Severity

data class ValidationResponse(
    val externalId: String,
    val message: String,
    val violations: List<Violation> = emptyList(),
    val violationsCount: Map<Severity, Int> = emptyMap(),
    val apiDefinition: String
)
