package com.ggl.openapi.linter.plugin.service

import arrow.core.ValidatedNel
import com.fasterxml.jackson.databind.JsonNode
import com.ggl.arrow.addon.ListExtensions.find
import com.ggl.arrow.addon.OptionExtensions.toValidated
import com.ggl.arrow.addon.ValidatedExtensions.flatMap
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.arrow.addon.ValidatedExtensions.peekLeft
import com.ggl.openapi.linter.plugin.model.Linter
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.LinterValidationServiceError
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import org.apache.maven.plugin.logging.Log

class ValidationService(private val validationServices: Set<LinterValidationService>) {

    fun validate(log: Log, linter: Linter, server: String, json: JsonNode): ValidatedNel<OpenAPILinterError, ValidationResponse> =
        log.debug("Choose validation service for linter")
            .run {
                validationServices
                    .find { it.linter == linter }
                    .toValidated { LinterValidationServiceError(linter) }
                    .toValidatedNel()
                    .peekLeft { log.error("No validation service found") }
                    .peek { log.debug("Found ${it.javaClass.simpleName}") }
                    .peek { log.info("Process schema validation with ${it.linter}") }
                    .flatMap { it.validate(log, server, json) }
            }
}
