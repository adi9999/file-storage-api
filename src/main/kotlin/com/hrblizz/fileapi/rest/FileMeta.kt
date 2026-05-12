package com.hrblizz.fileapi.rest

data class FileMeta(
    val token: String,
    val filename: String,
    val size: Long,
    val contentType: String,
    val createTime: String,
    val meta: Map<String, Any>
)
