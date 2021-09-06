package com.ggl.openapi.linter.plugin

import arrow.core.*
import com.fasterxml.jackson.databind.node.NullNode
import com.ggl.openapi.linter.plugin.display.Printer
import com.ggl.openapi.linter.plugin.model.Input
import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.FileNotFoundError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.GlobalRatesError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.InputError.InvalidRateError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.InputError.UnknownSeverityError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.RateError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.EmptyBodyError
import com.ggl.openapi.linter.plugin.model.Severity
import com.ggl.openapi.linter.plugin.model.Severity.MAY
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import com.ggl.openapi.linter.plugin.model.Threshold
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.reader.FileReader
import com.ggl.openapi.linter.plugin.resolver.Resolver
import com.ggl.openapi.linter.plugin.service.ValidationService
import com.ggl.openapi.linter.plugin.validator.InputValidator
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.apache.maven.monitor.logging.DefaultLog
import org.apache.maven.plugin.MojoFailureException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpenAPILinterMojoTest {

    @MockK
    private lateinit var inputValidator: InputValidator

    @MockK
    private lateinit var fileReader: FileReader

    @MockK
    private lateinit var printer: Printer

    @MockK
    private lateinit var resolver: Resolver

    @MockK
    private lateinit var validationService: ValidationService

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    private lateinit var mojo: OpenAPILinterMojo

    private companion object {
        private const val SCHEMA = "schema.yml"
        private const val SERVER = "ttp://127.0.0.1"
        private const val displayViolations = false

        private val thresholds = setOf(
            Threshold(MUST, 2),
            Threshold(MAY, 1)
        )

        private val thresholdMap = thresholds.associateBy(
            Threshold::severity andThen Severity::name,
            Threshold::rate andThen Int::toString
        )
        private val linter = ZALLY
        private val input = Input(linter, thresholds)
    }

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
        mojo = OpenAPILinterMojo()
            .resetField("server", SERVER)
            .resetField("schema", SCHEMA)
            .resetField("linter", linter.name)
            .resetField("thresholds", thresholdMap)
            .resetField("inputValidator", inputValidator)
            .resetField("fileReader", fileReader)
            .resetField("printer", printer)
            .resetField("resolver", resolver)
            .resetField("validationService", validationService)
            .resetField("displayViolations", displayViolations)
            .also { it.log = log }
    }

    @Test
    internal fun `It should validate schema`() {
        // Given
        val json = NullNode.instance
        val validationResponse = ValidationResponse(
            externalId = "externalId",
            apiDefinition = "apiDefinition",
            message = "message"
        )

        every { inputValidator.validate(log, linter.name, thresholdMap) } returns input.validNel()
        every { fileReader.read(log, SCHEMA) } returns json.validNel()
        every { validationService.validate(log, ZALLY, SERVER, json) } returns validationResponse.validNel()
        every { printer.display(log, validationResponse) } just Runs
        every { resolver.resolve(log, validationResponse, thresholds, displayViolations) } returns Unit.validNel()

        // When
        mojo.execute()

        // Then
        memoryLog.toString() `should be equal to` "[INFO] Processing with thresholds (MUST, 2), (MAY, 1)"

        verify { inputValidator.validate(log, linter.name, thresholdMap) }
        verify { fileReader.read(log, SCHEMA) }
        verify { validationService.validate(log, ZALLY, SERVER, json) }
        verify { printer.display(log, validationResponse) }
        verify { resolver.resolve(log, validationResponse, thresholds, displayViolations) }
    }

    @Test
    internal fun `It should do nothing when skipping schema validation`() {
        // Given
        mojo.resetField("skipSchemaValidation", true)

        // When
        mojo.execute()

        // Then
        memoryLog.toString().`should be empty`()
    }

    @Test
    internal fun `It should fail when thresholds are invalid`() {
        // Given
        every { inputValidator.validate(log, linter.name, thresholdMap) } returns nonEmptyListOf(
            UnknownSeverityError("severity"),
            InvalidRateError("rate")
        ).invalid()

        // When
        val action = mojo::execute

        // Then
        action `should throw` MojoFailureException::class `with message` "unknown threshold severity `severity`. You should provide values in [MUST, SHOULD, MAY, HINT]\nrate value should be positive"
        memoryLog.toString().`should be empty`()

        verify { inputValidator.validate(log, linter.name, thresholdMap) }
    }

    @Test
    internal fun `It should fail when an error occurs reading file`() {
        // Given
        every { inputValidator.validate(log, linter.name, thresholdMap) } returns input.validNel()
        every { fileReader.read(log, SCHEMA) } returns FileNotFoundError("path").invalidNel()

        // When
        val action = mojo::execute

        // Then
        action `should throw` MojoFailureException::class `with message` "File path not found"
        memoryLog.toString() `should be equal to` "[INFO] Processing with thresholds (MUST, 2), (MAY, 1)"

        verify { inputValidator.validate(log, linter.name, thresholdMap) }
        verify { fileReader.read(log, SCHEMA) }
    }

    @Test
    internal fun `It should fail when an error occurs when validating schema`() {
        // Given
        val json = NullNode.instance

        every { inputValidator.validate(log, linter.name, thresholdMap) } returns input.validNel()
        every { fileReader.read(log, SCHEMA) } returns json.validNel()
        every { validationService.validate(log, ZALLY, SERVER, json) } returns EmptyBodyError.invalidNel()

        // When
        val action = mojo::execute

        // Then
        action `should throw` MojoFailureException::class `with message` "The server response contains no body"
        memoryLog.toString() `should be equal to` "[INFO] Processing with thresholds (MUST, 2), (MAY, 1)"

        verify { inputValidator.validate(log, linter.name, thresholdMap) }
        verify { fileReader.read(log, SCHEMA) }
        verify { validationService.validate(log, ZALLY, SERVER, json) }
    }

    @Test
    internal fun `It should fail when an error occurs during resolution`() {
        // Given
        val json = NullNode.instance
        val validationResponse = ValidationResponse(
            externalId = "externalId",
            apiDefinition = "apiDefinition",
            message = "message"
        )

        every { inputValidator.validate(log, linter.name, thresholdMap) } returns input.validNel()
        every { fileReader.read(log, SCHEMA) } returns json.validNel()
        every { validationService.validate(any(), ZALLY, SERVER, json) } returns validationResponse.validNel()
        every { printer.display(log, validationResponse) } just Runs
        every { resolver.resolve(log, validationResponse, thresholds, displayViolations) } returns GlobalRatesError(
            listOf(
                RateError(threshold = Threshold(MUST, 3), count = 6)
            )
        ).invalidNel()

        // When
        val action = mojo::execute

        // Then
        action `should throw` MojoFailureException::class `with message` "Schema not validated.\nMUST: 6 errors [3 maximum]"
        memoryLog.toString() `should be equal to` "[INFO] Processing with thresholds (MUST, 2), (MAY, 1)"

        verify { inputValidator.validate(log, linter.name, thresholdMap) }
        verify { fileReader.read(log, SCHEMA) }
        verify { validationService.validate(log, ZALLY, SERVER, json) }
        verify { printer.display(log, validationResponse) }
        verify { resolver.resolve(log, validationResponse, thresholds, displayViolations) }
    }
}
