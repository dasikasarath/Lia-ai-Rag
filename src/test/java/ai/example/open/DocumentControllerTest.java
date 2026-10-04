package ai.example.open;

import ai.example.open.controller.DocumentController;
import ai.example.open.dto.DocumentUploadResponse;
import ai.example.open.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        documentController = new DocumentController(documentService);
    }

    @Test
    void testUploadEndpointSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "%PDF-1.4 dummy content".getBytes()
        );

        DocumentUploadResponse mockResponse = DocumentUploadResponse.builder()
                .status("SUCCESS")
                .fileName("test-document.pdf")
                .contentType("application/pdf")
                .fileSize(22)
                .rawDocumentsCount(1)
                .chunksCount(2)
                .message("Successfully extracted 1 page and stored in vector database.")
                .timestamp(LocalDateTime.now())
                .build();

        when(documentService.processAndStore(any())).thenReturn(mockResponse);

        ResponseEntity<?> response = documentController.uploadDocument(file);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof DocumentUploadResponse);

        DocumentUploadResponse body = (DocumentUploadResponse) response.getBody();
        assertEquals("SUCCESS", body.getStatus());
        assertEquals("test-document.pdf", body.getFileName());
        assertEquals(2, body.getChunksCount());
    }

    @Test
    void testUploadEndpointEmptyFileReturnsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        ResponseEntity<?> response = documentController.uploadDocument(emptyFile);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("FAILED", body.get("status"));
    }

    @Test
    void testStatusEndpoint() {
        ResponseEntity<Map<String, Object>> response = documentController.getStatus();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ACTIVE", body.get("status"));
        assertEquals("PgVector", body.get("vectorStore"));
    }
}
