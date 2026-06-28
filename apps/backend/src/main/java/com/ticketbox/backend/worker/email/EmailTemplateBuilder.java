package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.Ticket;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public String buildOrderConfirmationHtml(Order order, List<Ticket> tickets) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head><style>")
            .append("body { font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f5; margin: 0; padding: 20px; color: #18181b; }")
            .append(".container { max-width: 600px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 16px; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }")
            .append(".header { text-align: center; margin-bottom: 30px; }")
            .append(".header h1 { color: #5B21B6; margin: 0; font-size: 24px; }")
            .append(".header p { color: #71717A; margin-top: 5px; }")
            .append(".ticket-card { border: 1px solid #e4e4e7; border-radius: 16px; overflow: hidden; margin-bottom: 24px; background: #fff; }")
            .append(".ticket-header { background: linear-gradient(to right, hsl(250, 84%, 54%), hsl(270, 70%, 55%)); color: #fff; padding: 24px; text-align: center; }")
            .append(".ticket-header p { margin: 0; font-size: 12px; opacity: 0.8; letter-spacing: 1px; }")
            .append(".ticket-header h2 { margin: 5px 0 0 0; font-size: 20px; font-weight: 700; }")
            .append(".ticket-details { padding: 24px; }")
            .append(".detail-row { margin-bottom: 12px; font-size: 14px; display: flex; align-items: center; color: #3f3f46; }")
            .append(".qr-section { padding: 24px; text-align: center; border-top: 2px dashed #e4e4e7; position: relative; }")
            .append(".qr-section img { display: block; margin: 0 auto; width: 180px; height: 180px; }")
            .append(".qr-code-text { font-family: monospace; font-size: 12px; color: #71717A; margin-top: 12px; }")
            .append(".seat-info { background: #f4f4f5; padding: 16px; display: flex; justify-content: center; gap: 40px; text-align: center; }")
            .append(".seat-box p.label { margin: 0; font-size: 12px; color: #71717A; text-transform: uppercase; }")
            .append(".seat-box p.val { margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #5B21B6; }")
            .append(".footer { margin-top: 30px; font-size: 12px; color: #a1a1aa; text-align: center; border-top: 1px solid #e4e4e7; padding-top: 20px; }")
            .append("</style></head>")
            .append("<body>")
            .append("<div class='container'>")
            .append("<div class='header'>")
            .append("<h1>TicketBox</h1>")
            .append("<p>Hi ").append(order.getUser().getUsername()).append(", your order <strong>#").append(order.getId()).append("</strong> is confirmed.</p>")
            .append("</div>");

        for (Ticket ticket : tickets) {
            String concertTitle = ticket.getCategory().getConcert().getName();
            String dateStr = ticket.getCategory().getConcert().getStartTime() != null 
                             ? ticket.getCategory().getConcert().getStartTime().format(DATE_FORMATTER) 
                             : "Date TBD";
            String venue = ticket.getCategory().getConcert().getLocation() != null ? ticket.getCategory().getConcert().getLocation() : "Venue TBD";
            String holder = order.getUser().getFullName() != null && !order.getUser().getFullName().isEmpty() ? order.getUser().getFullName() : order.getUser().getUsername();
            
            html.append("<div class='ticket-card'>")
                // Header
                .append("<div class='ticket-header'>")
                .append("<p>E-TICKET</p>")
                .append("<h2>").append(concertTitle).append("</h2>")
                .append("</div>")
                
                // Details
                .append("<div class='ticket-details'>")
                .append("<div class='detail-row'>📅 ").append(dateStr).append("</div>")
                .append("<div class='detail-row'>📍 ").append(venue).append("</div>")
                .append("<div class='detail-row'>👤 ").append(holder).append("</div>")
                .append("</div>")
                
                // QR Code
                .append("<div class='qr-section'>")
                .append("<img src='cid:qr-").append(ticket.getId()).append("' alt='QR Code' />")
                .append("<div class='qr-code-text'>").append(ticket.getQrCode()).append("</div>")
                .append("</div>")
                
                // Seat Info
                .append("<div class='seat-info'>")
                .append("<div class='seat-box'><p class='label'>Khu vực</p><p class='val'>").append(ticket.getCategory().getName()).append("</p></div>")
                .append("<div class='seat-box'><p class='label'>Mã vé</p><p class='val'>").append(ticket.getId()).append("</p></div>")
                .append("</div>")
                
                .append("</div>");
        }

        html.append("<div class='footer'>TicketBox &copy; 2026. This is an automated email. Do not share your QR codes.</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");

        return html.toString();
    }
}
