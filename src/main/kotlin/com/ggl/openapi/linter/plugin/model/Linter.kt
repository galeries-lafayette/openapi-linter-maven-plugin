package com.ggl.openapi.linter.plugin.model

import arrow.core.Option
import com.ggl.arrow.addon.MapExtensions.getOption

enum class Linter {
    ZALLY;

    companion object {
        val values = values().toSet()
        private val valuesMap = values.associateBy { it.name.toUpperCase() }

        fun findByName(name: String): Option<Linter> = valuesMap.getOption(name.toUpperCase())
    }
}
