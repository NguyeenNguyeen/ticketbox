package com.ticketbox.backend.worker.email;

import com.ticketbox.backend.config.RabbitMQConfig;
import com.ticketbox.backend.dto.async.EmailTaskMessage;
import com.ticketbox.backend.entity.*;
import com.ticketbox.backend.repository.OrderRepository;
import com.ticketbox.backend.repository.TicketCategoryRepository;
import com.ticketbox.backend.repository.TicketRepository;
import com.ticketbox.backend.repository.UserRepository;
import com.ticketbox.backend.repository.ConcertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.rabbitmq.listener.simple.concurrency=1",
    "spring.rabbitmq.listener.simple.max-concurrency=1",
    "ticketbox.email.provider=resend"
})
public class EmailWorkerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @Autowired
    private EmailJobTracker jobTracker;

    @MockBean
    private RestTemplate restTemplate;

    private User testUser;
    private Order testOrder;
    private Ticket testTicket;

    @BeforeEach
    public void setup() {
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        concertRepository.deleteAll();
        userRepository.deleteAll();

        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        admin.purgeQueue(RabbitMQConfig.QUEUE_EMAIL, false);
        admin.purgeQueue(RabbitMQConfig.QUEUE_EMAIL_DLQ, false);

        testUser = User.builder()
                .username("emailuser")
                .password("pass")
                .role(RoleName.CUSTOMER)
                .build();
        userRepository.save(testUser);

        Concert testConcert = Concert.builder()
                .name("Email Test Concert")
                .description("Test")
                .location("Venue")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build();
        concertRepository.save(testConcert);

        TicketCategory cat = TicketCategory.builder()
                .concert(testConcert)
                .name("VIP")
                .price(BigDecimal.valueOf(100))
                .totalQuantity(10)
                .availableQuantity(9)
                .build();
        ticketCategoryRepository.save(cat);

        testOrder = Order.builder()
                .user(testUser)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(100))
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .emailStatus(EmailStatus.PENDING)
                .build();
        orderRepository.save(testOrder);

        testTicket = Ticket.builder()
                .category(cat)
                .owner(testUser)
                .order(testOrder)
                .qrCode(UUID.randomUUID().toString())
                .status(TicketStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        ticketRepository.save(testTicket);
    }

    @AfterEach
    public void teardown() {
        ticketRepository.deleteAll();
        orderRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        concertRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSuccessfulEmailDelivery() throws Exception {
        // Mock successful provider response
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn("{\"id\":\"12345\"}");

        String jobId = UUID.randomUUID().toString();
        EmailTaskMessage msg = new EmailTaskMessage(jobId, testOrder.getId(), testUser.getId(), "test@ticketbox.com", List.of(testTicket.getId()), "corr-1");

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_EMAIL, msg);

        Thread.sleep(3000); // Wait for async processing

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(EmailStatus.SENT, updated.getEmailStatus());

        EmailJobTracker.EmailJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
    }

    @Test
    public void testEmailTimeoutAndDlq() throws Exception {
        // Mock constant timeout
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenThrow(new RestClientException("Provider timeout"));

        String jobId = UUID.randomUUID().toString();
        EmailTaskMessage msg = new EmailTaskMessage(jobId, testOrder.getId(), testUser.getId(), "test@ticketbox.com", List.of(testTicket.getId()), "corr-2");

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_EMAIL, msg);

        // Wait for 3 retries (1s, 2s, 4s...) -> ~8-10 seconds total
        Thread.sleep(12000);

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(EmailStatus.PENDING, updated.getEmailStatus()); // Should not be marked sent

        // Check DLQ
        Message dlqMessage = rabbitTemplate.receive(RabbitMQConfig.QUEUE_EMAIL_DLQ, 5000);
        assertNotNull(dlqMessage, "Message should be routed to DLQ after exhausting retries");
    }
    @Test
    public void testPermanent4xxErrorRoutesToDlqImmediately() throws Exception {
        // Mock permanent 4xx error
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid API Key"));

        String jobId = UUID.randomUUID().toString();
        EmailTaskMessage msg = new EmailTaskMessage(jobId, testOrder.getId(), testUser.getId(), "test@ticketbox.com", List.of(testTicket.getId()), "corr-4");

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_EMAIL, msg);

        // Wait only a short time since no retries should happen (1s should be enough)
        Thread.sleep(3000);

        Order updated = orderRepository.findById(testOrder.getId()).orElseThrow();
        assertEquals(EmailStatus.FAILED, updated.getEmailStatus()); // Should be marked FAILED immediately

        // Check DLQ
        Message dlqMessage = rabbitTemplate.receive(RabbitMQConfig.QUEUE_EMAIL_DLQ, 5000);
        assertNotNull(dlqMessage, "Message should be routed to DLQ immediately without retries");
    }

    @Test
    public void testDuplicateJobIgnored() throws Exception {
        // Set DB status to SENT directly
        testOrder.setEmailStatus(EmailStatus.SENT);
        orderRepository.save(testOrder);

        String jobId = UUID.randomUUID().toString();
        EmailTaskMessage msg = new EmailTaskMessage(jobId, testOrder.getId(), testUser.getId(), "test@ticketbox.com", List.of(testTicket.getId()), "corr-3");

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_COMMANDS, RabbitMQConfig.ROUTING_KEY_EMAIL, msg);

        Thread.sleep(2000);

        EmailJobTracker.EmailJobProgress progress = jobTracker.getProgress(jobId);
        assertNotNull(progress);
        assertEquals("COMPLETED", progress.getStatus());
        assertEquals("Already sent", progress.getErrorReason());
    }
}
