package com.ggl.openapi.linter.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ggl.arrow.addon.testing.`should contain invalidNel`
import com.ggl.arrow.addon.testing.`should contain valid`
import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.DeserializationError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.EmptyBodyError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.ThirdPartyError.ServerError
import com.ggl.openapi.linter.plugin.model.validation.ValidationResponse
import com.ggl.openapi.linter.plugin.service.ZallyValidationService.Companion.API_DEFINITION
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import okhttp3.*
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ZallyValidationServiceTest {

    @MockK
    private lateinit var client: OkHttpClient

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    private lateinit var service: ZallyValidationService

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    private companion object {
        private const val JSON_REQUEST = "{\"key\": \"value\"}"
        private const val JSON_RESPONSE = "{\"key2\": \"value2\"}"
    }

    @Test
    internal fun `It should get validation response`() {
        // Given
        val call = mockk<Call>()
        val response = mockk<Response>()
        val validationResponse = mockk<ValidationResponse>()

        val server = "http://127.0.0.1"
        val jsonNode = NullNode.instance
        val objectNode = mockk<ObjectNode>()

        every { objectMapper.createObjectNode() } returns objectNode
        every { objectMapper.readValue(JSON_RESPONSE, ValidationResponse::class.java) } returns validationResponse
        every { client.newCall(any()) } returns call
        every { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) } returns objectNode
        every { objectNode.toString() } returns JSON_REQUEST
        every { call.execute() } returns response
        every { response.body() } returns ResponseBody.create(MediaType.get("application/json"), JSON_RESPONSE)

        // When
        val errorsOrValidationResponse = service.validate(log, server, jsonNode)

        // Then
        errorsOrValidationResponse `should contain valid` validationResponse

        memoryLog.toString() `should be equal to` """
            [INFO] Validate schema on server http://127.0.0.1
            [DEBUG] call zally server: POST http://127.0.0.1/api-violations
            [DEBUG] > Request body:
            $JSON_REQUEST
            [DEBUG] > Response body:
            $JSON_RESPONSE
            [DEBUG] zally response parsed successfully
        """.trimIndent()

        verify { objectMapper.createObjectNode() }
        verify { objectMapper.readValue(JSON_RESPONSE, ValidationResponse::class.java) }
        verify { client.newCall(any()) }
        verify { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) }
        verify { objectNode.toString() }
        verify { call.execute() }
        verify { response.body() }
    }

    @Test
    internal fun `It should be invalid when server response is not parsable`() {
        // Given
        val call = mockk<Call>()
        val response = mockk<Response>()
        val throwable = RuntimeException()

        val server = "http://127.0.0.1"
        val jsonNode = NullNode.instance
        val objectNode = mockk<ObjectNode>()

        every { objectMapper.createObjectNode() } returns objectNode
        every { objectMapper.readValue(JSON_RESPONSE, ValidationResponse::class.java) } throws throwable
        every { client.newCall(any()) } returns call
        every { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) } returns objectNode
        every { objectNode.toString() } returns JSON_REQUEST
        every { call.execute() } returns response
        every { response.body() } returns ResponseBody.create(MediaType.get("application/json"), JSON_RESPONSE)

        // When
        val errorsOrValidationResponse = service.validate(log, server, jsonNode)

        // Then
        errorsOrValidationResponse `should contain invalidNel` DeserializationError(throwable)

        memoryLog.toString() `should be equal to` """
            [INFO] Validate schema on server http://127.0.0.1
            [DEBUG] call zally server: POST http://127.0.0.1/api-violations
            [DEBUG] > Request body:
            $JSON_REQUEST
            [DEBUG] > Response body:
            $JSON_RESPONSE
            [ERROR] error parsing zally response
        """.trimIndent()

        verify { objectMapper.createObjectNode() }
        verify { objectMapper.readValue(JSON_RESPONSE, ValidationResponse::class.java) }
        verify { client.newCall(any()) }
        verify { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) }
        verify { objectNode.toString() }
        verify { call.execute() }
        verify { response.body() }
    }

    @Test
    internal fun `It should be invalid when cannot communicate with server`() {
        // Given
        val call = mockk<Call>()
        val throwable = RuntimeException()
        val serverError = ServerError(throwable)

        val server = "http://127.0.0.1"
        val jsonNode = NullNode.instance
        val objectNode = mockk<ObjectNode>()

        every { objectMapper.createObjectNode() } returns objectNode
        every { client.newCall(any()) } returns call
        every { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) } returns objectNode
        every { objectNode.toString() } returns JSON_REQUEST
        every { call.execute() } throws throwable

        // When
        val errorsOrValidationResponse = service.validate(log, server, jsonNode)

        // Then
        errorsOrValidationResponse `should contain invalidNel` serverError

        memoryLog.toString() `should be equal to` """
            [INFO] Validate schema on server http://127.0.0.1
            [DEBUG] call zally server: POST http://127.0.0.1/api-violations
            [DEBUG] > Request body:
            $JSON_REQUEST
            [ERROR] call to zally server failed: ServerError(throwable=java.lang.RuntimeException)
        """.trimIndent()

        verify { objectMapper.createObjectNode() }
        verify { client.newCall(any()) }
        verify { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) }
        verify { objectNode.toString() }
        verify { call.execute() }
    }

    @Test
    internal fun `It should be invalid when server response is empty`() {
        // Given
        val call = mockk<Call>()
        val response = mockk<Response>()

        val server = "http://127.0.0.1"
        val content = ""
        val jsonNode = NullNode.instance
        val objectNode = mockk<ObjectNode>()

        every { objectMapper.createObjectNode() } returns objectNode
        every { client.newCall(any()) } returns call
        every { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) } returns objectNode
        every { objectNode.toString() } returns JSON_REQUEST
        every { call.execute() } returns response
        every { response.body() } returns ResponseBody.create(MediaType.get("application/json"), content)

        // When
        val errorsOrValidationResponse = service.validate(log, server, jsonNode)

        // Then
        errorsOrValidationResponse `should contain invalidNel` EmptyBodyError

        memoryLog.toString() `should be equal to` """
            [INFO] Validate schema on server http://127.0.0.1
            [DEBUG] call zally server: POST http://127.0.0.1/api-violations
            [DEBUG] > Request body:
            $JSON_REQUEST
            [DEBUG] > Response body:

        """.trimIndent()

        verify { objectMapper.createObjectNode() }
        verify { client.newCall(any()) }
        verify { objectNode.set<ObjectNode>(API_DEFINITION, jsonNode) }
        verify { objectNode.toString() }
        verify { call.execute() }
        verify { response.body() }
    }
}
