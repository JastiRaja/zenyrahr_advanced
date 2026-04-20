package com.zenyrahr.hrms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    @Value("${app.local-storage.base-dir:uploads}")
    private String baseDir;

    public String storeOrganizationLogo(MultipartFile file, String organizationName) throws IOException {
        String safeOrgName = sanitizeName(organizationName);
        String extension = resolveExtension(file.getOriginalFilename());

        Path folder = Paths.get(baseDir, "organization-logos", safeOrgName).toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String fileName = "logo_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path target = folder.resolve(fileName).normalize();

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ("/uploads/organization-logos/" + safeOrgName + "/" + fileName).replace("\\", "/");
    }

    public String storeProfilePicture(MultipartFile file, String employeeId) throws IOException {
        String safeEmployeeId = sanitizeName(employeeId);
        String extension = resolveExtension(file.getOriginalFilename());

        Path folder = Paths.get(baseDir, "profile-pictures", "user-" + safeEmployeeId).toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String fileName = "profile_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path target = folder.resolve(fileName).normalize();

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return ("/uploads/profile-pictures/user-" + safeEmployeeId + "/" + fileName).replace("\\", "/");
    }

    public String storeOrganizationPolicyPdf(MultipartFile file, String organizationName, String title) throws IOException {
        String safeOrgName = sanitizeName(organizationName);
        String safeTitle = sanitizeName(title);
        String extension = resolveExtension(file.getOriginalFilename(), ".pdf");

        Path folder = Paths.get(baseDir, "organization-policies", safeOrgName).toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String fileName = safeTitle + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path target = folder.resolve(fileName).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return ("/uploads/organization-policies/" + safeOrgName + "/" + fileName).replace("\\", "/");
    }

    public void deleteByRelativeUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank()) {
            return;
        }
        String normalized = relativeUrl.trim().replace("\\", "/");
        if (!normalized.startsWith("/uploads/")) {
            return;
        }
        String relativePath = normalized.substring("/uploads/".length());
        Path target = Paths.get(baseDir).toAbsolutePath().normalize().resolve(relativePath).normalize();
        Path basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        if (!target.startsWith(basePath)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best-effort cleanup; DB delete should still proceed
        }
    }

    private String sanitizeName(String name) {
        String safe = (name == null ? "" : name)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return safe.isBlank() ? "org" : safe;
    }

    private String resolveExtension(String originalName) {
        return resolveExtension(originalName, ".png");
    }

    private String resolveExtension(String originalName, String defaultExtension) {
        if (originalName == null || !originalName.contains(".")) {
            return defaultExtension;
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }
}
