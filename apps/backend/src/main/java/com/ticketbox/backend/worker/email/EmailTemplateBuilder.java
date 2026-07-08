package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.entity.Guest;
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
            .append("<head>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<style>")
            .append("body { font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f5; margin: 0; padding: 10px; color: #18181b; }")
            .append(".container { width: 100%; max-width: 600px; margin: 0 auto; background: #ffffff; padding: 20px; border-radius: 16px; box-sizing: border-box; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }")
            .append(".header { text-align: center; margin-bottom: 24px; }")
            .append(".header h1 { color: #5B21B6; margin: 0; font-size: 24px; }")
            .append(".header p { color: #71717A; margin-top: 5px; font-size: 14px; }")
            .append(".ticket-card { border: 1px solid #e4e4e7; border-radius: 16px; overflow: hidden; margin: 0 auto 24px auto; background: #fff; max-width: 448px; width: 100%; box-sizing: border-box; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }")
            .append(".ticket-header { background: linear-gradient(to right, #6366f1, #a855f7); color: #fff; padding: 24px; text-align: center; }")
            .append(".ticket-header p { margin: 0; font-size: 14px; opacity: 0.8; letter-spacing: 1px; }")
            .append(".ticket-header h2 { margin: 4px 0 0 0; font-size: 20px; font-weight: 700; line-height: 1.2; }")
            .append(".ticket-details { padding: 24px; }")
            .append(".detail-row { margin-bottom: 12px; font-size: 14px; color: #3f3f46; }")
            .append(".detail-row strong { color: #5B21B6; display: inline-block; width: 20px; }")
            .append(".qr-section { padding: 24px; text-align: center; border-top: 2px dashed #e4e4e7; position: relative; }")
            .append(".qr-section::before, .qr-section::after { content: ''; position: absolute; top: -12px; width: 24px; height: 24px; background-color: #ffffff; border-radius: 50%; border: 1px solid #e4e4e7; }")
            .append(".qr-section::before { left: -13px; border-right-color: transparent; border-top-color: transparent; border-bottom-color: transparent; }") // Fake perforation cutouts - email support varies but looks nice if it works
            .append(".qr-section::after { right: -13px; border-left-color: transparent; border-top-color: transparent; border-bottom-color: transparent; }")
            .append(".qr-section img { display: block; margin: 0 auto; width: 180px; height: 180px; }")
            .append(".qr-code-text { font-family: monospace; font-size: 12px; color: #71717A; margin-top: 12px; }")
            .append(".seat-info { background: #f4f4f5; padding: 16px; display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; text-align: center; }")
            .append(".seat-box { min-width: 60px; }")
            .append(".seat-box p.label { margin: 0; font-size: 12px; color: #71717A; text-transform: uppercase; }")
            .append(".seat-box p.val { margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #5B21B6; }")
            .append(".footer { margin-top: 20px; font-size: 12px; color: #a1a1aa; text-align: center; border-top: 1px solid #e4e4e7; padding-top: 20px; }")
            .append("@media only screen and (max-width: 480px) {")
            .append("  .container { padding: 15px; }")
            .append("  .ticket-card { border-radius: 12px; }")
            .append("  .seat-info { gap: 15px; }")
            .append("}")
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
                .append("<div class='detail-row'><strong>📅</strong> ").append(dateStr).append("</div>")
                .append("<div class='detail-row'><strong>📍</strong> ").append(venue).append("</div>")
                .append("<div class='detail-row'><strong>👤</strong> ").append(holder).append("</div>")
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

    public String buildGuestConfirmationHtml(Guest guest, Ticket ticket) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            .append("<style>")
            .append("body { font-family: 'Inter', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f5; margin: 0; padding: 10px; color: #18181b; }")
            .append(".container { width: 100%; max-width: 600px; margin: 0 auto; background: #ffffff; padding: 20px; border-radius: 16px; box-sizing: border-box; box-shadow: 0 10px 25px rgba(0,0,0,0.05); }")
            .append(".header { text-align: center; margin-bottom: 24px; }")
            .append(".header h1 { color: #5B21B6; margin: 0; font-size: 24px; }")
            .append(".header p { color: #71717A; margin-top: 5px; font-size: 14px; }")
            .append(".ticket-card { border: 1px solid #e4e4e7; border-radius: 16px; overflow: hidden; margin: 0 auto 24px auto; background: #fff; max-width: 448px; width: 100%; box-sizing: border-box; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }")
            .append(".ticket-header { background: linear-gradient(to right, #6366f1, #a855f7); color: #fff; padding: 24px; text-align: center; }")
            .append(".ticket-header p { margin: 0; font-size: 14px; opacity: 0.8; letter-spacing: 1px; }")
            .append(".ticket-header h2 { margin: 4px 0 0 0; font-size: 20px; font-weight: 700; line-height: 1.2; }")
            .append(".ticket-details { padding: 24px; }")
            .append(".detail-row { margin-bottom: 12px; font-size: 14px; color: #3f3f46; }")
            .append(".detail-row strong { color: #5B21B6; display: inline-block; width: 20px; }")
            .append(".qr-section { padding: 24px; text-align: center; border-top: 2px dashed #e4e4e7; position: relative; }")
            .append(".qr-section::before, .qr-section::after { content: ''; position: absolute; top: -12px; width: 24px; height: 24px; background-color: #ffffff; border-radius: 50%; border: 1px solid #e4e4e7; }")
            .append(".qr-section::before { left: -13px; border-right-color: transparent; border-top-color: transparent; border-bottom-color: transparent; }")
            .append(".qr-section::after { right: -13px; border-left-color: transparent; border-top-color: transparent; border-bottom-color: transparent; }")
            .append(".qr-section img { display: block; margin: 0 auto; width: 180px; height: 180px; }")
            .append(".qr-code-text { font-family: monospace; font-size: 12px; color: #71717A; margin-top: 12px; }")
            .append(".seat-info { background: #f4f4f5; padding: 16px; display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; text-align: center; }")
            .append(".seat-box { min-width: 60px; }")
            .append(".seat-box p.label { margin: 0; font-size: 12px; color: #71717A; text-transform: uppercase; }")
            .append(".seat-box p.val { margin: 4px 0 0 0; font-size: 16px; font-weight: 700; color: #5B21B6; }")
            .append(".footer { margin-top: 20px; font-size: 12px; color: #a1a1aa; text-align: center; border-top: 1px solid #e4e4e7; padding-top: 20px; }")
            .append("@media only screen and (max-width: 480px) {")
            .append("  .container { padding: 15px; }")
            .append("  .ticket-card { border-radius: 12px; }")
            .append("  .seat-info { gap: 15px; }")
            .append("}")
            .append("</style></head>")
            .append("<body>")
            .append("<div class='container'>")
            .append("<div class='header'>")
            .append("<h1>TicketBox</h1>")
            .append("<p>Hi ").append(guest.getFullName()).append(", your <strong>VIP Guest Ticket</strong> is confirmed.</p>")
            .append("</div>");

        String concertTitle = ticket.getCategory().getConcert().getName();
        String dateStr = ticket.getCategory().getConcert().getStartTime() != null 
                         ? ticket.getCategory().getConcert().getStartTime().format(DATE_FORMATTER) 
                         : "Date TBD";
        String venue = ticket.getCategory().getConcert().getLocation() != null ? ticket.getCategory().getConcert().getLocation() : "Venue TBD";
        
        html.append("<div class='ticket-card'>")
            .append("<div class='ticket-header'>")
            .append("<p>GUEST E-TICKET</p>")
            .append("<h2>").append(concertTitle).append("</h2>")
            .append("</div>")
            
            .append("<div class='ticket-details'>")
            .append("<div class='detail-row'><strong>📅</strong> ").append(dateStr).append("</div>")
            .append("<div class='detail-row'><strong>📍</strong> ").append(venue).append("</div>")
            .append("<div class='detail-row'><strong>👤</strong> ").append(guest.getFullName()).append("</div>")
            .append("</div>")
            
            .append("<div class='qr-section'>")
            .append("<img src='cid:qr-").append(ticket.getId()).append("' alt='QR Code' />")
            .append("<div class='qr-code-text'>").append(ticket.getQrCode()).append("</div>")
            .append("</div>")
            
            .append("<div class='seat-info'>")
            .append("<div class='seat-box'><p class='label'>Khu vực</p><p class='val'>").append(ticket.getCategory().getName()).append("</p></div>")
            .append("<div class='seat-box'><p class='label'>Mã vé</p><p class='val'>").append(ticket.getId()).append("</p></div>")
            .append("</div>")
            
            .append("</div>");

        html.append("<div class='footer'>TicketBox &copy; 2026. This is an automated email. Do not share your QR codes.</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");

        return html.toString();
    }
}
