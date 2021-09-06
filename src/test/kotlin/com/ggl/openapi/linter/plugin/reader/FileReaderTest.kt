package com.ggl.openapi.linter.plugin.reader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.ggl.arrow.addon.testing.`should contain invalidNel`
import com.ggl.arrow.addon.testing.`should contain valid`
import com.ggl.openapi.linter.plugin.MemoryLog
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.FileNotFoundError
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError.FileError.ReadFileError
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.amshove.kluent.`should be equal to`
import org.apache.maven.monitor.logging.DefaultLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Paths

@ExtendWith(MockKExtension::class)
internal class FileReaderTest {

    @MockK
    private lateinit var objectMapper: ObjectMapper

    @InjectMockKs
    private lateinit var fileReader: FileReader

    private val memoryLog = MemoryLog()
    private val log = DefaultLog(memoryLog)

    @BeforeEach
    private fun setUp() {
        memoryLog.clear()
    }

    @Test
    internal fun `It should read file`() {
        // Given
        val path = getPath("pom.xml")
        val file = File(path)
        val json = NullNode.instance

        every { objectMapper.readTree(file) } returns json

        // When
        val errorsOrJson = fileReader.read(log, path)

        // Then
        errorsOrJson `should contain valid` json
        memoryLog.toString() `should be equal to` """
            [INFO] Reading file `$path`
            [DEBUG] File `$path` found
            [DEBUG] Parsing schema
            [DEBUG] schema parsed
        """.trimIndent()

        verify { objectMapper.readTree(file) }
    }

    @Test
    internal fun `It should be invalid when file has a bad format`() {
        // Given
        val path = getPath("pom.xml")
        val file = File(path)

        every { objectMapper.readTree(file) } throws RuntimeException()

        // When
        val errorsOrJson = fileReader.read(log, path)

        // Then
        errorsOrJson `should contain invalidNel` ReadFileError(path)

        memoryLog.toString() `should be equal to` """
            [INFO] Reading file `$path`
            [DEBUG] File `$path` found
            [DEBUG] Parsing schema
            [ERROR] cannot parse schema
        """.trimIndent()

        verify { objectMapper.readTree(file) }
    }

    @Test
    internal fun `It should be invalid when file not exists`() {
        // Given
        val path = getPath("not-existing-file")

        // When
        val errorsOrJson = fileReader.read(log, path)

        // Then
        errorsOrJson `should contain invalidNel` FileNotFoundError(path)

        memoryLog.toString() `should be equal to` """
            [INFO] Reading file `$path`
            [ERROR] File `$path` not found
        """.trimIndent()
    }

    private fun getPath(name: String) = Paths.get(name).toAbsolutePath().toString()
}
