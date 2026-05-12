package com.hrblizz.fileapi.service

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.exception.NotFoundException
import com.hrblizz.fileapi.rest.FileDownloadData
import com.hrblizz.fileapi.rest.FileMeta
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class FileService(
    private val fileRepository: FileRepository,
    private val fileStorageService: FileStorageService
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    }

    fun uploadFile(
        name: String,
        contentType: String,
        meta: Map<String, Any>,
        source: String,
        expireTime: LocalDateTime?,
        content: MultipartFile
    ): String {
        val token = UUID.randomUUID().toString()
        fileStorageService.store(content.inputStream, token)

        val entity = FileEntity().also {
            it.token = token
            it.filename = name
            it.size = content.size
            it.contentType = contentType
            it.createTime = LocalDateTime.now()
            it.meta = meta
            it.source = source
            it.expireTime = expireTime
        }
        try {
            fileRepository.save(entity)
        } catch (e: Exception) {
            fileStorageService.delete(token)
            throw e
        }
        return token
    }

    fun getFileMetas(tokens: List<String>): Map<String, FileMeta> {
        return fileRepository.findAllById(tokens).associate { entity ->
            entity.token to FileMeta(
                token = entity.token,
                filename = entity.filename,
                size = entity.size,
                contentType = entity.contentType,
                createTime = entity.createTime.format(FORMATTER),
                meta = entity.meta
            )
        }
    }

    fun getFileContent(token: String): FileDownloadData {
        val entity = fileRepository.findById(token)
            .orElseThrow { NotFoundException("File not found") }
        return FileDownloadData(
            filename = entity.filename,
            size = entity.size,
            contentType = entity.contentType,
            createTime = entity.createTime.format(FORMATTER),
            inputStream = fileStorageService.retrieve(token)
        )
    }

    fun deleteFile(token: String) {
        val entity = fileRepository.findById(token)
            .orElseThrow { NotFoundException("File not found") }
        fileRepository.delete(entity)
        fileStorageService.delete(entity.token)
    }
}
