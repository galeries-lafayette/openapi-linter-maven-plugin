package com.ggl.openapi.linter.plugin

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.ggl.arrow.addon.ValidatedExtensions.flatMap
import com.ggl.arrow.addon.ValidatedExtensions.peek
import com.ggl.openapi.linter.plugin.config.SeverityDeserializer
import com.ggl.openapi.linter.plugin.display.Printer
import com.ggl.openapi.linter.plugin.model.Severity
import com.ggl.openapi.linter.plugin.reader.FileReader
import com.ggl.openapi.linter.plugin.resolver.Resolver
import com.ggl.openapi.linter.plugin.service.ValidationService
import com.ggl.openapi.linter.plugin.service.ZallyValidationService
import com.ggl.openapi.linter.plugin.utils.handleErrors
import com.ggl.openapi.linter.plugin.validator.InputValidator
import okhttp3.OkHttpClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

@Mojo(name = "validate", defaultPhase = VALIDATE)
class OpenAPILinterMojo : AbstractMojo() {

    private val inputValidator = InputValidator()
    private val fileReader = FileReader(YAMLMapper())
    private val printer = Printer()
    private val resolver = Resolver()

    private val validationService = ValidationService(
        setOf(
            ZallyValidationService(
                client = OkHttpClient(),
                objectMapper = ObjectMapper()
                    .setPropertyNamingStrategy(SNAKE_CASE)
                    .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                    .registerModules(
                        KotlinModule(),
                        SimpleModule()
                            .addDeserializer(Severity::class.java, SeverityDeserializer())
                    )
            )
        )
    )

    @Parameter(required = true)
    private lateinit var schema: String

    @Parameter(required = true)
    private lateinit var server: String

    @Parameter(required = true)
    private lateinit var linter: String

    @Parameter
    private var displayViolations = false

    @Parameter
    private var thresholds = emptyMap<String, String>()

    @Parameter(property = "skipSchemaValidation")
    private var skipSchemaValidation = false

    override fun execute() {
        if (!skipSchemaValidation) {
            inputValidator
                .validate(log, linter, thresholds)
                .peek { log.info("Processing with thresholds ${thresholds.toList().joinToString()}") }
                .flatMap { input ->
                    fileReader.read(log, schema)
                        .flatMap { validationService.validate(log, input.linter, server, it) }
                        .peek { printer.display(log, it) }
                        .flatMap { resolver.resolve(log, it, input.thresholds, displayViolations) }
                }
                .handleErrors()
        }
    }
}
