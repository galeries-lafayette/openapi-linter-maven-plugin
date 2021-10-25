package com.ggl.openapi.linter.plugin

import com.ggl.openapi.linter.plugin.model.Linter.ZALLY
import com.ggl.openapi.linter.plugin.model.Severity.MAY
import com.ggl.openapi.linter.plugin.model.Severity.MUST
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.`with message`
import org.apache.maven.monitor.logging.DefaultLog
import org.apache.maven.plugin.MojoFailureException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WireMockTest
internal class OpenAPILinterMojoIT {

    private companion object {
        private const val SCHEMA = "src/test/resources/schema/sample-api.yaml"
        private val linter = ZALLY

        private val memoryLog = MemoryLog(1)
        private val log = DefaultLog(memoryLog)
    }

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    @Test
    internal fun `It should validate schema`(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        // Given
        val server = wireMockRuntimeInfo.httpBaseUrl
        val mojo = OpenAPILinterMojo()
            .resetField("server", server)
            .resetField("schema", SCHEMA)
            .resetField("linter", linter.name)
            .resetField("displayViolations", true)
            .resetField("thresholds", mapOf(MUST.name to "159", MAY.name to "16"))
            .also { it.log = log }

        // When
        mojo.execute()

        // Then
        memoryLog.toString() `should be equal to` """
            [INFO] Validate plugin configuration
            [INFO] Processing with thresholds (MUST, 159), (MAY, 16)
            [INFO] Reading file `$SCHEMA`
            [INFO] Process schema validation with $linter
            [INFO] Validate schema on server $server
            [INFO] Comparing violations to thresholds
            [WARNING] MUST: 11 errors [159 maximum]
            [WARNING] MAY: 1 errors [16 maximum]
            [INFO] Violations per threshold
            [WARNING] -------------------------------
            [WARNING] 11 violations found for severity `MUST`:
            [WARNING] #1 https://zalando.github.io/restful-api-guidelines/#219 [1 violations]
            [WARNING] /info/x-audience
            [WARNING]
            [WARNING] #2 https://zalando.github.io/restful-api-guidelines/#215 [1 violations]
            [WARNING] /info/x-api-id
            [WARNING]
            [WARNING] #3 https://zalando.github.io/restful-api-guidelines/#218 [3 violations]
            [WARNING] /info/contact/url
            [WARNING] /info/contact/name
            [WARNING] /info/contact/email
            [WARNING]
            [WARNING] #4 https://zalando.github.io/restful-api-guidelines/#115 [1 violations]
            [WARNING] /servers/0
            [WARNING]
            [WARNING] #5 https://zalando.github.io/restful-api-guidelines/#105 [1 violations]
            [WARNING] /paths/~1users/get
            [WARNING]
            [WARNING] #6 https://zalando.github.io/restful-api-guidelines/#104 [1 violations]
            [WARNING] /components/securitySchemes
            [WARNING]
            [WARNING] #7 https://zalando.github.io/restful-api-guidelines/#110 [1 violations]
            [WARNING] /paths/~1users/get/responses/200/content/application~1json/schema
            [WARNING]
            [WARNING] #8 https://zalando.github.io/restful-api-guidelines/#101 [1 violations for 0 paths]
            [WARNING]
            [WARNING] #9 https://github.com/zalando/zally/blob/master/server/rules.md#m011-tag-all-operations [1 violations]
            [WARNING] /paths/~1users/get
            [WARNING] -------------------------------
            [WARNING] 1 violations found for severity `MAY`:
            [WARNING] #1 https://zalando.github.io/restful-api-guidelines/#151 [1 violations]
            [WARNING] /paths/~1users/get
            [INFO] Schema validated!
        """.trimIndent()
    }

    @Test
    internal fun `It should not validate schema when too many violations`(wireMockRuntimeInfo: WireMockRuntimeInfo) {
        // Given
        val server = wireMockRuntimeInfo.httpBaseUrl
        val mojo = OpenAPILinterMojo()
            .resetField("server", server)
            .resetField("schema", SCHEMA)
            .resetField("linter", linter.name)
            .resetField("displayViolations", true)
            .resetField("thresholds", mapOf(MUST.name to "2", MAY.name to "1"))
            .also { it.log = log }

        // When
        val action = mojo::execute

        // Then
        action `should throw` MojoFailureException::class `with message`
            """
                Schema not validated.
                MUST: 11 errors [2 maximum]
            """.trimIndent()

        memoryLog.toString() `should be equal to` """
            [INFO] Validate plugin configuration
            [INFO] Processing with thresholds (MUST, 2), (MAY, 1)
            [INFO] Reading file `$SCHEMA`
            [INFO] Process schema validation with $linter
            [INFO] Validate schema on server $server
            [INFO] Comparing violations to thresholds
            [ERROR] MUST: 11 errors [2 maximum]
            [WARNING] MAY: 1 errors [1 maximum]
            [INFO] Violations per threshold
            [WARNING] -------------------------------
            [WARNING] 11 violations found for severity `MUST`:
            [WARNING] #1 https://zalando.github.io/restful-api-guidelines/#219 [1 violations]
            [WARNING] /info/x-audience
            [WARNING]
            [WARNING] #2 https://zalando.github.io/restful-api-guidelines/#215 [1 violations]
            [WARNING] /info/x-api-id
            [WARNING]
            [WARNING] #3 https://zalando.github.io/restful-api-guidelines/#218 [3 violations]
            [WARNING] /info/contact/url
            [WARNING] /info/contact/name
            [WARNING] /info/contact/email
            [WARNING]
            [WARNING] #4 https://zalando.github.io/restful-api-guidelines/#115 [1 violations]
            [WARNING] /servers/0
            [WARNING]
            [WARNING] #5 https://zalando.github.io/restful-api-guidelines/#105 [1 violations]
            [WARNING] /paths/~1users/get
            [WARNING]
            [WARNING] #6 https://zalando.github.io/restful-api-guidelines/#104 [1 violations]
            [WARNING] /components/securitySchemes
            [WARNING]
            [WARNING] #7 https://zalando.github.io/restful-api-guidelines/#110 [1 violations]
            [WARNING] /paths/~1users/get/responses/200/content/application~1json/schema
            [WARNING]
            [WARNING] #8 https://zalando.github.io/restful-api-guidelines/#101 [1 violations for 0 paths]
            [WARNING]
            [WARNING] #9 https://github.com/zalando/zally/blob/master/server/rules.md#m011-tag-all-operations [1 violations]
            [WARNING] /paths/~1users/get
            [ERROR] too many MUST violations: 11 is greater than 2
            [WARNING] -------------------------------
            [WARNING] 1 violations found for severity `MAY`:
            [WARNING] #1 https://zalando.github.io/restful-api-guidelines/#151 [1 violations]
            [WARNING] /paths/~1users/get
            [ERROR] too many MAY violations: 1 is greater than 1
            [ERROR] Schema not validated
        """.trimIndent()
    }
}
