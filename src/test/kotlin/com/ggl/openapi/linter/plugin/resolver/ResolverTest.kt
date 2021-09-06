package com.ggl.openapi.linter.plugin.resolver

import com.ggl.arrow.addon.testing.`should contain invalidNel`
import com.ggl.arrow.addon.testing.`should contain valid`
import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.GlobalRatesError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.RateError
import com.ggl.openapi.linter.plugin.model.Severity
import com.ggl.openapi.linter.plugin.model.Severity.MAY
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import com.ggl.openapi.linter.plugin.model.Threshold
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.model.validation.Violation
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ResolverTest {

    private val resolver = Resolver()

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    private companion object {
        val violations = listOf(
            Violation(
                violationType = MUST,
                title = "title",
                description = "description",
                ruleLink = "ruleLink",
                paths = listOf("path"),
                pointer = "pointer"
            ),
            Violation(
                violationType = MUST,
                title = "title2",
                description = "description2",
                ruleLink = "ruleLink2",
                paths = listOf("path2", "path2-bis"),
                pointer = "pointer2"
            ),
            Violation(
                violationType = MUST,
                title = "title3",
                description = "description3",
                ruleLink = "ruleLink3",
                paths = listOf("path3"),
                pointer = "pointer3"
            ),
            Violation(
                violationType = MAY,
                title = "title4",
                description = "description4",
                ruleLink = "ruleLink4",
                paths = listOf("path4"),
                pointer = "pointer4"
            ),
            Violation(
                violationType = MAY,
                title = "title5",
                description = "description5",
                ruleLink = "ruleLink5",
                paths = listOf("path5"),
                pointer = "pointer5"
            )
        )
        private val validationResponse = ValidationResponse(
            externalId = "externalId",
            message = "message",
            apiDefinition = "apiDefinition",
            violationsCount = Severity.values.associateWith { severity -> violations.count { it.violationType == severity } },
            violations = violations
        )
    }

    @Test
    internal fun `It should be invalid when more violations than expected`() {
        // Given
        val mustThreshold = Threshold(MUST, rate = 2)
        val mayThreshold = Threshold(MAY, rate = 1)
        val thresholds: Set<Threshold> = setOf(mustThreshold, mayThreshold)
        val displayViolations = false

        // When
        val errorsOrUnit = resolver.resolve(log, validationResponse, thresholds, displayViolations)

        // Then
        errorsOrUnit `should contain invalidNel` GlobalRatesError(
            listOf(
                RateError(mustThreshold, 3),
                RateError(mayThreshold, 2)
            )
        )

        memoryLog.toString() `should be equal to` """
            [INFO] Comparing violations to thresholds
            [ERROR] MUST: 3 errors [2 maximum]
            [ERROR] MAY: 2 errors [1 maximum]
            [ERROR] Schema not validated
        """.trimIndent()
    }

    @Test
    internal fun `It should be valid when less violations than expected`() {
        // Given
        val mustThreshold = Threshold(MUST, rate = 4)
        val mayThreshold = Threshold(MAY, rate = 3)
        val thresholds: Set<Threshold> = setOf(mustThreshold, mayThreshold)
        val displayViolations = false

        // When
        val errorsOrUnit = resolver.resolve(log, validationResponse, thresholds, displayViolations)

        // Then
        errorsOrUnit `should contain valid` Unit

        memoryLog.toString() `should be equal to` """
            [INFO] Comparing violations to thresholds
            [WARNING] MUST: 3 errors [4 maximum]
            [WARNING] MAY: 2 errors [3 maximum]
            [INFO] Schema validated!
        """.trimIndent()
    }

    @Test
    internal fun `It should be valid and print violations`() {
        // Given
        val mustThreshold = Threshold(MUST, rate = 4)
        val mayThreshold = Threshold(MAY, rate = 3)
        val thresholds: Set<Threshold> = setOf(mustThreshold, mayThreshold)
        val displayViolations = true

        // When
        val errorsOrUnit = resolver.resolve(log, validationResponse, thresholds, displayViolations)

        // Then
        errorsOrUnit `should contain valid` Unit

        memoryLog.toString() `should be equal to` """
            [INFO] Comparing violations to thresholds
            [WARNING] MUST: 3 errors [4 maximum]
            [WARNING] MAY: 2 errors [3 maximum]
            [INFO] Violations per threshold
            [WARNING] -------------------------------
            [WARNING] 3 violations found for severity `MUST`:
            [WARNING] #1 ruleLink [1 violations]
            [WARNING] path
            [WARNING]
            [WARNING] #2 ruleLink2 [1 violations for 2 paths]
            [WARNING] path2
            [WARNING] path2-bis
            [WARNING]
            [WARNING] #3 ruleLink3 [1 violations]
            [WARNING] path3
            [WARNING] -------------------------------
            [WARNING] 2 violations found for severity `MAY`:
            [WARNING] #1 ruleLink4 [1 violations]
            [WARNING] path4
            [WARNING]
            [WARNING] #2 ruleLink5 [1 violations]
            [WARNING] path5
            [INFO] Schema validated!
        """.trimIndent()
    }
}
