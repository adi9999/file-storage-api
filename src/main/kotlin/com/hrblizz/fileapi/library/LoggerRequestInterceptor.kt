package com.hrblizz.fileapi.library

import com.hrblizz.fileapi.library.log.Logger
import com.hrblizz.fileapi.library.log.TraceLogItem
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class LoggerRequestInterceptor(
    private val logger: Logger
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute("start_time", System.currentTimeMillis())
        CorrelationId.set(UUID.randomUUID().toString())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val traceItem = TraceLogItem(
            request.method,
            request.requestURL.toString(),
            response.status.toLong(),
            System.currentTimeMillis() - (request.getAttribute("start_time") as Long)
        )
        this.logger.info(traceItem)
        CorrelationId.clear()
    }
}
