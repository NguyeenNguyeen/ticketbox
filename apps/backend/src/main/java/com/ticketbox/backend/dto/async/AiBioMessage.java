package com.ticketbox.backend.dto.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiBioMessage {
    private String jobId;
    private Long concertId;
    private Long organizerId;
    private String pdfStoragePath;
    private String correlationId;
    private Map<String, Object> metadata;
}
