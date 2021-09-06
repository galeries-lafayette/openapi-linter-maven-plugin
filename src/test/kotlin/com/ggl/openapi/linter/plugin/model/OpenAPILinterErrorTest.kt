package com.ggl.openapi.linter.plugin.model

import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.*
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.FileNotFoundError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.ReadFileError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.InputError.*
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.EmptyBodyError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.ServerError
import com.ggl.openapi.linter.plugin.model.Severity.MAY
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class OpenAPILinterErrorTest {

    @Test
    internal fun `It should have toString method`() {
        // Given
        val error = RateError(Threshold(MUST, 1), 2)

        // When
        val string = error.toString()

        // Then
        string `should be equal to` "MUST: 2 errors [1 maximum]"
    }

    @ParameterizedTest(name = "It should get error message for {0}")
    @MethodSource("errorWithMessage")
    internal fun `It should get error message`(error: OpenAPILinterError, message: String) {
        // When
        val errorMessage = error.message

        // Then
        errorMessage `should be equal to` message
    }

    private companion object {
        @JvmStatic
        private fun errorWithMessage() = listOf(
            arguments(InvalidLinterError("linter"), "unknown linter `linter`. You should provide values in [ZALLY]"),
            arguments(UnknownSeverityError("severity"), "unknown threshold severity `severity`. You should provide values in [MUST, SHOULD, MAY, HINT]"),
            arguments(InvalidRateError("rate"), "rate value should be positive"),
            arguments(FileNotFoundError("path"), "File path not found"),
            arguments(ReadFileError("path"), "Cannot read file path"),
            arguments(EmptyBodyError, "The server response contains no body"),
            arguments(ServerError(RuntimeException("message")), "message"),
            arguments(LinterValidationServiceError(ZALLY), "No linter validation service found for ZALLY"),
            arguments(
                GlobalRatesError(
                    listOf(
                        RateError(Threshold(MUST, 1), 2),
                        RateError(Threshold(MAY, 3), 4)
                    )
                ),
                "Schema not validated.\nMUST: 2 errors [1 maximum]\nMAY: 4 errors [3 maximum]"
            )
        )
    }
}
