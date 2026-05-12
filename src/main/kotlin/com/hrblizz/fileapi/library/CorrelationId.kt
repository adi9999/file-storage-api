package com.hrblizz.fileapi.library

object CorrelationId {
    private val holder = ThreadLocal<String?>()

    fun get(): String? = holder.get()
    fun set(id: String) { holder.set(id) }
    fun clear() { holder.remove() }
}
