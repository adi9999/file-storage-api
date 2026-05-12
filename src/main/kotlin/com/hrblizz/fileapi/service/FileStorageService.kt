package com.hrblizz.fileapi.service

import java.io.InputStream

interface FileStorageService {
    fun store(inputStream: InputStream, token: String)
    fun retrieve(token: String): InputStream
    fun delete(token: String)
}
