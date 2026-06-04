package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.entity.Order;
import org.springframework.stereotype.Service;

@Service
public class EmailTemplateBuilder {

    public String buildOrderConfirmationHtml(Order order) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><style>" +
                "body { font-family: Arial, sans-serif; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: #000; color: #fff; padding: 10px; text-align: center; }" +
                ".content { padding: 20px; line-height: 1.6; }" +
                ".footer { margin-top: 20px; font-size: 12px; color: #777; text-align: center; }" +
                "</style></head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><h2>TicketBox Confirmation</h2></div>" +
                "<div class='content'>" +
                "<p>Hi " + order.getUser().getUsername() + ",</p>" +
                "<p>Thank you for your purchase! Your order <strong>#" + order.getId() + "</strong> is confirmed.</p>" +
                "<p>Please find your E-tickets attached as a PDF document. You can present the QR codes at the venue gate for entry.</p>" +
                "<p>Total Amount: <strong>$" + order.getTotalAmount() + "</strong></p>" +
                "<p>We hope you enjoy the event!</p>" +
                "</div>" +
                "<div class='footer'>TicketBox &copy; 2026. This is an automated email.</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
