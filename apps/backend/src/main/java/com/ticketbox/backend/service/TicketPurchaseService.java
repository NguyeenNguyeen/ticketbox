package com.ticketbox.backend.service;

import com.ticketbox.backend.entity.*;
import com.ticketbox.backend.pattern.factory.TicketFactory;
import com.ticketbox.backend.pattern.factory.TicketFactoryProvider;
import com.ticketbox.backend.pattern.state.OrderStateContext;
import com.ticketbox.backend.pattern.strategy.PricingStrategy;
import com.ticketbox.backend.pattern.strategy.StandardPricingStrategy;
import com.ticketbox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class TicketPurchaseService {

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private OrderStateContext orderStateContext;

    @Autowired
    private TicketFactoryProvider ticketFactoryProvider;

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private TicketPurchaseService self;

    /**
     * Handles ticket purchase with Concurrency & Idempotency protection.
     * This method orchestrates the 3-phase payment protection flow.
     */
    public Order purchaseTicket(User user, Long categoryId, int quantity, String idempotencyKey) {
        
        // 1. Idempotency Check (Redis)
        String idempKeyRedis = "idemp:" + idempotencyKey;
        if (!redisService.setIfAbsentIdempotencyKey(idempKeyRedis, Duration.ofMinutes(10))) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Order is processing"));
        }

        // Phase A: Reserve Tickets (Transaction 1)
        Order order;
        try {
            order = self.reserveTickets(user, categoryId, quantity, idempotencyKey);
        } catch (Exception e) {
            redisService.releaseLock("lock:purchase:" + user.getId() + ":" + categoryId);
            throw e; // e.g. IllegalStateException if oversold or per-user limit exceeded
        }

        // Phase B: Process Payment (No Database Transaction, External I/O)
        try {
            paymentGatewayService.processPayment(order);
        } catch (com.ticketbox.backend.exception.PaymentDeclinedException e) {
            // Payment declined (insufficient funds). Cancel and restore.
            return self.finalizeOrderFailure(order.getId(), categoryId, quantity);
        } catch (Exception e) {
            // Infrastructure error (PaymentGatewayException, CallNotPermittedException, BulkheadFullException).
            // Do NOT restore tickets. Keep in PAYING state for 10 minutes.
            // Re-throw so GlobalExceptionHandler/Controller can return PAYMENT_MAINTENANCE.
            throw e;
        }

        // Phase C: Finalize Success (Transaction 2)
        return self.finalizeOrderSuccess(order.getId(), categoryId, quantity, user);
    }

    @Transactional
    public Order reserveTickets(User user, Long categoryId, int quantity, String idempotencyKey) {
        String lockKey = "lock:purchase:" + user.getId() + ":" + categoryId;
        if (!redisService.acquireLock(lockKey, Duration.ofSeconds(10))) {
            throw new IllegalStateException("Too many requests. Please try again later.");
        }
        
        try {
            // Pessimistic Locking on Database to prevent oversell
            TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if (category.getAvailableQuantity() < quantity) {
                throw new IllegalStateException("Not enough tickets available. Oversell prevented.");
            }

            // Per-user limit enforcement
            int maxPerUser = getMaxPerUser(category.getName());
            int alreadyPurchased = orderItemRepository.sumQuantityByUserAndCategoryAndStatus(
                    user.getId(), categoryId, OrderStatus.COMPLETED);
            if (alreadyPurchased + quantity > maxPerUser) {
                throw new IllegalStateException(
                    "Purchase limit exceeded. You can only buy " + maxPerUser +
                    " tickets of type " + category.getName() + " per account. " +
                    "You have already purchased " + alreadyPurchased + ".");
            }

            // Update quantity
            category.setAvailableQuantity(category.getAvailableQuantity() - quantity);
            ticketCategoryRepository.save(category);

            // Calculate Price (Strategy Pattern)
            PricingStrategy pricingStrategy = new StandardPricingStrategy();
            BigDecimal unitPrice = pricingStrategy.calculatePrice(category.getPrice());
            BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));

            // Create Order (State Pattern)
            Order order = Order.builder()
                    .user(user)
                    .totalAmount(totalPrice)
                    .status(OrderStatus.PENDING)
                    .idempotencyKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            order = orderRepository.save(order);
            
            // Move order state to PAYING (reservation held)
            orderStateContext.processPayment(order);
            order = orderRepository.save(order);
            
            // Create OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .ticketCategory(category)
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();
            orderItemRepository.save(orderItem);

            return order;
        } finally {
            // We release the distributed lock here because reservation is complete.
            // The DB pessimistic lock is released when this transaction commits.
            redisService.releaseLock(lockKey);
        }
    }

    @Transactional
    public Order finalizeOrderSuccess(Long orderId, Long categoryId, int quantity, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        TicketCategory category = ticketCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        orderStateContext.completeOrder(order);
        order = orderRepository.save(order);

        // Create Tickets (Abstract Factory Pattern)
        TicketFactory factory = ticketFactoryProvider.getFactory(category.getName());
        for (int i = 0; i < quantity; i++) {
            Ticket ticket = factory.createTicket(category, user, order);
            ticketRepository.save(ticket);
        }

        return order;
    }

    @Transactional
    public Order finalizeOrderFailure(Long orderId, Long categoryId, int quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        // Restore inventory (pessimistic lock to ensure consistency)
        TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        
        category.setAvailableQuantity(category.getAvailableQuantity() + quantity);
        ticketCategoryRepository.save(category);

        orderStateContext.cancelOrder(order);
        return orderRepository.save(order);
    }

    private int getMaxPerUser(String categoryName) {
        String upper = categoryName.toUpperCase();
        if (upper.contains("SVIP")) return 2;
        if (upper.contains("VIP")) return 2;
        return 4;
    }
}
