package com.hrblizz.fileapi.data.entities

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "files")
class FileEntity {
    @Id
    lateinit var token: String
    lateinit var filename: String
    var size: Long = 0
    lateinit var contentType: String
    lateinit var createTime: LocalDateTime
    lateinit var meta: Map<String, Any>
    lateinit var source: String
    var expireTime: LocalDateTime? = null
}
