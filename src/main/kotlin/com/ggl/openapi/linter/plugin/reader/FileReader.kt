package com.ggl.openapi.linter.plugin.reader

import arrow.core.ValidatedNel
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ggl.arrow.addon.ValidatedExtensions.check
import com.ggl.arrow.addon.ValidatedExtensions.flatMap
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.arrow.addon.ValidatedExtensions.peekLeft
import com.ggl.arrow.addon.toValidated
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.FileNotFoundError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.ReadFileError
import org.apache.maven.plugin.logging.Log
import java.io.File

class FileReader(private val objectMapper: ObjectMapper) {

    fun read(log: Log, path: String): ValidatedNel<OpenAPILinterError, JsonNode> =
        log.info("Reading file `$path`")
            .run {
                with(File(path)) {
                    check(exists()) { FileNotFoundError(path) }
                        .peekLeft { log.error("File `$path` not found") }
                        .peek { log.debug("File `$path` found") }
                        .flatMap { read(log, it) }
                        .toValidatedNel()
                }
            }

    private fun read(log: Log, file: File) =
        log.debug("Parsing schema")
            .run {
                runCatching { objectMapper.readTree(file) }
                    .toValidated { ReadFileError(file.path) }
                    .peekLeft { log.error("cannot parse schema") }
                    .peek { log.debug("schema parsed") }
            }
}
