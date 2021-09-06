package com.ggl.openapi.linter.plugin.service

import arrow.core.ValidatedNel
import com.fasterxml.jackson.databind.JsonNode
import com.ggl.openapi.linter.plugin.model.Linter
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import org.apache.maven.plugin.logging.Log

abstract class LinterValidationService(val linter: Linter) {

    abstract fun validate(log: Log, server: String, json: JsonNode): ValidatedNel<OpenAPILinterError, ValidationResponse>
}
