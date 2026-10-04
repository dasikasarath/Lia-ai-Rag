package ai.example.open.service;

import ai.example.open.dto.DocumentUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    public DocumentServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = TokenTextSplitter.builder().build();
    }

    @Override
    public DocumentUploadResponse processAndStore(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        log.info("Processing file: '{}' (size: {} bytes, type: {})", originalFilename, file.getSize(), file.getContentType());

        // 1. Extract raw document(s)
        List<Document> rawDocuments = extractDocuments(file);
        if (rawDocuments.isEmpty()) {
            throw new IllegalArgumentException("No readable text content could be extracted from: " + originalFilename);
        }

        // 2. Perform chunking & splitting
        List<Document> chunks = splitDocuments(rawDocuments);
        if (chunks.isEmpty()) {
            chunks = rawDocuments;
        }

        // Enrich chunks with chunk-specific metadata
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("chunk_index", i + 1);
            chunk.getMetadata().put("total_chunks", chunks.size());
        }

        // 3. Store chunks in vector database
        log.info("Storing {} chunks into VectorStore for file: '{}'", chunks.size(), originalFilename);
        storeDocuments(chunks);

        return DocumentUploadResponse.builder()
                .status("SUCCESS")
                .fileName(originalFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .rawDocumentsCount(rawDocuments.size())
                .chunksCount(chunks.size())
                .message("Successfully extracted " + rawDocuments.size() + " raw page(s)/section(s), split into "
                        + chunks.size() + " chunk(s), and stored in vector database.")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public List<DocumentUploadResponse> processAndStoreMultiple(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided for batch processing.");
        }

        List<DocumentUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                responses.add(processAndStore(file));
            }
        }
        return responses;
    }

    @Override
    public List<Document> extractDocuments(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType();
        boolean isPdf = originalFilename.toLowerCase().endsWith(".pdf")
                || "application/pdf".equalsIgnoreCase(contentType);

        List<Document> extractedDocs = new ArrayList<>();

        if (isPdf) {
            log.info("Parsing PDF file using PagePdfDocumentReader: {}", originalFilename);
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> pdfPages = pdfReader.get();
            if (pdfPages != null) {
                extractedDocs.addAll(pdfPages);
            }
        } else {
            log.info("Parsing text-based file: {}", originalFilename);
            String textContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (textContent.trim().isEmpty()) {
                throw new IllegalArgumentException("The uploaded file contains no text content.");
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", originalFilename);
            metadata.put("filename", originalFilename);
            metadata.put("content_type", contentType != null ? contentType : "text/plain");
            metadata.put("file_size", file.getSize());

            extractedDocs.add(new Document(textContent, metadata));
        }

        // Attach standard metadata to all extracted documents
        for (Document doc : extractedDocs) {
            Map<String, Object> meta = doc.getMetadata();
            meta.putIfAbsent("source", originalFilename);
            meta.putIfAbsent("filename", originalFilename);
            meta.put("upload_time", LocalDateTime.now().toString());
            meta.put("content_type", contentType != null ? contentType : "application/octet-stream");
            meta.put("file_size", file.getSize());
        }

        return extractedDocs;
    }

    @Override
    public List<Document> splitDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        return tokenTextSplitter.apply(documents);
    }

    @Override
    public void storeDocuments(List<Document> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            log.warn("storeDocuments called with empty chunk list, skipping storage.");
            return;
        }
        vectorStore.add(chunks);
        log.info("Successfully added {} chunks to vector database.", chunks.size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be null or empty.");
        }
    }
}
