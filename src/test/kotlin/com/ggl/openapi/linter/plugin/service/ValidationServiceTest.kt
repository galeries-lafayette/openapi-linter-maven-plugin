package com.ggl.openapi.linter.plugin.service

import arrow.core.validNel
import com.fasterxml.jackson.databind.node.NullNode
import com.ggl.arrow.addon.testing.`should contain invalidNel`
import com.ggl.arrow.addon.testing.`should contain valid`
import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.Linter
import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.LinterValidationServiceError
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ValidationServiceTest {

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    @Test
    internal fun `It should get validation response`() {
        // Given
        val server = "server"
        val json = NullNode.instance
        val linter = ZALLY

        val linterValidationService = mockk<ZallyValidationService>()
        val validationResponse = mockk<ValidationResponse>()

        val validationService = ValidationService(setOf(linterValidationService))

        every { linterValidationService.validate(log, server, json) } returns validationResponse.validNel()
        every { linterValidationService.linter } returns linter

        // When
        val errorsOrValidationResponse = validationService.validate(log, linter, server, json)

        // Then
        errorsOrValidationResponse `should contain valid` validationResponse

        memoryLog.toString() `should be equal to` """
            [DEBUG] Choose validation service for linter
            [DEBUG] Found ZallyValidationService
            [INFO] Process schema validation with ZALLY
        """.trimIndent()

        verify { linterValidationService.validate(log, server, json) }
        verify { linterValidationService.linter }
    }

    @Test
    internal fun `It should be invalid when no service found for linter`() {
        // Given
        val server = "server"
        val json = NullNode.instance

        val linter = mockk<Linter>()
        val validationService = ValidationService(emptySet())

        // When
        val errorsOrValidationResponse = validationService.validate(log, linter, server, json)

        // Then
        errorsOrValidationResponse `should contain invalidNel` LinterValidationServiceError(linter)

        memoryLog.toString() `should be equal to` """
            [DEBUG] Choose validation service for linter
            [ERROR] No validation service found
        """.trimIndent()
    }
}
