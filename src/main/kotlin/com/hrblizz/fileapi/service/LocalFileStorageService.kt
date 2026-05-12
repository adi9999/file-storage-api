package com.hrblizz.fileapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

@Service
class LocalFileStorageService(
    @Value("\${file.upload-dir}") private val uploadDir: String
) : FileStorageService {

    override fun store(inputStream: InputStream, token: String) {
        val dir = Paths.get(uploadDir)
        if (!Files.exists(dir)) Files.createDirectories(dir)
        Files.copy(inputStream, dir.resolve(token))
    }

    override fun retrieve(token: String): InputStream {
        return Files.newInputStream(Paths.get(uploadDir, token))
    }

    override fun delete(token: String) {
        Files.deleteIfExists(Paths.get(uploadDir, token))
    }
}
