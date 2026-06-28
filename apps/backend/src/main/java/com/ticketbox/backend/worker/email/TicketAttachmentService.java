package com.ticketbox.backend.worker.email;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ticketbox.backend.dto.email.EmailAttachment;
import com.ticketbox.backend.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TicketAttachmentService {

    public List<EmailAttachment> generateETicketPdfs(List<Ticket> tickets) {
        log.info("Generating E-ticket PDFs for {} tickets", tickets.size());
        List<EmailAttachment> attachments = new ArrayList<>();

        java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        for (Ticket ticket : tickets) {
            try (PDDocument document = new PDDocument();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                // Custom page size to look like a mobile ticket
                org.apache.pdfbox.pdmodel.common.PDRectangle ticketSize = new org.apache.pdfbox.pdmodel.common.PDRectangle(400, 650);
                PDPage page = new PDPage(ticketSize);
                document.addPage(page);

                // Load Unicode fonts
                org.apache.pdfbox.pdmodel.font.PDFont normalFont;
                org.apache.pdfbox.pdmodel.font.PDFont boldFont;
                try {
                    normalFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, new org.springframework.core.io.ClassPathResource("fonts/DejaVuSans.ttf").getInputStream());
                    boldFont = org.apache.pdfbox.pdmodel.font.PDType0Font.load(document, new org.springframework.core.io.ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream());
                } catch (Exception e) {
                    log.warn("Failed to load DejaVu fonts, falling back to standard fonts", e);
                    normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                }

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // --- 1. Draw Header Background ---
                    contentStream.setNonStrokingColor(new java.awt.Color(91, 33, 182)); // #5B21B6
                    contentStream.addRect(0, 530, 400, 120);
                    contentStream.fill();

                    // --- 2. Write Header Text ---
                    contentStream.beginText();
                    try {
                        contentStream.setFont(normalFont, 12);
                        contentStream.setNonStrokingColor(new java.awt.Color(255, 255, 255)); // White
                        contentStream.newLineAtOffset(165, 620);
                        contentStream.showText("E-TICKET");
                    } finally {
                        contentStream.endText();
                    }

                    contentStream.beginText();
                    try {
                        contentStream.setFont(boldFont, 20);
                        String concertTitle = ticket.getCategory().getConcert().getName();
                        float titleWidth = boldFont.getStringWidth(concertTitle) / 1000 * 20;
                        float startX = (400 - titleWidth) / 2;
                        if (startX < 20) startX = 20; // safe margin
                        contentStream.newLineAtOffset(startX, 580);
                        contentStream.showText(concertTitle.length() > 30 ? concertTitle.substring(0, 27) + "..." : concertTitle);
                    } catch (Exception e) {
                        log.warn("Failed to render concert title text: {}", e.getMessage());
                    } finally {
                        contentStream.endText();
                    }

                    // --- 3. Write Ticket Details ---
                    String dateStr = ticket.getCategory().getConcert().getStartTime() != null 
                            ? ticket.getCategory().getConcert().getStartTime().format(dateFormatter) 
                            : "Date TBD";
                    String venueStr = ticket.getCategory().getConcert().getLocation() != null ? ticket.getCategory().getConcert().getLocation() : "Venue TBD";
                    String holderStr = ticket.getOrder().getUser().getFullName() != null && !ticket.getOrder().getUser().getFullName().isEmpty() 
                            ? ticket.getOrder().getUser().getFullName() 
                            : ticket.getOrder().getUser().getUsername();

                    contentStream.setNonStrokingColor(new java.awt.Color(63, 63, 70)); // #3f3f46
                    
                    contentStream.beginText();
                    try {
                        contentStream.setFont(normalFont, 14);
                        contentStream.newLineAtOffset(40, 480);
                        contentStream.showText("Date: " + dateStr);
                        contentStream.newLineAtOffset(0, -30);
                        contentStream.showText("Venue: " + (venueStr.length() > 35 ? venueStr.substring(0, 32) + "..." : venueStr));
                        contentStream.newLineAtOffset(0, -30);
                        contentStream.showText("Holder: " + holderStr);
                    } catch (Exception e) {
                        log.warn("Failed to render ticket details text: {}", e.getMessage());
                    } finally {
                        contentStream.endText();
                    }

                    // --- 4. Draw Perforation Line ---
                    contentStream.setStrokingColor(new java.awt.Color(228, 228, 231)); // #e4e4e7
                    contentStream.setLineDashPattern(new float[]{5, 5}, 0);
                    contentStream.setLineWidth(2f);
                    contentStream.moveTo(0, 350);
                    contentStream.lineTo(400, 350);
                    contentStream.stroke();

                    // --- 5. Draw QR Code ---
                    byte[] qrCodeBytes = generateQRCode(ticket.getQrCode());
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrCodeBytes, "qr-" + ticket.getId());
                    contentStream.drawImage(pdImage, 110, 130, 180, 180);
                    
                    // QR Text
                    contentStream.beginText();
                    try {
                        // COURIER or normalFont
                        contentStream.setFont(normalFont, 12);
                        contentStream.setNonStrokingColor(new java.awt.Color(113, 113, 122)); // #71717A
                        float qrTextWidth = normalFont.getStringWidth(ticket.getQrCode()) / 1000 * 12;
                        contentStream.newLineAtOffset((400 - qrTextWidth) / 2, 110);
                        contentStream.showText(ticket.getQrCode());
                    } catch (Exception e) {
                        log.warn("Failed to render QR text: {}", e.getMessage());
                    } finally {
                        contentStream.endText();
                    }

                    // --- 6. Draw Seat Info Footer ---
                    contentStream.setNonStrokingColor(new java.awt.Color(244, 244, 245)); // #f4f4f5
                    contentStream.addRect(0, 0, 400, 80);
                    contentStream.fill();

                    contentStream.beginText();
                    try {
                        contentStream.setFont(normalFont, 12);
                        contentStream.setNonStrokingColor(new java.awt.Color(113, 113, 122));
                        contentStream.newLineAtOffset(80, 50);
                        contentStream.showText("ZONE");
                        contentStream.newLineAtOffset(180, 0);
                        contentStream.showText("TICKET ID");
                    } finally {
                        contentStream.endText();
                    }

                    contentStream.beginText();
                    try {
                        contentStream.setFont(boldFont, 16);
                        contentStream.setNonStrokingColor(new java.awt.Color(91, 33, 182)); // #5B21B6
                        contentStream.newLineAtOffset(80, 30);
                        String zone = ticket.getCategory().getName();
                        contentStream.showText(zone.length() > 10 ? zone.substring(0, 7) + "..." : zone);
                        contentStream.newLineAtOffset(180, 0);
                        contentStream.showText(String.valueOf(ticket.getId()));
                    } catch (Exception e) {
                        log.warn("Failed to render seat info text: {}", e.getMessage());
                    } finally {
                        contentStream.endText();
                    }
                }

                document.save(baos);
                attachments.add(new EmailAttachment(
                        "eticket-" + ticket.getId() + ".pdf",
                        baos.toByteArray(),
                        "application/pdf",
                        null
                ));
            } catch (IOException | WriterException e) {
                log.error("Failed to generate E-ticket PDF for ticket {}", ticket.getId(), e);
                throw new RuntimeException("E-ticket PDF generation failed for ticket " + ticket.getId(), e);
            }
        }
        return attachments;
    }

    public byte[] generateQRCode(String data) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        }
    }
}
