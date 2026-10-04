package ai.example.open.controller;

import ai.example.open.dto.DocumentUploadResponse;
import ai.example.open.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Primary endpoint to accept a MultipartFile, extract its text,
     * split it into chunks, and store the resulting embeddings in PgVector.
     * Accessible via /api/documents/upload, /documents/upload, or /upload.
     */
    @PostMapping(
            value = {"/api/documents/upload", "/documents/upload", "/upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "Please select a non-empty file to upload."
            ));
        }

        try {
            log.info("Received document upload request for: '{}' ({} bytes)",
                    file.getOriginalFilename(), file.getSize());
            DocumentUploadResponse response = documentService.processAndStore(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Document validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to process and store document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to process and store document: " + e.getMessage()
            ));
        }
    }

    /**
     * Batch upload endpoint to accept multiple files at once.
     */
    @PostMapping(
            value = {"/api/documents/upload/batch", "/upload/batch"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> uploadMultipleDocuments(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "No files provided for batch upload."
            ));
        }

        try {
            List<DocumentUploadResponse> responses = documentService.processAndStoreMultiple(files);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Batch document upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Batch upload failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Inspect raw extracted documents from a MultipartFile without storing to vector store.
     */
    @PostMapping(
            value = "/api/documents/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> extractDocument(@RequestParam("file") MultipartFile file) {
        try {
            List<Document> extracted = documentService.extractDocuments(file);
            return ResponseEntity.ok(extracted);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Inspect chunked documents from a MultipartFile without storing to vector store.
     */
    @PostMapping(
            value = "/api/documents/chunk",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> chunkDocument(@RequestParam("file") MultipartFile file) {
        try {
            List<Document> extracted = documentService.extractDocuments(file);
            List<Document> chunks = documentService.splitDocuments(extracted);
            return ResponseEntity.ok(chunks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Status endpoint for the document ingestion service.
     */
    @GetMapping("/api/documents/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "DocumentIngestionService");
        status.put("status", "ACTIVE");
        status.put("vectorStore", "PgVector");
        status.put("supportedTypes", List.of("PDF (.pdf)", "Plain Text (.txt)", "Markdown (.md)", "JSON (.json)", "CSV (.csv)"));
        return ResponseEntity.ok(status);
    }
}
