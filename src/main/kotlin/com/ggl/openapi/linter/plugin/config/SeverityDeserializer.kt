package com.ggl.openapi.linter.plugin.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.ggl.openapi.linter.plugin.model.Severity

class SeverityDeserializer : JsonDeserializer<Severity?>() {

    override fun deserialize(jsonParser: JsonParser, context: DeserializationContext) =
        Severity.findByName(jsonParser.text).orNull()
}
