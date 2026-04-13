package com.uwazy.api.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload/image")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMINISTRATEUR')")
    public ResponseEntity<FileResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String fileName = fileStorageService.storeFile(file, "images");
        String fileDownloadUri = generateDownloadUri(fileName);

        return ResponseEntity.ok(new FileResponse(
                "Image uploaded successfully", 
                fileDownloadUri, 
                fileName, 
                file.getOriginalFilename()
        ));
    }

    @PostMapping("/upload/video")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMINISTRATEUR')")
    public ResponseEntity<FileResponse> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Validate video file type
        String contentType = file.getContentType();
        if (!isValidVideoType(contentType)) {
            throw new RuntimeException("Invalid video file type. Allowed: mp4, webm, avi, mov");
        }

        String fileName = fileStorageService.storeFile(file, "videos");
        String fileDownloadUri = generateDownloadUri(fileName);

        return ResponseEntity.ok(new FileResponse(
                "Video uploaded successfully", 
                fileDownloadUri, 
                fileName, 
                file.getOriginalFilename()
        ));
    }

    @PostMapping("/upload/document")
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMINISTRATEUR')")
    public ResponseEntity<FileResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Validate document file type
        String contentType = file.getContentType();
        if (!isValidDocumentType(contentType, file.getOriginalFilename())) {
            throw new RuntimeException("Invalid document file type. Allowed: pdf, doc, docx, xlsx, pptx");
        }

        String fileName = fileStorageService.storeFile(file, "documents");
        String fileDownloadUri = generateDownloadUri(fileName);

        return ResponseEntity.ok(new FileResponse(
                "Document uploaded successfully", 
                fileDownloadUri, 
                fileName, 
                file.getOriginalFilename()
        ));
    }

    @GetMapping("/download/{*fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // {*fileName} captures the leading slash — strip it
        String cleanFileName = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        Resource resource = fileStorageService.loadFileAsResource(cleanFileName);
        
        String contentType = null;
        String lowerFileName = resource.getFilename() != null ? resource.getFilename().toLowerCase() : "";
        try {
            java.io.File file = resource.getFile();
            contentType = java.nio.file.Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = request.getServletContext().getMimeType(file.getAbsolutePath());
            }
        } catch (IOException ex) {
            // Fallback to manual mapping
        }
        if (contentType == null) {
            if (lowerFileName.endsWith(".pdf")) contentType = "application/pdf";
            else if (lowerFileName.endsWith(".mp4")) contentType = "video/mp4";
            else if (lowerFileName.endsWith(".webm")) contentType = "video/webm";
            else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lowerFileName.endsWith(".png")) contentType = "image/png";
            else if (lowerFileName.endsWith(".gif")) contentType = "image/gif";
            else contentType = "application/octet-stream";
        }

        MediaType mediaType = MediaType.parseMediaType(contentType);

        // Handle Range requests for video streaming
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null && mediaType.getType().equals("video")) {
            try {
                long fileLength = resource.contentLength();
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                if (!ranges.isEmpty()) {
                    HttpRange range = ranges.get(0);
                    long start = range.getRangeStart(fileLength);
                    long end = range.getRangeEnd(fileLength);
                    long rangeLength = end - start + 1;

                    InputStream inputStream = resource.getInputStream();
                    inputStream.skip(start);
                    byte[] bytes = inputStream.readNBytes((int) rangeLength);
                    inputStream.close();

                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .contentType(mediaType)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(rangeLength))
                            .body(new org.springframework.core.io.ByteArrayResource(bytes));
                }
            } catch (Exception ex) {
                // Fall through to full response
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    private String generateDownloadUri(String filePath) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(filePath)
                .toUriString();
    }

    private boolean isValidVideoType(String contentType) {
        if (contentType == null) return false;
        return contentType.contains("video/mp4") ||
               contentType.contains("video/webm") ||
               contentType.contains("video/avi") ||
               contentType.contains("video/quicktime");
    }

    private boolean isValidDocumentType(String contentType, String fileName) {
        if (contentType == null || fileName == null) return false;
        
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".pdf") ||
               lowerFileName.endsWith(".doc") ||
               lowerFileName.endsWith(".docx") ||
               lowerFileName.endsWith(".xlsx") ||
               lowerFileName.endsWith(".pptx") ||
               contentType.contains("pdf") ||
               contentType.contains("word") ||
               contentType.contains("sheet") ||
               contentType.contains("presentation");
    }

    public static class FileResponse {
        private String message;
        private String url;
        private String path;
        private String originalFileName;

        public FileResponse() {}

        public FileResponse(String message, String url, String path, String originalFileName) {
            this.message = message;
            this.url = url;
            this.path = path;
            this.originalFileName = originalFileName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }
    }
}
