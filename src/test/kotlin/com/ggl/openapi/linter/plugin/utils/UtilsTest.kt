package com.ggl.openapi.linter.plugin.utils

import arrow.core.ValidatedNel
import arrow.core.invalid
import arrow.core.nonEmptyListOf
import arrow.core.validNel
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.FileNotFoundError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.ReadFileError
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.apache.maven.plugin.MojoFailureException
import org.junit.jupiter.api.Test

internal class UtilsTest {

    @Test
    internal fun `It should get value when validated is valid`() {
        // Given
        val value = "value"
        val validated = value.validNel()

        // When
        val result = validated.handleErrors()

        // Then
        result `should be` value
    }

    @Test
    internal fun `It should throw error when validated is invalid`() {
        // Given
        val error1 = FileNotFoundError("path1")
        val error2 = ReadFileError("path2")
        val validated: ValidatedNel<OpenAPILinterError, Any> = nonEmptyListOf(error1, error2).invalid()

        // When
        val action = validated::handleErrors

        // Then
        action `should throw` MojoFailureException::class `with message` "File path1 not found\nCannot read file path2"
    }
}
