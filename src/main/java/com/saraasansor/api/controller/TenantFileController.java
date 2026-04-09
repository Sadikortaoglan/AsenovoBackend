package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.service.FileStorageService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/files")
public class TenantFileController {

    private static final Pattern TENANT_BRANDING_KEY_PATTERN = Pattern.compile("^tenants/(\\d+)/branding/[^/]+$");

    private final FileStorageService fileStorageService;

    public TenantFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/**")
    public ResponseEntity<?> getTenantBrandingFile(HttpServletRequest request) {
        String storageKey = extractStorageKey(request);
        if (!StringUtils.hasText(storageKey)) {
            return ResponseEntity.status(404).body(ApiResponse.error("File not found"));
        }

        Matcher matcher = TENANT_BRANDING_KEY_PATTERN.matcher(storageKey);
        if (!matcher.matches()) {
            return ResponseEntity.status(404).body(ApiResponse.error("File not found"));
        }

        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.getId() == null || !tenant.getId().toString().equals(matcher.group(1))) {
            return ResponseEntity.status(404).body(ApiResponse.error("File not found"));
        }

        Path filePath;
        try {
            filePath = fileStorageService.resolveStoredPath(storageKey);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(ApiResponse.error("File not found"));
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return ResponseEntity.status(404).body(ApiResponse.error("File not found"));
        }

        Resource resource = new FileSystemResource(filePath.toFile());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .contentType(resolveMediaType(filePath))
                .body(resource);
    }

    private String extractStorageKey(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String pathWithinApp = request.getRequestURI();
        if (StringUtils.hasText(contextPath) && pathWithinApp.startsWith(contextPath)) {
            pathWithinApp = pathWithinApp.substring(contextPath.length());
        }

        final String filesPrefix = "/files/";
        int markerIndex = pathWithinApp.indexOf(filesPrefix);
        if (markerIndex < 0) {
            return null;
        }

        String storageKey = pathWithinApp.substring(markerIndex + filesPrefix.length());
        return storageKey.trim();
    }

    private MediaType resolveMediaType(Path filePath) {
        try {
            String detected = Files.probeContentType(filePath);
            if (StringUtils.hasText(detected)) {
                return MediaType.parseMediaType(detected);
            }
        } catch (Exception ignored) {
        }

        String filename = filePath.getFileName() != null
                ? filePath.getFileName().toString().toLowerCase(Locale.ROOT)
                : "";
        if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (filename.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
