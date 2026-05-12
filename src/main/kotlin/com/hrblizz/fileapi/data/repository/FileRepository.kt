package com.hrblizz.fileapi.data.repository

import com.hrblizz.fileapi.data.entities.FileEntity
import org.springframework.data.mongodb.repository.MongoRepository

interface FileRepository : MongoRepository<FileEntity, String>
