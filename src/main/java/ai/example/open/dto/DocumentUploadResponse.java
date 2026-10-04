package ai.example.open.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {

    private String status;
    private String fileName;
    private String contentType;
    private long fileSize;
    private int rawDocumentsCount;
    private int chunksCount;
    private String message;
    private LocalDateTime timestamp;
}
