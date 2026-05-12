# Summary

## What the app does

This is a file storage REST API. Clients upload files via multipart form and receive a UUID token.
That token is used to download the file, retrieve its metadata, or delete it. File contents are
stored on disk; metadata (filename, size, content type, upload time, custom meta fields) is
persisted in MongoDB. All endpoints require HTTP Basic Auth (admin / hunter2).

## How to start

1. Start MongoDB: `docker-compose up -d`
2. Run the app: `./do.sh start` (or `mvn spring-boot:run`)
3. API docs: http://localhost:6011/docs

## Comments

- **Streaming over buffering**: upload uses `Files.copy(inputStream, path)` and download returns
  `StreamingResponseBody` so large files never materialize fully in heap.
- **Delete ordering**: MongoDB record is deleted first, then the file on disk. The reverse would
  leave a live token pointing to a missing file; an orphaned disk file is the safer failure mode.
- **Atomicity on upload failure**: if `fileRepository.save` throws after the file has been written,
  `fileStorageService.delete` is called in the catch block so no orphaned files accumulate.
- **Exception package at root**: `BadRequestException` and `NotFoundException` live under
  `exception/` rather than `controller/exception/`.
- **FileStorageService abstraction**: an interface separates the "where files live" concern from
  `FileService`, making it straightforward to swap in an S3 or GCS implementation without touching
  business logic.
- **CorrelationId lifecycle**: `ThreadLocal` is set in `preHandle` and cleared in `afterCompletion`;
  Spring's `HandlerInterceptor` guarantees `afterCompletion` is called even when the handler throws,
  so the thread is always returned to the pool clean. `correlationId` is assigned in `LogItem`'s
  property initializer via `CorrelationId.get()`, so every log item automatically carries it without
  manual assignment at the call site.
- **4xx exception logging**: `MaxUploadSizeExceededException`, and
  `AccessDeniedException` are all logged at `warning` level using `ExceptionLogItem` (captures
  stack trace + correlationId). 5xx errors are logged at `crit`. Routine 404/405/415 responses
  are intentionally silent to avoid log noise.
- **Unit tests**: four tests in `AppTest` cover the core service behavior — token returned on
  upload, metas filtered to found tokens only, 404 thrown on download of missing token, and 404
  thrown on delete of missing token.
- **Profile-based config**: `application-local.properties` holds dev-only MongoDB.

## Which part took the most time and why

Handling the multipart upload cleanly took the most thought — specifically parsing the `meta` JSON
field from a string part, dealing with the naming collision between Spring's `ResponseEntity` and
the project's custom `ResponseEntity`, and making sure the download endpoint streamed file content
with the correct headers rather than going through the JSON envelope.

## What I learned

- **Streaming vs buffering trade-offs at the I/O boundary**: switching from `content.bytes` /
  `readAllBytes` to `Files.copy` / `StreamingResponseBody` removes heap pressure for large files
  without changing the API contract. The extra Tika re-read (store then detect) is negligible
  compared to loading a 500 MB file into memory.
- **Spring's `HandlerInterceptor` contract as a safety guarantee**: `afterCompletion` is called
  even when the handler throws, which makes it the correct place to clear `ThreadLocal` state —
  relying on this contract is safer than wrapping every handler in try-finally.

## TODOs

- Implement expiry: a scheduled job to delete files and metadata whose `expireTime` has passed
- Store files in object storage (S3/GCS) instead of local disk and implement another FileStorageService
- Validate declared content type against actual file bytes using Apache Tika to reject uploads
  where the MIME type doesn't match the content (e.g. a script disguised as an image)
