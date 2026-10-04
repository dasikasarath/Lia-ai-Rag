package ai.example.open.service;

import ai.example.open.dto.DocumentUploadResponse;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {

    /**
     * Extracts text from an uploaded MultipartFile, splits it into chunks,
     * enriches chunks with metadata, and stores them in the Vector database.
     *
     * @param file the uploaded multipart file (PDF, TXT, MD, CSV, etc.)
     * @return structured metadata and ingestion summary response
     * @throws IOException if reading the file fails
     */
    DocumentUploadResponse processAndStore(MultipartFile file) throws IOException;

    /**
     * Processes multiple uploaded files in batch.
     *
     * @param files array of uploaded multipart files
     * @return list of upload responses
     * @throws IOException if reading files fails
     */
    List<DocumentUploadResponse> processAndStoreMultiple(MultipartFile[] files) throws IOException;

    /**
     * Extracts raw Document objects from a MultipartFile without splitting or storing.
     *
     * @param file the uploaded file
     * @return list of extracted Document instances
     * @throws IOException if extraction fails
     */
    List<Document> extractDocuments(MultipartFile file) throws IOException;

    /**
     * Splits extracted Document objects into smaller chunks using a text splitter.
     *
     * @param documents list of raw Document objects
     * @return list of chunked Document objects
     */
    List<Document> splitDocuments(List<Document> documents);

    /**
     * Stores chunks into the configured VectorStore.
     *
     * @param chunks list of document chunks
     */
    void storeDocuments(List<Document> chunks);
}
