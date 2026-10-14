package com.panduranga.S3DemoApplication.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

/**
 * Service class that wraps the three core S3 operations:
 *   - uploadFile   : puts an object into the configured bucket
 *   - downloadFile : retrieves an object as a byte array
 *   - deleteFile   : removes an object from the bucket
 *
 * The bucket name is injected from application.properties so it never
 * needs to be changed here.
 */
@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // Constructor injection — preferred over field injection
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // -------------------------------------------------------------------------
    // Upload
    // -------------------------------------------------------------------------

    /**
     * Uploads a multipart file to S3.
     *
     * @param file     the file sent by the HTTP client
     * @param fileName the S3 object key (name) to store the file under
     * @throws IOException if the file bytes cannot be read
     */
    public void uploadFile(MultipartFile file, String fileName) throws IOException {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        // RequestBody.fromBytes() loads the entire file into memory — fine for
        // typical document / image uploads. For very large files (>100 MB) a
        // streaming approach with RequestBody.fromInputStream() would be better.
        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    /**
     * Downloads an object from S3 and returns its raw bytes.
     *
     * @param fileName the S3 object key to retrieve
     * @return the file content as a byte array
     * @throws IOException if the response stream cannot be read
     */
    public byte[] downloadFile(String fileName) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        // ResponseInputStream implements InputStream, so we read it fully
        // and close it in the try-with-resources block.
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest)) {
            return response.readAllBytes();
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /**
     * Deletes an object from S3.
     *
     * @param fileName the S3 object key to delete
     */
    public void deleteFile(String fileName) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.deleteObject(deleteRequest);
    }
}
