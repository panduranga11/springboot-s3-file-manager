package com.panduranga.S3DemoApplication.controller;

import com.panduranga.S3DemoApplication.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;

/**
 * REST controller exposing three S3 file management endpoints:
 *
 *   POST   /api/s3/upload/{fileName}    — upload a file
 *   GET    /api/s3/download/{fileName}  — download a file
 *   DELETE /api/s3/delete/{fileName}    — delete a file
 *
 * All routes are prefixed with /api/s3 to keep the API organised.
 * Error handling is done inline here to keep the code simple and readable
 * for beginners; a @ControllerAdvice could centralise this in larger projects.
 */
@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    // -------------------------------------------------------------------------
    // Upload  —  POST /api/s3/upload/{fileName}
    // -------------------------------------------------------------------------

    /**
     * Accepts a multipart/form-data request with a "file" part and uploads it
     * to S3 under the given fileName (used as the S3 object key).
     *
     * curl example:
     *   curl -X POST http://localhost:8080/api/s3/upload/myfile.pdf \
     *        -F "file=@/path/to/myfile.pdf"
     */
    @PostMapping("/upload/{fileName}")
    public ResponseEntity<String> uploadFile(
            @PathVariable String fileName,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body("Upload failed: the provided file is empty.");
        }

        try {
            s3Service.uploadFile(file, fileName);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("File uploaded successfully: " + fileName);
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: could not read file bytes — " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Download  —  GET /api/s3/download/{fileName}
    // -------------------------------------------------------------------------

    /**
     * Downloads the named file from S3 and streams it back to the client as an
     * octet-stream (binary download).  The Content-Disposition header tells the
     * browser to save the file rather than display it.
     *
     * curl example:
     *   curl -O http://localhost:8080/api/s3/download/myfile.pdf
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            byte[] content = s3Service.downloadFile(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // "attachment" triggers a browser download dialog
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);

        } catch (NoSuchKeyException e) {
            // S3 returned a 404 — the object does not exist
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Delete  —  DELETE /api/s3/delete/{fileName}
    // -------------------------------------------------------------------------

    /**
     * Deletes the named object from S3.
     *
     * NOTE: The AWS S3 DeleteObject API returns 204 No Content even if the key
     * did not exist — this is by design (idempotent operation). The response
     * here mirrors that behaviour.
     *
     * curl example:
     *   curl -X DELETE http://localhost:8080/api/s3/delete/myfile.pdf
     */
    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        try {
            s3Service.deleteFile(fileName);
            return ResponseEntity
                    .ok("File deleted successfully: " + fileName);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Delete failed: " + e.getMessage());
        }
    }
}
