package com.ticketbox.backend.worker.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiJobProgress {
    private String jobId;
    private Long concertId;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String errorReason;
}
