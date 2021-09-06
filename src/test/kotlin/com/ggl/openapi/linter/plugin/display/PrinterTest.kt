package com.ggl.openapi.linter.plugin.display

import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.Severity.HINT
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.model.validation.Violation
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.Test

internal class PrinterTest {

    private val printer = Printer()

    @Test
    internal fun `It should display violations`() {
        // Given
        val memoryLog = MemoryLog()
        val log = DefaultLog(memoryLog)

        val response = ValidationResponse(
            externalId = "externalId",
            message = "message",
            apiDefinition = "apiDefinition",
            violations = listOf(
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
                    violationType = HINT,
                    title = "title3",
                    description = "description3",
                    ruleLink = "ruleLink3",
                    paths = listOf("path3"),
                    pointer = "pointer3"
                )
            )
        )

        // When
        printer.display(log, response)

        // Then
        memoryLog.toString() `should be equal to` """
            [DEBUG] Rule violations


            ====================================================================
            |                        RULE VIOLATIONS                           |
            ====================================================================


                `MUST` SEVERITY: 2 violations found
            --------------------------------------------------
            # Violation n°1
            title: title
            description: description
            rule: ruleLink
            paths: [path]

            # Violation n°2
            title: title2
            description: description2
            rule: ruleLink2
            paths: [path2, path2-bis]


                `HINT` SEVERITY: 1 violations found
            --------------------------------------------------
            # Violation n°1
            title: title3
            description: description3
            rule: ruleLink3
            paths: [path3]
        """.trimIndent()
    }
}
