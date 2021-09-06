package com.ggl.openapi.linter.plugin.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.ggl.openapi.linter.plugin.model.Severity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.`should be`
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SeverityDeserializerTest {

    private val deserializer = SeverityDeserializer()

    @ParameterizedTest
    @EnumSource(Severity::class)
    internal fun `It should deserialize`(given: Severity) {
        // Given
        val jsonParser = mockk<JsonParser>()
        val context = mockk<DeserializationContext>()

        every { jsonParser.text } returns given.name.toLowerCase()

        // When
        val severity = deserializer.deserialize(jsonParser, context)

        // Then
        severity `should be` given

        verify { jsonParser.text }
    }
}
