package com.ggl.openapi.linter.plugin.validator

import com.ggl.arrow.addon.testing.`should contain all invalid`
import com.ggl.arrow.addon.testing.`should contain valid`
import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.Input
import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.InputError.*
import com.ggl.openapi.linter.plugin.model.Severity.MAY
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import com.ggl.openapi.linter.plugin.model.Threshold
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InputValidatorTest {

    private val validator = InputValidator()

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    @Test
    internal fun `It should validate thresholds when empty map`() {
        // Given
        val thresholds = emptyMap<String, String>()
        val linter = ZALLY

        // When
        val errorsOrThresholds = validator.validate(log, linter.name, thresholds)

        // Then
        errorsOrThresholds `should contain valid` Input(linter, emptySet())

        memoryLog.toString() `should be equal to` """
            [INFO] Validate plugin configuration
            [DEBUG] Validating linter `$linter`
        """.trimIndent()
    }

    @Test
    internal fun `It should validate thresholds`() {
        // Given
        val linter = ZALLY
        val severity1 = MUST
        val severity2 = MAY

        val rate1 = 5
        val rate2 = 10

        val thresholds = mapOf(
            severity1.name to rate1.toString(),
            severity2.name to rate2.toString()
        )

        // When
        val errorsOrThresholds = validator.validate(log, linter.name, thresholds)

        // Then
        errorsOrThresholds `should contain valid` Input(
            linter = linter,
            thresholds = setOf(
                Threshold(severity1, rate1),
                Threshold(severity2, rate2)
            )
        )

        memoryLog.toString() `should be equal to` """
            [INFO] Validate plugin configuration
            [DEBUG] Validating linter `$linter`
            [DEBUG] Validating threshold [MUST: 5]
            [DEBUG] Validating severity for `MUST`
            [DEBUG] severity `MUST` is successfully validated
            [DEBUG] Validating rate `5`
            [DEBUG] rate `5` is successfully validated
            [DEBUG] threshold [MUST: 5] is successfully validated
            [DEBUG] Validating threshold [MAY: 10]
            [DEBUG] Validating severity for `MAY`
            [DEBUG] severity `MAY` is successfully validated
            [DEBUG] Validating rate `10`
            [DEBUG] rate `10` is successfully validated
            [DEBUG] threshold [MAY: 10] is successfully validated
        """.trimIndent()
    }

    @Test
    internal fun `It should be invalid when wrong inputs`() {
        // Given
        val linter = "bad-linter"
        val severity = "bad-severity"
        val rate = "bad-number"
        val thresholds = mapOf(severity to rate)

        val invalidLinterError = InvalidLinterError(linter)
        val unknownSeverityError = UnknownSeverityError(severity)
        val invalidRateError = InvalidRateError(rate)

        // When
        val errorsOrThresholds = validator.validate(log, linter, thresholds)

        // Then
        errorsOrThresholds `should contain all invalid` listOf(invalidLinterError, unknownSeverityError, invalidRateError)

        memoryLog.toString() `should be equal to` """
            [INFO] Validate plugin configuration
            [DEBUG] Validating linter `$linter`
            [DEBUG] Validating threshold [bad-severity: bad-number]
            [DEBUG] Validating severity for `bad-severity`
            [ERROR] error found for name bad-severity: UnknownSeverityError(severity=bad-severity)
            [DEBUG] Validating rate `bad-number`
            [ERROR] invalid value `bad-number`: rate should be positive
        """.trimIndent()
    }
}
