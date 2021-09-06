package com.ggl.openapi.linter.plugin

import org.codehaus.plexus.logging.AbstractLogger

class MemoryLog(threshold: Int = 0) : AbstractLogger(threshold, "memory") {
    private val sb = StringBuilder()

    private companion object {
        private val TAGS = arrayOf("DEBUG", "INFO", "WARNING", "ERROR", "FATAL ERROR")
    }

    override fun debug(message: String, throwable: Throwable?) {
        if (isDebugEnabled) {
            log(0, message, throwable)
        }
    }

    override fun info(message: String, throwable: Throwable?) {
        if (isInfoEnabled) {
            log(1, message, throwable)
        }
    }

    override fun warn(message: String, throwable: Throwable?) {
        if (isWarnEnabled) {
            log(2, message, throwable)
        }
    }

    override fun error(message: String, throwable: Throwable?) {
        if (isErrorEnabled) {
            log(3, message, throwable)
        }
    }

    override fun fatalError(message: String, throwable: Throwable?) {
        if (isFatalErrorEnabled) {
            log(4, message, throwable)
        }
    }

    override fun getChildLogger(message: String) = this

    private fun log(level: Int, message: String, throwable: Throwable?) {
        if (sb.isNotEmpty()) {
            sb.append("\n")
        }
        sb.append("[${TAGS[level]}]")
        if (message.isNotBlank()) {
            sb.append(" $message")
        }
        throwable?.printStackTrace(System.out)
    }

    fun clear() = sb.clear()

    override fun toString() = sb.toString()
}
