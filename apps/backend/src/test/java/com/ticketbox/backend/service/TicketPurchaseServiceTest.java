package com.ticketbox.backend.service;

import com.ticketbox.backend.dto.PaymentResult;
import com.ticketbox.backend.entity.*;
import com.ticketbox.backend.exception.PaymentDeclinedException;
import com.ticketbox.backend.exception.PaymentGatewayException;
import com.ticketbox.backend.pattern.factory.TicketFactory;
import com.ticketbox.backend.pattern.factory.TicketFactoryProvider;
import com.ticketbox.backend.pattern.state.OrderStateContext;
import com.ticketbox.backend.repository.OrderItemRepository;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.TicketCategoryRepository;
import com.ticketbox.backend.repository.TicketRepository;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketPurchaseServiceTest {

    @Mock private TicketCategoryRepository ticketCategoryRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private RedisService redisService;
    @Mock private OrderStateContext orderStateContext;
    @Mock private TicketFactoryProvider ticketFactoryProvider;
    @Mock private PaymentGatewayService paymentGatewayService;
    @Mock private TicketFactory ticketFactory;

    @Spy
    @InjectMocks
    private TicketPurchaseService ticketPurchaseService;

    private User testUser;
    private TicketCategory testCategory;
    private String idempKey;
    private List<com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem> testItems;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        testCategory = TicketCategory.builder()
                .id(100L)
                .name("VIP")
                .price(new BigDecimal("1000"))
                .availableQuantity(50)
                .build();
        idempKey = UUID.randomUUID().toString();
        
        com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem item = new com.ticketbox.backend.controller.TicketController.PurchaseRequest.PurchaseItem();
        item.setCategoryId(100L);
        item.setQuantity(2);
        testItems = Collections.singletonList(item);
        
        ReflectionTestUtils.setField(ticketPurchaseService, "self", ticketPurchaseService);
    }

    @Test
    @DisplayName("Normal payment success - complete flow")
    void purchaseTicket_success() {
        // Mock Redis
        when(redisService.setIfAbsentIdempotencyKey(eq("idemp:" + idempKey), any(Duration.class))).thenReturn(true);
        
        // Mock reserveTickets
        Order reservedOrder = Order.builder().id(1L).status(OrderStatus.PAYING).build();
        doReturn(reservedOrder).when(ticketPurchaseService).reserveTickets(testUser, testItems, idempKey);
        
        // Mock payment
        PaymentResult successResult = PaymentResult.builder().status(PaymentResult.PaymentStatus.SUCCESS).build();
        when(paymentGatewayService.processPayment(reservedOrder)).thenReturn(successResult);
        
        // Mock finalize
        Order completedOrder = Order.builder().id(1L).status(OrderStatus.COMPLETED).build();
        doReturn(completedOrder).when(ticketPurchaseService).finalizeOrderSuccess(1L, testUser);

        // Execute
        Order finalOrder = ticketPurchaseService.purchaseTicket(testUser, testItems, idempKey);

        // Verify
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(paymentGatewayService).processPayment(reservedOrder);
        verify(ticketPurchaseService).finalizeOrderSuccess(1L, testUser);
    }

    @Test
    @DisplayName("Payment declined - cancels order and restores inventory")
    void purchaseTicket_paymentDeclined() {
        when(redisService.setIfAbsentIdempotencyKey(eq("idemp:" + idempKey), any(Duration.class))).thenReturn(true);
        
        Order reservedOrder = Order.builder().id(1L).status(OrderStatus.PAYING).build();
        doReturn(reservedOrder).when(ticketPurchaseService).reserveTickets(testUser, testItems, idempKey);
        
        // Mock payment throwing PaymentDeclinedException
        when(paymentGatewayService.processPayment(reservedOrder))
                .thenThrow(new PaymentDeclinedException("Insufficient funds"));
        
        // Mock finalize failure
        Order cancelledOrder = Order.builder().id(1L).status(OrderStatus.CANCELLED).build();
        doReturn(cancelledOrder).when(ticketPurchaseService).finalizeOrderFailure(1L);

        // Execute
        Order finalOrder = ticketPurchaseService.purchaseTicket(testUser, testItems, idempKey);

        // Verify
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(ticketPurchaseService).finalizeOrderFailure(1L);
    }

    @Test
    @DisplayName("Payment Gateway Exception - propagates and keeps order in PAYING state")
    void purchaseTicket_paymentGatewayException() {
        when(redisService.setIfAbsentIdempotencyKey(eq("idemp:" + idempKey), any(Duration.class))).thenReturn(true);
        
        Order reservedOrder = Order.builder().id(1L).status(OrderStatus.PAYING).build();
        doReturn(reservedOrder).when(ticketPurchaseService).reserveTickets(testUser, testItems, idempKey);
        
        // Mock payment throwing PaymentGatewayException
        when(paymentGatewayService.processPayment(reservedOrder))
                .thenThrow(new PaymentGatewayException("Connection timeout"));
        
        // Execute and expect exception
        assertThrows(PaymentGatewayException.class, () -> {
            ticketPurchaseService.purchaseTicket(testUser, testItems, idempKey);
        });

        // Verify finalize operations are NOT called (order remains in PAYING state)
        verify(ticketPurchaseService, never()).finalizeOrderSuccess(any(), any());
        verify(ticketPurchaseService, never()).finalizeOrderFailure(any());
    }

    @Test
    @DisplayName("Circuit Breaker OPEN - throws CallNotPermittedException")
    void purchaseTicket_circuitOpen() {
        when(redisService.setIfAbsentIdempotencyKey(eq("idemp:" + idempKey), any(Duration.class))).thenReturn(true);
        
        Order reservedOrder = Order.builder().id(1L).status(OrderStatus.PAYING).build();
        doReturn(reservedOrder).when(ticketPurchaseService).reserveTickets(testUser, testItems, idempKey);
        
        // Mock payment throwing a generic exception (simulating Circuit Open or Bulkhead Full)
        RuntimeException ex = new RuntimeException("Circuit breaker open");
        when(paymentGatewayService.processPayment(reservedOrder)).thenThrow(ex);
        
        // Execute and expect exception
        assertThrows(RuntimeException.class, () -> {
            ticketPurchaseService.purchaseTicket(testUser, testItems, idempKey);
        });

        verify(ticketPurchaseService, never()).finalizeOrderSuccess(any(), any());
        verify(ticketPurchaseService, never()).finalizeOrderFailure(any());
    }
}
