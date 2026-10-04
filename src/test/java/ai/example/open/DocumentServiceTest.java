package ai.example.open;

import ai.example.open.dto.DocumentUploadResponse;
import ai.example.open.service.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private VectorStore vectorStore;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(vectorStore);
    }

    @Test
    void testProcessAndStoreTextFile() throws IOException {
        String content = "Spring AI provides high-level abstractions for AI models and Vector Stores. "
                + "It enables developers to build powerful RAG architectures with ease. "
                + "Chunking and splitting documents is an essential step in building retrieval augmented generation.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample_document.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadResponse response = documentService.processAndStore(file);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("sample_document.txt", response.getFileName());
        assertTrue(response.getChunksCount() > 0);
        assertTrue(response.getRawDocumentsCount() > 0);

        // Verify vectorStore.add was invoked with the chunks
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    void testExtractDocumentsTextFile() throws IOException {
        String content = "Hello Spring AI Document Processing";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.md",
                "text/markdown",
                content.getBytes(StandardCharsets.UTF_8)
        );

        List<Document> docs = documentService.extractDocuments(file);
        assertEquals(1, docs.size());
        assertEquals("notes.md", docs.get(0).getMetadata().get("filename"));
        assertEquals("notes.md", docs.get(0).getMetadata().get("source"));
        assertTrue(docs.get(0).getText().contains("Hello Spring AI"));
    }

    @Test
    void testEmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> documentService.processAndStore(emptyFile));
    }

    @Test
    void testSplitDocuments() {
        Document doc1 = new Document("Line 1 of test document. This is sentence one.");
        Document doc2 = new Document("Line 2 of test document. This is sentence two.");

        List<Document> chunks = documentService.splitDocuments(List.of(doc1, doc2));
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
    }
}
