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

    /**
     * Handles ticket purchase with Concurrency, Idempotency and payment protection.
     */
    public Order purchaseTicket(User user, Long categoryId, int quantity, String idempotencyKey) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        String idempKeyRedis = "idemp:" + idempotencyKey;
        if (!redisService.setIfAbsentIdempotencyKey(idempKeyRedis, Duration.ofMinutes(10))) {
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Order is processing"));
        }

        Order order;
        try {
            order = self.reserveTickets(user, categoryId, quantity, idempotencyKey);
        } catch (RuntimeException ex) {
            redisService.releaseLock(idempKeyRedis);
            throw ex;
        }

        try {
            PaymentResult result = paymentGatewayService.processPayment(order);
            return self.finalizeOrderSuccess(order.getId(), categoryId, quantity, user);
        } catch (PaymentDeclinedException e) {
            return self.finalizeOrderFailure(order.getId(), categoryId, quantity);
        }
    }

    @Transactional
    public Order reserveTickets(User user, Long categoryId, int quantity, String idempotencyKey) {
        String lockKey = "lock:purchase:" + user.getId() + ":" + categoryId;
        if (!redisService.acquireLock(lockKey, Duration.ofSeconds(10))) {
            throw new IllegalStateException("Too many requests. Please try again later.");
        }

        try {
            TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            int maxPerUser = TicketPurchaseLimit.getLimit(category.getName());
            int alreadyPurchased = ticketRepository.countByOwnerAndCategoryAndOrderStatus(
                    user, category, OrderStatus.COMPLETED);
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

            PricingStrategy pricingStrategy = new StandardPricingStrategy();
            BigDecimal unitPrice = pricingStrategy.calculatePrice(category.getPrice());
            BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));

            Order order = Order.builder()
                    .user(user)
                    .totalAmount(totalPrice)
                    .status(OrderStatus.PENDING)
                    .idempotencyKey(idempotencyKey)
                    .createdAt(LocalDateTime.now())
                    .build();

            order = orderRepository.save(order);
            orderStateContext.processPayment(order);
            order = orderRepository.save(order);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .ticketCategory(category)
                    .quantity(quantity)
                    .price(unitPrice)
                    .build();
            orderItemRepository.save(orderItem);

            return order;
        } finally {
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

        TicketCategory category = ticketCategoryRepository.findByIdWithPessimisticLock(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.setAvailableQuantity(category.getAvailableQuantity() + quantity);
        ticketCategoryRepository.save(category);

        orderStateContext.cancelOrder(order);
        return orderRepository.save(order);
    }
}
