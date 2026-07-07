package com.ticketbox.backend.service;

import com.ticketbox.backend.config.TicketPurchaseLimit;
import com.ticketbox.backend.dto.PaymentResult;
import com.ticketbox.backend.entity.*;
import com.ticketbox.backend.exception.PaymentDeclinedException;
import com.ticketbox.backend.pattern.factory.TicketFactory;
import com.ticketbox.backend.pattern.factory.TicketFactoryProvider;
import com.ticketbox.backend.pattern.state.OrderStateContext;
import com.ticketbox.backend.pattern.strategy.PricingStrategy;
import com.ticketbox.backend.pattern.strategy.StandardPricingStrategy;
import com.ticketbox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    @Lazy
    private TicketPurchaseService self;

    @Autowired
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    /**
     * Handles ticket purchase with Concurrency, Idempotency and payment protection.
     */
    public Order purchaseTicket(User user, java.util.List<com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem> items, String idempotencyKey) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        String idempKeyRedis = "idemp:" + idempotencyKey;
        if (!redisService.setIfAbsentIdempotencyKey(idempKeyRedis, Duration.ofMinutes(10))) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Order is processing"));
        }

        Order order;
        try {
            order = self.reserveTickets(user, items, idempotencyKey);
        } catch (RuntimeException ex) {
            redisService.releaseLock(idempKeyRedis);
            throw ex;
        }

        try {
            PaymentResult result = paymentGatewayService.processPayment(order);
            return self.finalizeOrderSuccess(order.getId(), user);
        } catch (PaymentDeclinedException e) {
            return self.finalizeOrderFailure(order.getId());
        }
    }

    @Transactional
    public Order reserveTickets(User user, java.util.List<com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem> items, String idempotencyKey) {
        String lockKey = "lock:purchase:" + user.getId();
        if (!redisService.acquireLock(lockKey, Duration.ofSeconds(10))) {
            throw new IllegalStateException("Too many requests. Please try again later.");
        }

        try {
            BigDecimal totalPrice = BigDecimal.ZERO;
            PricingStrategy pricingStrategy = new StandardPricingStrategy();
            
            Order order = Order.builder()
                    .user(user)
                    .totalAmount(BigDecimal.ZERO)
                    .status(OrderStatus.PENDING)
                    .idempotencyKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .build();
            order = orderRepository.save(order);

            for (com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem item : items) {
                Long categoryId = item.getCategoryId();
                int quantity = item.getQuantity();

                if (quantity <= 0) {
                    throw new IllegalArgumentException("Quantity must be greater than 0.");
                }

                TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Category not found"));

                if ("CANCELLED".equals(category.getConcert().getEffectiveStatus())) {
                    throw new IllegalStateException("Cannot purchase tickets for a cancelled or postponed event.");
                }
                if ("UPCOMING".equals(category.getConcert().getEffectiveStatus())) {
                    throw new IllegalStateException("Tickets are not yet on sale for this event.");
                }

                int maxPerUser = category.getMaxPerUser() != null ? category.getMaxPerUser() : 4;
                int alreadyPurchasedCompleted = orderItemRepository.sumQuantityByUserAndCategoryAndStatus(
                        user.getId(), category.getId(), OrderStatus.COMPLETED);
                int alreadyPurchasedPending = orderItemRepository.sumQuantityByUserAndCategoryAndStatus(
                        user.getId(), category.getId(), OrderStatus.PENDING);
                int alreadyPurchased = alreadyPurchasedCompleted + alreadyPurchasedPending;
                if (alreadyPurchased + quantity > maxPerUser) {
                    int remaining = maxPerUser - alreadyPurchased;
                    throw new IllegalStateException(
                            "Purchase limit exceeded for category '" + category.getName() + "'. " +
                            "Max " + maxPerUser + " ticket(s) per account. " +
                            "You have already purchased " + alreadyPurchased + ", " +
                            "remaining quota: " + Math.max(0, remaining) + ".");
                }

                if (category.getAvailableQuantity() < quantity) {
                    throw new IllegalStateException("Not enough tickets available. Oversell prevented.");
                }

                category.setAvailableQuantity(category.getAvailableQuantity() - quantity);
                ticketCategoryRepository.save(category);

                BigDecimal unitPrice = pricingStrategy.calculatePrice(category.getPrice());
                BigDecimal itemTotalPrice = unitPrice.multiply(new BigDecimal(quantity));
                totalPrice = totalPrice.add(itemTotalPrice);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .ticketCategory(category)
                        .quantity(quantity)
                        .price(unitPrice)
                        .build();
                orderItemRepository.save(orderItem);
            }

            order.setTotalAmount(totalPrice);
            orderStateContext.processPayment(order);
            return orderRepository.save(order);
        } finally {
            redisService.releaseLock(lockKey);
        }
    }

    @Transactional
    public Order finalizeOrderSuccess(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        orderStateContext.completeOrder(order);
        order = orderRepository.save(order);

        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        java.util.List<Long> ticketIds = new java.util.ArrayList<>();

        for (OrderItem item : orderItems) {
            TicketCategory category = item.getTicketCategory();
            TicketFactory factory = ticketFactoryProvider.getFactory(category.getName());
            for (int i = 0; i < item.getQuantity(); i++) {
                Ticket ticket = factory.createTicket(category, user, order);
                ticket = ticketRepository.save(ticket);
                ticketIds.add(ticket.getId());
            }
        }

        String jobId = java.util.UUID.randomUUID().toString();
        com.ticketbox.backend.dto.async.EmailTaskMessage message = new com.ticketbox.backend.dto.async.EmailTaskMessage(
                jobId,
                order.getId(),
                user.getId(),
                user.getEmail(),
                ticketIds,
                order.getIdempotencyKey()
        );
        rabbitTemplate.convertAndSend(
                com.ticketbox.backend.config.RabbitMQConfig.EXCHANGE_COMMANDS,
                com.ticketbox.backend.config.RabbitMQConfig.ROUTING_KEY_EMAIL,
                message
        );

        return order;
    }

    @Transactional
    public Order finalizeOrderFailure(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(item.getTicketCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            category.setAvailableQuantity(category.getAvailableQuantity() + item.getQuantity());
            ticketCategoryRepository.save(category);
        }

        orderStateContext.cancelOrder(order);
        return orderRepository.save(order);
    }
}
