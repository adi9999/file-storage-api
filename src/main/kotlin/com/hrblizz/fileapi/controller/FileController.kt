package com.hrblizz.fileapi.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.hrblizz.fileapi.exception.BadRequestException
import com.hrblizz.fileapi.rest.FileMeta
import com.hrblizz.fileapi.rest.FileMetasRequest
import com.hrblizz.fileapi.rest.ResponseEntity
import com.hrblizz.fileapi.service.FileService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.time.LocalDateTime
import org.springframework.http.ResponseEntity as SpringResponseEntity

@RestController
class FileController(
    private val fileService: FileService,
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/files", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart("name") name: String,
        @RequestPart("contentType") contentType: String,
        @RequestPart("meta") metaJson: String,
        @RequestPart("source") source: String,
        @RequestPart(value = "expireTime", required = false) expireTime: String?,
        @RequestPart("content") content: MultipartFile
    ): SpringResponseEntity<ResponseEntity<Map<String, String>>> {
        if (name.isBlank()) {
            throw BadRequestException("name cannot be blank")
        }
        if (contentType.isBlank()) {
            throw BadRequestException("contentType cannot be blank")
        }
        if (content.isEmpty) {
            throw BadRequestException("content cannot be empty")
        }
        if (source.isBlank()) {
            throw BadRequestException("source cannot be blank")
        }
        val meta: Map<String, Any> = try {
            objectMapper.readValue(metaJson, object : TypeReference<Map<String, Any>>() {})
        } catch (e: Exception) {
            throw BadRequestException("Invalid meta JSON: ${e.message}")
        }
        val expireDateTime = expireTime?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: Exception) {
                throw BadRequestException("Invalid expireTime format '$it': ${e.message}")
            }
        }
        val token = fileService.uploadFile(name, contentType, meta, source, expireDateTime, content)
        val body = ResponseEntity(mapOf("token" to token), HttpStatus.CREATED.value())
        return SpringResponseEntity(body, HttpStatus.CREATED)
    }

    @PostMapping("/files/metas")
    fun getFileMetas(@RequestBody request: FileMetasRequest): SpringResponseEntity<ResponseEntity<Map<String, Map<String, FileMeta>>>?> {
        val files = fileService.getFileMetas(request.tokens)
        val body = ResponseEntity(mapOf("files" to files), HttpStatus.OK.value())
        return SpringResponseEntity(body, HttpStatus.OK)
    }

    @GetMapping("/file/{token}")
    fun downloadFile(@PathVariable token: String): SpringResponseEntity<StreamingResponseBody> {
        val data = fileService.getFileContent(token)
        val headers = HttpHeaders().apply {
            set("X-Filename", data.filename)
            set("X-Filesize", data.size.toString())
            set("X-CreateTime", data.createTime)
            set("Content-Disposition", "attachment; filename=\"${data.filename}\"")
            contentType = MediaType.parseMediaType(data.contentType)
        }
        val body = StreamingResponseBody { outputStream ->
            data.inputStream.use { it.copyTo(outputStream) }
        }
        return SpringResponseEntity(body, headers, HttpStatus.OK)
    }

    @DeleteMapping("/file/{token}")
    fun deleteFile(@PathVariable token: String): SpringResponseEntity<Unit> {
        fileService.deleteFile(token)
        return SpringResponseEntity(HttpStatus.OK)
    }
}
