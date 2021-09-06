package com.ggl.openapi.linter.plugin.display

import com.ggl.openapi.linter.plugin.model.Severity
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.model.validation.Violation
import org.apache.maven.plugin.logging.Log

class Printer {

    private companion object {
        private const val SEPARATOR = "\n\n\n"
    }

    fun display(log: Log, response: ValidationResponse) =
        log.debug(
            "Rule violations" + SEPARATOR + header() + SEPARATOR +
                response.violations
                    .groupBy { it.violationType }
                    .toSortedMap(compareBy { it.criticity })
                    .map { (severity, violations) ->
                        severity.message(violations.size) + "\n" +
                            violations
                                .withIndex()
                                .joinToString("\n\n") { it.message() }
                    }
                    .joinToString(SEPARATOR)
        )

    private fun header() = """
        ====================================================================
        |                        RULE VIOLATIONS                           |
        ====================================================================
    """.trimIndent()

    private fun Severity.message(nbrRules: Int) =
        """
            `$name` SEVERITY: $nbrRules violations found
        --------------------------------------------------
        """
            .trimIndent()

    private fun IndexedValue<Violation>.message() =
        with(value) {
            """
            # Violation n°${index + 1}
            title: $title
            description: $description
            rule: $ruleLink
            paths: [${paths.joinToString()}]
            """
        }
            .trimIndent()
}
