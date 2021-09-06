package com.ggl.openapi.linter.plugin.model

import arrow.core.Option
import com.ggl.arrow.addon.MapExtensions.getOption

enum class Severity(val criticity: Int) {
    MUST(1), SHOULD(2), MAY(3), HINT(4);

    companion object {
        val values = values().toList().sortedBy { it.criticity }.toSet()
        private val valuesMap = values.associateBy { it.name.toUpperCase() }

        fun findByName(name: String): Option<Severity> = valuesMap.getOption(name.toUpperCase())
    }
}
