package com.ticketbox.backend.worker.csv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportProgress {
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private int totalRows;
    private int processedRows;
    private int successfulRows;
    private int failedRows;
}
