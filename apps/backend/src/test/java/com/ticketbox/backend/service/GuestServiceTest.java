package com.ticketbox.backend.service;

import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.entity.Guest;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.repository.GuestRepository;
import com.ticketbox.backend.repository.TicketCategoryRepository;
import com.ticketbox.backend.repository.TicketRepository;
import com.ticketbox.backend.worker.csv.CsvRowValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GuestServiceTest {

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private ConcertRepository concertRepository;

    @Mock
    private TicketCategoryRepository ticketCategoryRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CsvRowValidator csvRowValidator;

    @InjectMocks
    private GuestService guestService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testImportGuestsFromCsv_skipsInvalidRows() throws Exception {
        // Arrange
        String csvData = "Name,Email,Phone,ConcertName,Sponsor,Status\n" +
                         "Valid User,valid@example.com,,Test Concert,,\n" +
                         "Invalid Email,invalid-email,,Test Concert,,\n" +
                         "Missing Email,,,Test Concert,,";
        
        MockMultipartFile file = new MockMultipartFile("file", "guests.csv", "text/csv", csvData.getBytes(StandardCharsets.UTF_8));
        
        Concert concert = new Concert();
        concert.setId(1L);
        concert.setName("Test Concert");
        
        when(concertRepository.findByName(any())).thenReturn(Optional.of(concert));
        
        when(csvRowValidator.isValid("Valid User", "valid@example.com")).thenReturn(true);
        when(csvRowValidator.isValid("Invalid Email", "invalid-email")).thenReturn(false);
        when(csvRowValidator.isValid("Missing Email", "")).thenReturn(false);
        
        Guest savedGuest = new Guest();
        savedGuest.setId(10L);
        savedGuest.setEmail("valid@example.com");
        when(guestRepository.save(any())).thenReturn(savedGuest);
        
        TicketCategory category = new TicketCategory();
        category.setName("GUEST");
        when(ticketCategoryRepository.findByConcertId(any())).thenReturn(Collections.singletonList(category));
        
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        when(ticketRepository.save(any())).thenReturn(ticket);

        // Act
        int importedCount = guestService.importGuestsFromCsv(file);

        // Assert
        assertEquals(1, importedCount);
        verify(guestRepository, times(1)).save(any(Guest.class));
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
