package com.ticketbox.backend.dto.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachment {
    private String filename;
    private byte[] data;
    private String mimeType;
    private String contentId; // If set, this is an inline image
}
