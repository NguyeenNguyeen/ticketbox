package com.ticketbox.backend.worker.email;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import java.util.List;

@Service
@Slf4j
public class TicketAttachmentService {

    public byte[] generateETicketPdf(List<Ticket> tickets) {
        log.info("Generating E-ticket PDF for {} tickets", tickets.size());

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (Ticket ticket : tickets) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    // Header
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText("TicketBox E-Ticket");
                    contentStream.endText();

                    // Ticket Info
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                    contentStream.newLineAtOffset(50, 650);
                    contentStream.showText("Ticket ID: " + ticket.getId());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Order ID: " + ticket.getOrder().getId());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Category: " + ticket.getCategory().getName());
                    contentStream.newLineAtOffset(0, -20);
                    contentStream.showText("Concert: " + ticket.getCategory().getConcert().getName());
                    contentStream.endText();

                    // QR Code
                    byte[] qrCodeBytes = generateQRCode(ticket.getQrCode());
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrCodeBytes, "qr-" + ticket.getId());
                    contentStream.drawImage(pdImage, 50, 400, 200, 200);
                }
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException | WriterException e) {
            log.error("Failed to generate E-ticket PDF", e);
            throw new RuntimeException("E-ticket PDF generation failed", e);
        }
    }

    private byte[] generateQRCode(String data) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        }
    }
}
