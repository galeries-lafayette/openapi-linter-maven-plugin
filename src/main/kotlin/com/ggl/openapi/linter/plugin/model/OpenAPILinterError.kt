package com.ggl.openapi.linter.plugin.model

sealed class OpenAPILinterError(open val message: String) {

    sealed class InputError(override val message: String) : OpenAPILinterError(message) {
        data class UnknownSeverityError(val severity: String) : InputError("unknown threshold severity `$severity`. You should provide values in ${Severity.values}")
        data class InvalidRateError(val rate: String) : InputError("$rate value should be positive")
        data class InvalidLinterError(val linter: String) : InputError("unknown linter `$linter`. You should provide values in ${Linter.values}")
    }

    sealed class FileError(override val message: String) : OpenAPILinterError(message) {
        data class FileNotFoundError(val path: String) : FileError("File $path not found")
        data class ReadFileError(val path: String) : FileError("Cannot read file $path")
    }

    sealed class ThirdPartyError(override val message: String) : OpenAPILinterError(message) {
        object EmptyBodyError : ThirdPartyError("The server response contains no body")
        data class ServerError(val throwable: Throwable) : ThirdPartyError(throwable.message.orEmpty())
    }

    data class LinterValidationServiceError(val linter: Linter) : OpenAPILinterError("No linter validation service found for $linter")

    data class DeserializationError(val throwable: Throwable) : OpenAPILinterError("Unable to deserialize: ${throwable.message}")

    data class RateError(val threshold: Threshold, val count: Int) {
        override fun toString() = "${threshold.severity.name}: $count errors [${threshold.rate} maximum]"
    }

    data class GlobalRatesError(val errors: List<RateError>) : OpenAPILinterError("Schema not validated.\n${errors.joinToString("\n")}")
}
