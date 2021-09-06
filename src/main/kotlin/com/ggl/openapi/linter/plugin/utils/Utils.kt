package com.ggl.openapi.linter.plugin.utils

import arrow.core.ValidatedNel
import com.ggl.arrow.addon.ValidatedExtensions.getOrElseThrow
import com.ggl.openapi.linter.plugin.model.OpenAPILinterError
import org.apache.maven.plugin.MojoFailureException

fun <T> ValidatedNel<OpenAPILinterError, T>.handleErrors() =
    this
        .mapLeft { it.map(OpenAPILinterError::message) }
        .mapLeft { it.all.joinToString("\n") }
        .getOrElseThrow(::MojoFailureException)
