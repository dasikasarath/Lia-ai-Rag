package ai.example.open.controller;

import ai.example.open.dto.Quest;
import ai.example.open.service.AIservice;
import ai.example.open.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class Aicontroller {

    private final AIservice as;
    private final DocumentService documentService;

    public Aicontroller(AIservice as, DocumentService documentService) {
        this.as = as;
        this.documentService = documentService;
    }

    @PostMapping("/chat")
    public String chat(@RequestBody Quest question) {
        return as.asku(question);
    }

    @PostMapping("/rag")
    public String rag(@RequestBody Quest que) {
        return as.searchres(que.getQuestion(), que.getConversationId());
    }

    @PostMapping("/chat/clear")
    public ResponseEntity<Map<String, String>> clearMemory(@RequestBody(required = false) Quest quest) {
        String convId = (quest != null && quest.getConversationId() != null)
                ? quest.getConversationId()
                : "default-session";
        as.clearMemory(convId);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Memory cleared for conversation: " + convId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/path")
    public ResponseEntity<Map<String, String>> legacyPath() {
        return ResponseEntity.ok(Map.of(
                "message", "Hardcoded document extraction has been replaced by dynamic multipart upload.",
                "instruction", "Please use POST /api/documents/upload with a MultipartFile to extract, chunk, and store documents."
        ));
    }

    @GetMapping("/chunk")
    public ResponseEntity<Map<String, String>> legacyChunk() {
        return ResponseEntity.ok(Map.of(
                "message", "Dynamic chunking is available via POST /api/documents/chunk.",
                "instruction", "Send a MultipartFile to POST /api/documents/chunk to preview text chunks."
        ));
    }

    @GetMapping("/vss")
    public ResponseEntity<Map<String, String>> legacyVss() {
        return ResponseEntity.ok(Map.of(
                "message", "Vector storage is now fully dynamic via multipart file upload.",
                "instruction", "Use POST /api/documents/upload or POST /upload with 'file' parameter to ingest any document."
        ));
    }
}
