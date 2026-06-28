package com.ticketbox.backend.service;

import com.ticketbox.backend.dto.GuestDto;
import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.entity.Guest;
import com.ticketbox.backend.entity.GuestStatus;
import com.ticketbox.backend.repository.ConcertRepository;
import com.ticketbox.backend.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestService {

    private final GuestRepository guestRepository;
    private final ConcertRepository concertRepository;

    @Transactional(readOnly = true)
    public List<GuestDto> getAllGuests(String search, String concertName) {
        String querySearch = (search == null || search.trim().isEmpty()) ? "" : search.trim();
        String queryConcert = (concertName == null || concertName.trim().isEmpty() || "all".equalsIgnoreCase(concertName)) ? "" : concertName.trim();
        
        return guestRepository.searchGuests(queryConcert, querySearch).stream().map(g -> GuestDto.builder()
                .id("g" + g.getId())
                .name(g.getFullName())
                .email(g.getEmail())
                .phone(g.getPhone() != null ? g.getPhone() : "")
                .sponsor(g.getSponsor() != null ? g.getSponsor() : "")
                .concert(g.getConcert() != null ? g.getConcert().getName() : "")
                .status(g.getStatus() != null ? g.getStatus().name() : "PENDING")
                .importedAt(g.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void deleteGuest(Long guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new IllegalArgumentException("Guest not found");
        }
        guestRepository.deleteById(guestId);
    }

    @Transactional
    public void confirmGuest(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest not found"));
        guest.setStatus(GuestStatus.CONFIRMED);
        guestRepository.save(guest);
    }

    @Transactional
    public int importGuestsFromCsv(MultipartFile file) {
        int importedCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Read first line to detect delimiter
            reader.mark(1024);
            String firstLine = reader.readLine();
            reader.reset();
            char delimiter = (firstLine != null && firstLine.contains(";")) ? ';' : ',';

            CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .build();
                
            CSVParser csvParser = new CSVParser(reader, format);
            
            for (CSVRecord record : csvParser) {
                if (record.size() < 2) {
                    log.warn("Skipping line due to insufficient columns: {}", record);
                    continue;
                }
                
                String name = record.get(0);
                String email = record.get(1);
                String phone = record.size() > 2 ? record.get(2) : "";
                String concertName = record.size() > 3 ? record.get(3) : "";
                String sponsor = record.size() > 4 ? record.get(4) : "";
                String statusStr = record.size() > 5 ? record.get(5) : "PENDING";
                
                if (name.isEmpty() || email.isEmpty()) {
                    log.warn("Skipping line due to empty name or email");
                    continue;
                }
                
                Concert concert = null;
                if (!concertName.isEmpty()) {
                    concert = concertRepository.findByName(concertName)
                            .orElseGet(() -> concertRepository.findFirstByNameContainingIgnoreCase(concertName).orElse(null));
                }
                if (concert == null) {
                    log.warn("Skipping line: No concert found matching '{}'", concertName);
                    continue;
                }
                
                GuestStatus status = parseStatus(statusStr);
                
                Optional<Guest> existingGuestOpt = guestRepository.findByConcertIdAndEmail(concert.getId(), email);
                if (existingGuestOpt.isPresent()) {
                    Guest existing = existingGuestOpt.get();
                    existing.setFullName(name);
                    existing.setPhone(phone);
                    existing.setSponsor(sponsor);
                    existing.setStatus(status);
                    guestRepository.save(existing);
                } else {
                    Guest newGuest = Guest.builder()
                        .concert(concert)
                        .email(email)
                        .fullName(name)
                        .phone(phone)
                        .sponsor(sponsor)
                        .status(status)
                        .build();
                    guestRepository.save(newGuest);
                }
                importedCount++;
            }
            log.info("Successfully imported {} guests", importedCount);
            return importedCount;
        } catch (Exception e) {
            log.error("Failed to parse CSV file", e);
            throw new RuntimeException("Failed to import CSV: " + e.getMessage());
        }
    }
    
    private GuestStatus parseStatus(String val) {
        if (val == null) return GuestStatus.PENDING;
        String upper = val.toUpperCase().trim();
        if (upper.contains("XÁC NHẬN") || upper.contains("CONFIRMED")) return GuestStatus.CONFIRMED;
        if (upper.contains("CHECK") || upper.contains("CHECKED_IN")) return GuestStatus.CHECKED_IN;
        return GuestStatus.PENDING;
    }
}
