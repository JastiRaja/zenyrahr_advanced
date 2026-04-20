package com.zenyrahr.hrms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.bucket-name}")
    private String bucketName;

    @Value("${aws.presigned-url-duration}")
    private long presignedUrlDuration;

    @Value("${aws.expenses-folder}")
    private String expensesFolder;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadProfilePicture(MultipartFile file, String userId) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = String.format("profile-pictures/user_%s/profile_%d%s",
                userId, System.currentTimeMillis(), fileExtension);
        uploadFile(file, key); // Upload the file to S3
        return key;
    }

    public String uploadOrganizationLogo(MultipartFile file, String organizationName) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String safeOrgName = organizationName == null ? "org" : organizationName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (safeOrgName.isBlank()) {
            safeOrgName = "org";
        }
        String key = String.format("organization-logos/%s/logo_%d%s",
                safeOrgName, System.currentTimeMillis(), fileExtension);
        uploadFile(file, key);
        return key;
    }

    public String uploadCategoryDocuments(List<MultipartFile> files, String userId, String category) throws IOException {
        validateCategory(category);

        // Check for empty file list or all files empty
        if (files == null || files.isEmpty() || files.stream().allMatch(f -> f.isEmpty())) {
            throw new IllegalArgumentException("No valid files provided for upload.");
        }

        ByteArrayOutputStream zipStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipStream)) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    addToZip(zipOut, file);
                }
            }
        }

        String folder = category.equals("expenses") ? expensesFolder : category;
        String zipKey = String.format("documents/%s/user_%s/docs_%d.zip",
                folder, userId, System.currentTimeMillis());
        uploadZip(zipStream.toByteArray(), zipKey);
        return zipKey;
    }

    public String updateCategoryDocument(MultipartFile file, String userId, String category) throws IOException {
        validateCategory(category);
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String key = String.format("documents/%s/user_%s/update_%d%s",
                category, userId, System.currentTimeMillis(), fileExtension);

        uploadFile(file, key);
        return key;
    }

    public String deleteCategoryDocument(String userId, String category) {
        validateCategory(category);
        String prefix = String.format("documents/%s/user_%s/", category, userId);

        List<S3Object> objects = listObjects(prefix);
        if (objects.isEmpty()) {
            return "No documents found to delete";
        }

        objects.forEach(obj -> {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(obj.key())
                    .build());
        });

        return String.format("Deleted %d documents in category '%s'", objects.size(), category);
    }

    public List<String> listFilesInFolder(String prefix) {
        return listObjects(prefix).stream()
                .map(S3Object::key)
                .map(key -> key.replaceFirst(prefix, ""))
                .toList();
    }

    public String getFileUrl(String key) {
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .toString();
    }

    public String getPresignedUrl(String key) {
        // Determine content type based on file extension
        String contentType = determineContentType(key);
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentType(contentType)
                .responseContentDisposition("attachment")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlDuration))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String determineContentType(String key) {
        String extension = key.substring(key.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    private List<S3Object> listObjects(String prefix) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        List<S3Object> objects = new ArrayList<>();
        ListObjectsV2Response listRes;
        do {
            listRes = s3Client.listObjectsV2(listReq);
            objects.addAll(listRes.contents());
            listReq = listReq.toBuilder().continuationToken(listRes.nextContinuationToken()).build();
        } while (listRes.isTruncated());

        return objects;
    }

    private String uploadFile(MultipartFile file, String key) throws IOException {
        try (InputStream fileStream = file.getInputStream()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentDisposition("attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(fileStream, file.getSize()));

            return getFileUrl(key);
        }
    }

    private String uploadZip(byte[] zipBytes, String key) {
        String filename = key.substring(key.lastIndexOf('/') + 1);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/zip")
                .contentDisposition("attachment; filename=\"" + filename + "\"")
                .build();

        try {
            s3Client.putObject(putRequest, RequestBody.fromBytes(zipBytes));
            log.info("Uploaded ZIP to S3 with key {}", key);
        } catch (Exception e) {
            log.error("Failed to upload ZIP to S3 with key {}", key, e);
        }
        return getFileUrl(key);
    }

    private void addToZip(ZipOutputStream zipOut, MultipartFile file) throws IOException {
        ZipEntry zipEntry = new ZipEntry(file.getOriginalFilename());
        zipOut.putNextEntry(zipEntry);
        zipOut.write(file.getBytes());
        zipOut.closeEntry();
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    private void validateCategory(String category) {
        if (!List.of("travel_requests", expensesFolder, "leave_requests", "leaveRequest").contains(category)) {
            throw new IllegalArgumentException("Invalid category: " + category);
        }
    }
}