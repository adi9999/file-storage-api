package com.hrblizz.fileapi.library.log

import com.hrblizz.fileapi.library.CorrelationId
import java.time.LocalDateTime

open class LogItem constructor(
    val message: String
) {
    val dateTime: LocalDateTime = LocalDateTime.now()

    var correlationId: String? = CorrelationId.get()
    var type: String? = null

    override fun toString(): String {
        return "[$dateTime] [$correlationId] $message"
    }
}
