package com.ticketbox.backend.dto.async;

import java.io.Serializable;

/**
 * Message payload for importing guest list from a CSV file.
 */
public class CsvImportMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileId;
    private Long concertId;

    public CsvImportMessage() {
    }

    public CsvImportMessage(String fileId, Long concertId) {
        this.fileId = fileId;
        this.concertId = concertId;
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

    @Override
    public String toString() {
        return "CsvImportMessage{" +
                "fileId='" + fileId + '\'' +
                ", concertId=" + concertId +
                '}';
    }
}
