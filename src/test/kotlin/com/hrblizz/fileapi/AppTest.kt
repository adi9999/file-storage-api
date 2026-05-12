package com.hrblizz.fileapi

import com.hrblizz.fileapi.data.entities.FileEntity
import com.hrblizz.fileapi.data.repository.FileRepository
import com.hrblizz.fileapi.exception.NotFoundException
import com.hrblizz.fileapi.service.FileService
import com.hrblizz.fileapi.service.LocalFileStorageService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.Optional

class AppTest {

    @Test
    fun uploadFileReturnsToken(@TempDir tempDir: Path) {
        val repo = mock(FileRepository::class.java)
        `when`(repo.save(any(FileEntity::class.java))).thenAnswer { it.arguments[0] }

        val service = FileService(repo, LocalFileStorageService(tempDir.toString()))
        val file = MockMultipartFile("content", "test.txt", "text/plain", "hello".toByteArray())

        val token = service.uploadFile("test.txt", "text/plain", emptyMap(), "test", null, file)

        assertNotNull(token)
        assertEquals(36, token.length)
    }

    @Test
    fun getFileMetasFiltersToFoundTokens(@TempDir tempDir: Path) {
        val repo = mock(FileRepository::class.java)
        val entity = FileEntity().also {
            it.token = "abc"
            it.filename = "file.pdf"
            it.size = 100
            it.contentType = "application/pdf"
            it.createTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
            it.meta = mapOf("key" to "value")
            it.source = "test"
        }
        `when`(repo.findAllById(listOf("abc", "missing"))).thenReturn(listOf(entity))

        val service = FileService(repo, LocalFileStorageService(tempDir.toString()))
        val result = service.getFileMetas(listOf("abc", "missing"))

        assertEquals(1, result.size)
        assertNotNull(result["abc"])
        assertEquals("file.pdf", result["abc"]?.filename)
    }

    @Test
    fun getFileContentThrowsNotFoundForMissingToken(@TempDir tempDir: Path) {
        val repo = mock(FileRepository::class.java)
        `when`(repo.findById("missing")).thenReturn(Optional.empty())

        val service = FileService(repo, LocalFileStorageService(tempDir.toString()))

        assertThrows(NotFoundException::class.java) {
            service.getFileContent("missing")
        }
    }

    @Test
    fun deleteFileThrowsNotFoundForMissingToken(@TempDir tempDir: Path) {
        val repo = mock(FileRepository::class.java)
        `when`(repo.findById("missing")).thenReturn(Optional.empty())

        val service = FileService(repo, LocalFileStorageService(tempDir.toString()))

        assertThrows(NotFoundException::class.java) {
            service.deleteFile("missing")
        }
    }
}
