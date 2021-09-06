package com.ggl.openapi.linter.plugin.validator

import arrow.core.ValidatedNel
import arrow.core.flatten
import arrow.core.toOption
import com.ggl.arrow.addon.OptionExtensions.toValidated
import com.ggl.arrow.addon.ValidatedExtensions.combine
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.arrow.addon.ValidatedExtensions.peekLeft
import com.ggl.arrow.addon.ValidatedExtensions.sequence
import com.ggl.openapi.linter.plugin.model.*
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.InputError.*
import org.apache.maven.plugin.logging.Log

class InputValidator {

    fun validate(log: Log, linter: String, thresholds: Map<String, String>): ValidatedNel<OpenAPILinterError, Input> =
        log.info("Validate plugin configuration")
            .run {
                combine(
                    validateLinter(log, linter),
                    validateThresholds(log, thresholds)
                ) { linter, thresholds -> Input(linter, thresholds) }
                    .mapLeft { it.flatten() }
            }

    private fun validateLinter(log: Log, linter: String) =
        log.debug("Validating linter `$linter`")
            .run {
                Linter.findByName(linter)
                    .toValidated { InvalidLinterError(linter) }
                    .toValidatedNel()
            }

    private fun validateThresholds(log: Log, thresholds: Map<String, String>) =
        thresholds.entries
            .map { validateThreshold(log, it.key, it.value) }
            .sequence()
            .mapLeft { it.flatten() }
            .map { it.toSet() }

    private fun validateThreshold(log: Log, name: String, rate: String) =
        log.debug("Validating threshold [$name: $rate]")
            .run {
                combine(
                    validateSeverity(log, name),
                    validateRate(log, rate)
                ) { level, rate -> Threshold(level, rate) }
            }
            .peek { log.debug("threshold [$name: $rate] is successfully validated") }

    private fun validateSeverity(log: Log, name: String) =
        log.debug("Validating severity for `$name`")
            .run {
                Severity
                    .findByName(name)
                    .toValidated { UnknownSeverityError(name) }
                    .peekLeft { log.error("error found for name $name: $it") }
                    .peek { log.debug("severity `$name` is successfully validated") }
            }

    private fun validateRate(log: Log, rate: String) =
        log.debug("Validating rate `$rate`")
            .run {
                rate
                    .toIntOrNull()
                    .toOption()
                    .filter { it >= 0 }
                    .toValidated { InvalidRateError(rate) }
                    .peekLeft { log.error("invalid value `$rate`: rate should be positive") }
                    .peek { log.debug("rate `$rate` is successfully validated") }
            }
}
