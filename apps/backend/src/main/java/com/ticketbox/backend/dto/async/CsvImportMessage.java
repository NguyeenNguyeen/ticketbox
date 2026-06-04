package com.ticketbox.backend.dto.async;

import java.io.Serializable;

/**
 * Message payload for importing guest list from a CSV file.
 */
public class CsvImportMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private String fileId;
    private Long concertId;
    private Long organizerId;

    public CsvImportMessage() {
    }

    public CsvImportMessage(String jobId, String fileId, Long concertId, Long organizerId) {
        this.jobId = jobId;
        this.fileId = fileId;
        this.concertId = concertId;
        this.organizerId = organizerId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Long getConcertId() {
        return concertId;
    }

    public void setConcertId(Long concertId) {
        this.concertId = concertId;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    @Override
    public String toString() {
        return "CsvImportMessage{" +
                "jobId='" + jobId + '\'' +
                ", fileId='" + fileId + '\'' +
                ", concertId=" + concertId +
                ", organizerId=" + organizerId +
                '}';
    }
}
