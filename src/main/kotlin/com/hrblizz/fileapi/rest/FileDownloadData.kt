package com.hrblizz.fileapi.rest

import java.io.InputStream

data class FileDownloadData(
    val filename: String,
    val size: Long,
    val contentType: String,
    val createTime: String,
    val inputStream: InputStream
)
