package com.ggl.openapi.linter.plugin.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ggl.arrow.addon.ValidatedExtensions.check
import com.ggl.arrow.addon.ValidatedExtensions.flatMap
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.arrow.addon.ValidatedExtensions.peekLeft
import com.ggl.arrow.addon.toValidated
import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.DeserializationError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.EmptyBodyError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.ServerError
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import okhttp3.MediaType.parse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.maven.plugin.logging.Log

class ZallyValidationService(
    private val client: OkHttpClient,
    private val objectMapper: ObjectMapper
) : LinterValidationService(ZALLY) {

    companion object {
        const val API_DEFINITION = "api_definition"
    }

    override fun validate(log: Log, server: String, json: JsonNode) =
        log.info("Validate schema on server $server")
            .run {
                callServer(log, server, json.addRootElement())
                    .flatMap { it.check(it.isNotBlank()) { EmptyBodyError } }
                    .flatMap { readJson(log, it) }
                    .toValidatedNel()
            }

    private fun callServer(log: Log, server: String, body: JsonNode) =
        runCatching {
            log.debug("call zally server: POST $server/api-violations")
            log.debug("> Request body:\n$body")

            client
                .newCall(
                    Request.Builder()
                        .url("$server/api-violations")
                        .post(RequestBody.create(parse("application/json"), body.toString()))
                        .build()
                )
                .execute()
        }
            .toValidated(::ServerError)
            .peekLeft { log.error("call to zally server failed: $it") }
            .map { it.body()?.string().orEmpty() }
            .peek { log.debug("> Response body:\n$it") }

    private fun JsonNode.addRootElement() =
        objectMapper
            .createObjectNode()
            .set<ObjectNode>(API_DEFINITION, this)

    private fun readJson(log: Log, str: String) =
        runCatching { objectMapper.readValue(str, ValidationResponse::class.java) }
            .toValidated(::DeserializationError)
            .peekLeft { log.error("error parsing zally response") }
            .peek { log.debug("zally response parsed successfully") }
}
