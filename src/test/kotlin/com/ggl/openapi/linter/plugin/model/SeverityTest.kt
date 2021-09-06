package com.ggl.openapi.linter.plugin.model

import com.ggl.arrow.addon.testing.`should be empty`
import com.ggl.arrow.addon.testing.`should contain`
import com.ggl.openapi.linter.plugin.model.Severity.*
import com.ggl.openapi.linter.plugin.model.Severity.Companion.findByName
import com.ggl.openapi.linter.plugin.model.Severity.Companion.values
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SeverityTest {

    @Test
    internal fun `It should get all severities sorted by criticity`() {
        // When
        val severities = values.toList()

        // Then
        severities `should be equal to` listOf(MUST, SHOULD, MAY, HINT)
    }

    @ParameterizedTest
    @EnumSource(Severity::class)
    internal fun `It should find severity by name`(severity: Severity) {
        // When
        val maybeSeverity = findByName(severity.name)

        // Then
        maybeSeverity `should contain` severity
    }

    @Test
    internal fun `It should return none when severity not found`() {
        // Given
        val name = "unknown"

        // When
        val maybeSeverity = findByName(name)

        // Then
        maybeSeverity.`should be empty`()
    }
}
