package com.ggl.openapi.linter.plugin.model.validation

import com.ggl.openapi.linter.plugin.model.Severity

data class Violation(
    val title: String,
    val description: String,
    val violationType: Severity,
    val ruleLink: String,
    val paths: List<String> = emptyList(),
    val pointer: String,
    val startLine: String? = null,
    val endLine: String? = null
)
