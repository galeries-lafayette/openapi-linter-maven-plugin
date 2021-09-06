package com.ggl.openapi.linter.plugin

fun <T : Any> T.resetField(fieldName: String, value: Any) =
    also {
        val field = it::class.java.getDeclaredField(fieldName)

        field.isAccessible = true
        field.set(it, value)
    }
