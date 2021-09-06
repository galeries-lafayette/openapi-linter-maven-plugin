package com.ggl.openapi.linter.plugin.resolver

import arrow.core.None
import arrow.core.ValidatedNel
import arrow.core.getOrElse
import arrow.core.valid
import com.ggl.arrow.addon.ListExtensions.find
import com.ggl.arrow.addon.MapExtensions.getOption
import com.ggl.arrow.addon.OptionExtensions.peek
import com.ggl.arrow.addon.OptionExtensions.peekLeft
import com.ggl.arrow.addon.ValidatedExtensions.check
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.arrow.addon.ValidatedExtensions.peekLeft
import com.ggl.arrow.addon.ValidatedExtensions.sequence
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.GlobalRatesError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.RateError
import com.ggl.openapi.linter.plugin.model.Severity
import com.ggl.openapi.linter.plugin.model.Threshold
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.model.validation.Violation
import org.apache.maven.plugin.logging.Log
import kotlin.Int.Companion.MAX_VALUE

class Resolver {

    fun resolve(log: Log, response: ValidationResponse, thresholds: Set<Threshold>, displayViolations: Boolean): ValidatedNel<OpenAPILinterError, Unit> =
        validate(log, response.violationsCount, thresholds)
            .also {
                if (displayViolations) {
                    displayViolations(log, response, thresholds)
                }
            }
            .peek { log.info("Schema validated!") }
            .peekLeft { log.error("Schema not validated") }
            .toValidatedNel()

    private fun validate(log: Log, violationsCount: Map<Severity, Int>, thresholds: Set<Threshold>) =
        log.info("Comparing violations to thresholds")
            .run {
                violationsCount
                    .filterValues { it > 0 }
                    .map { (severity, count) ->
                        Triple(
                            severity,
                            count,
                            thresholds.find { it.severity == severity }
                        )
                    }
                    .map { (severity, count, maybeThreshold) ->
                        maybeThreshold
                            .peekLeft { log.warn("$severity: $count errors") }
                            .map {
                                val message = "$severity: $count errors [${it.rate} maximum]"
                                check(it.rate >= count) { RateError(it, count) }
                                    .peekLeft { log.error(message) }
                                    .peek { log.warn(message) }
                            }
                            .getOrElse { None.valid() }
                    }
                    .sequence()
                    .bimap({ GlobalRatesError(it.all) }) { }
            }

    private fun displayViolations(log: Log, response: ValidationResponse, thresholds: Set<Threshold>) =
        log.info("Violations per threshold")
            .run {
                response
                    .violations
                    .groupBy { it.violationType }
                    .map { (severity, violations) ->
                        log.warn("-------------------------------")
                        thresholds
                            .associateBy { it.severity }
                            .completeWithDefault()
                            .getOption(severity)
                            .peek { displayViolationsForSeverity(log, it, severity, violations) }
                    }
            }

    private fun displayViolationsForSeverity(log: Log, threshold: Threshold, severity: Severity, violations: List<Violation>) {
        val errorCount = violations.size
        log.warn("$errorCount violations found for severity `$severity`:")

        violations
            .groupBy { it.ruleLink }
            .toList()
            .withIndex()
            .forEach { (index, ruleLinkWithViolations) ->
                val paths = ruleLinkWithViolations.second.flatMap { it.paths }.filter { it.isNotBlank() }.toSet()
                val pathsMessage = if (ruleLinkWithViolations.second.size != paths.size) " for ${paths.size} paths" else ""
                if (index > 0) log.warn("")
                log.warn("#${index + 1} ${ruleLinkWithViolations.first} [${ruleLinkWithViolations.second.size} violations$pathsMessage]")
                paths.forEach(log::warn)
            }

        if (errorCount >= threshold.rate)
            log.error("too many $severity violations: $errorCount is greater than ${threshold.rate}")
    }

    private fun Map<Severity, Threshold>.completeWithDefault() =
        this +
            Severity.values
                .filterNot(::containsKey)
                .associateWith { Threshold(it, MAX_VALUE) }
}
