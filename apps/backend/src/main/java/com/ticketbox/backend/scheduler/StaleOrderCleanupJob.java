package com.ticketbox.backend.scheduler;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderItem;
import com.ticketbox.backend.entity.OrderStatus;
import com.ticketbox.backend.repository.OrderItemRepository;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.service.TicketPurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StaleOrderCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderCleanupJob.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TicketPurchaseService ticketPurchaseService;

    @Value("${ticketbox.payment.reservation-ttl-minutes:10}")
    private int reservationTtlMinutes;

    /**
     * Runs every 60 seconds (60000ms) to clean up orders stuck in PAYING state.
     * This prevents inventory leaks when payments fail or the circuit is open.
     */
    @Scheduled(fixedDelayString = "${ticketbox.payment.cleanup-interval-seconds:60}000")
    @Transactional
    public void cleanupStaleOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(reservationTtlMinutes);
        
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PAYING, cutoff);
        
        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("Found {} stale orders in PAYING state older than {} minutes. Starting cleanup...", 
                 staleOrders.size(), reservationTtlMinutes);

        int cancelledCount = 0;
        for (Order order : staleOrders) {
            try {
                log.warn("Stale order {} expired due to payment reservation timeout. Cancelling...", order.getId());
                
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                if (items.isEmpty()) {
                    log.error("Order {} has no items, cannot restore inventory.", order.getId());
                    continue;
                }
                
                // For TicketBox, we only allow one category per purchase flow right now
                OrderItem item = items.get(0);
                
                ticketPurchaseService.finalizeOrderFailure(order.getId(), item.getTicketCategory().getId(), item.getQuantity());
                
                cancelledCount++;
            } catch (Exception e) {
                log.error("Failed to cleanup stale order {}", order.getId(), e);
            }
        }
        
        if (cancelledCount > 0) {
            log.info("Successfully cleaned up {} stale orders.", cancelledCount);
        }
    }
}
