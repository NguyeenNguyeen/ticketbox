package com.ticketbox.backend.controller;

import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.repository.TicketCategoryRepository;
import com.ticketbox.backend.service.ConcertService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ConcertController {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    // ──────────────────────────────────────────────────────────────
    // Public endpoints
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/concerts")
    public ResponseEntity<List<ConcertListItemDto>> getAllConcerts() {
        List<Concert> concerts = concertService.getAllConcerts();
        List<ConcertListItemDto> dtos = concerts.stream().map(c -> {
            ConcertListItemDto dto = new ConcertListItemDto();
            dto.setId(c.getId().toString());
            dto.setTitle(c.getName());
            dto.setVenue(c.getLocation());
            dto.setDate(c.getStartTime() != null ? c.getStartTime().toString() : "");
            dto.setBannerUrl(resolveBannerUrl(c.getName()));
            dto.setStatus(c.getEffectiveStatus());

            List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(c.getId());
            BigDecimal minPrice = categories.stream()
                    .map(TicketCategory::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            dto.setPriceFrom(minPrice);

            if (c.getArtists() != null) {
                dto.setArtists(c.getArtists().stream()
                        .map(com.ticketbox.backend.entity.Artist::getName)
                        .collect(Collectors.toList()));
            } else {
                dto.setArtists(java.util.List.of());
            }

            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/concerts/{id}")
    public ResponseEntity<ConcertDetailDto> getConcertById(@PathVariable Long id) {
        Concert c = concertService.getConcertById(id);

        ConcertDetailDto dto = new ConcertDetailDto();
        dto.setId(c.getId().toString());
        dto.setTitle(c.getName());
        dto.setDescription(c.getDescription());

        List<ArtistDto> artistDtos = java.util.List.of();
        if (c.getArtists() != null) {
            artistDtos = c.getArtists().stream().map(a -> {
                ArtistDto ad = new ArtistDto();
                ad.setId(a.getId().toString());
                ad.setName(a.getName());
                ad.setAvatarUrl(a.getAvatarUrl());
                ad.setBio(a.getBio());
                return ad;
            }).collect(Collectors.toList());
        }

        dto.setArtists(artistDtos);
        dto.setVenue(c.getLocation());
        dto.setAddress(c.getLocation());
        dto.setDate(c.getStartTime() != null ? c.getStartTime().toString() : "");
        dto.setDoors("18:00");
        dto.setShowTime("19:30");
        dto.setBannerUrl(resolveBannerUrl(c.getName()));
        dto.setStatus(c.getEffectiveStatus());

        List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(c.getId());
        List<TicketCategoryDto> categoryDtos = categories.stream().map(cat -> {
            TicketCategoryDto catDto = new TicketCategoryDto();
            catDto.setId(cat.getId());
            catDto.setName(cat.getName());
            catDto.setPrice(cat.getPrice());
            catDto.setTotalQuantity(cat.getTotalQuantity());
            catDto.setAvailableQuantity(cat.getAvailableQuantity());
            catDto.setMaxPerUser(getMaxPerUser(cat.getName()));
            // Use concert's saleStartTime if set; otherwise 7 days before show (null-safe)
            LocalDateTime saleStart = c.getSaleStartTime();
            if (saleStart == null) {
                saleStart = c.getStartTime() != null ? c.getStartTime().minusDays(7) : LocalDateTime.now();
            }
            catDto.setSaleStartTime(saleStart.toString());
            catDto.setColor(getZoneColor(cat.getName()));
            return catDto;
        }).collect(Collectors.toList());

        dto.setTicketCategories(categoryDtos);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/concerts/{id}/categories")
    public ResponseEntity<List<TicketCategoryDto>> getConcertCategories(@PathVariable Long id) {
        Concert c = concertService.getConcertById(id);
        List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(id);
        List<TicketCategoryDto> dtos = categories.stream().map(cat -> {
            TicketCategoryDto dto = new TicketCategoryDto();
            dto.setId(cat.getId());
            dto.setName(cat.getName());
            dto.setPrice(cat.getPrice());
            dto.setTotalQuantity(cat.getTotalQuantity());
            dto.setAvailableQuantity(cat.getAvailableQuantity());
            dto.setMaxPerUser(getMaxPerUser(cat.getName()));
            LocalDateTime saleStart = c.getSaleStartTime();
            if (saleStart == null) {
                saleStart = c.getStartTime() != null ? c.getStartTime().minusDays(7) : LocalDateTime.now();
            }
            dto.setSaleStartTime(saleStart.toString());
            dto.setColor(getZoneColor(cat.getName()));
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ──────────────────────────────────────────────────────────────
    // Admin endpoints (ORGANIZER only — enforced by SecurityConfig)
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/admin/concerts")
    public ResponseEntity<ConcertDetailDto> createConcert(@RequestBody ConcertCreateRequest req) {
        Concert concert = Concert.builder()
                .name(req.getTitle())
                .description(req.getDescription())
                .location(req.getVenue())
                .startTime(LocalDateTime.parse(req.getDate() + "T" + (req.getShowTime() != null ? req.getShowTime() : "19:00") + ":00"))
                .endTime(LocalDateTime.parse(req.getDate() + "T23:59:00"))
                .saleStartTime(req.getSaleStartTime() != null
                        ? LocalDateTime.parse(req.getSaleStartTime() + "T00:00:00")
                        : null)
                .build();

        Concert saved = concertService.createConcert(concert);

        // Create ticket categories
        if (req.getTicketCategories() != null) {
            for (ConcertCreateRequest.TicketCategoryReq catReq : req.getTicketCategories()) {
                TicketCategory cat = new TicketCategory();
                cat.setConcert(saved);
                cat.setName(catReq.getName());
                cat.setPrice(catReq.getPrice());
                cat.setTotalQuantity(catReq.getTotalQuantity());
                cat.setAvailableQuantity(catReq.getTotalQuantity());
                cat.setVersion(0L);
                ticketCategoryRepository.save(cat);
            }
        }

        return getConcertById(saved.getId());
    }

    @PutMapping("/admin/concerts/{id}")
    public ResponseEntity<ConcertDetailDto> updateConcert(
            @PathVariable Long id,
            @RequestBody ConcertCreateRequest req) {

        Concert updated = Concert.builder()
                .name(req.getTitle())
                .description(req.getDescription())
                .location(req.getVenue())
                .startTime(LocalDateTime.parse(req.getDate() + "T" + (req.getShowTime() != null ? req.getShowTime() : "19:00") + ":00"))
                .endTime(LocalDateTime.parse(req.getDate() + "T23:59:00"))
                .saleStartTime(req.getSaleStartTime() != null
                        ? LocalDateTime.parse(req.getSaleStartTime() + "T00:00:00")
                        : null)
                .build();

        concertService.updateConcert(id, updated);

        // Update ticket categories: update existing ones by name match, create new ones
        if (req.getTicketCategories() != null) {
            List<TicketCategory> existing = ticketCategoryRepository.findByConcertId(id);
            for (ConcertCreateRequest.TicketCategoryReq catReq : req.getTicketCategories()) {
                TicketCategory match = existing.stream()
                        .filter(e -> e.getName().equalsIgnoreCase(catReq.getName()))
                        .findFirst()
                        .orElse(null);
                if (match != null) {
                    match.setPrice(catReq.getPrice());
                    match.setTotalQuantity(catReq.getTotalQuantity());
                    ticketCategoryRepository.save(match);
                } else {
                    Concert ref = concertService.getConcertById(id);
                    TicketCategory cat = new TicketCategory();
                    cat.setConcert(ref);
                    cat.setName(catReq.getName());
                    cat.setPrice(catReq.getPrice());
                    cat.setTotalQuantity(catReq.getTotalQuantity());
                    cat.setAvailableQuantity(catReq.getTotalQuantity());
                    cat.setVersion(0L);
                    ticketCategoryRepository.save(cat);
                }
            }
        }

        return getConcertById(id);
    }

    @DeleteMapping("/admin/concerts/{id}")
    public ResponseEntity<Void> cancelConcert(@PathVariable Long id) {
        concertService.cancelConcert(id);
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private String resolveBannerUrl(String concertName) {
        if (concertName.contains("Anh Trai Say Hi")) return "/concert-anh-trai-say-hi.png";
        if (concertName.contains("Anh Trai Vượt Ngàn")) return "/concert-anh-trai-vuot-ngan.png";
        if (concertName.contains("Em Xinh")) return "/concert-em-xinh-say-hi.png";
        if (concertName.contains("Chị Đẹp")) return "/concert-chi-dep-dap-gio.png";
        return "/concert-anh-trai-say-hi.png";
    }

    private String getZoneColor(String categoryName) {
        switch (categoryName.toUpperCase()) {
            case "SVIP": return "#F59E0B";
            case "VIP": return "#7C3AED";
            case "CAT1": return "#3B82F6";
            case "CAT2": return "#10B981";
            case "GA": default: return "#9CA3AF";
        }
    }

    private int getMaxPerUser(String categoryName) {
        String upper = categoryName.toUpperCase();
        if (upper.contains("SVIP")) return 2;
        if (upper.contains("VIP")) return 2;
        return 4;
    }

    // ──────────────────────────────────────────────────────────────
    // DTOs
    // ──────────────────────────────────────────────────────────────

    @Data
    static class ConcertListItemDto {
        private String id;
        private String title;
        private String venue;
        private String date;
        private String bannerUrl;
        private String status;
        private BigDecimal priceFrom;
        private List<String> artists;
    }

    @Data
    static class ConcertDetailDto {
        private String id;
        private String title;
        private String description;
        private List<ArtistDto> artists;
        private String venue;
        private String address;
        private String date;
        private String doors;
        private String showTime;
        private String bannerUrl;
        private String status;
        private List<TicketCategoryDto> ticketCategories;
    }

    @Data
    static class ArtistDto {
        private String id;
        private String name;
        private String avatarUrl;
        private String bio;
    }

    @Data
    static class TicketCategoryDto {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer totalQuantity;
        private Integer availableQuantity;
        private Integer maxPerUser;
        private String saleStartTime;
        private String color;
    }

    @Data
    static class ConcertCreateRequest {
        private String title;
        private String description;
        private String venue;
        private String address;
        private String date;       // "2026-12-20"
        private String doors;      // "18:00"
        private String showTime;   // "19:30"
        private String saleStartTime; // "2026-12-01"
        private List<TicketCategoryReq> ticketCategories;

        @Data
        static class TicketCategoryReq {
            private String name;
            private BigDecimal price;
            private Integer totalQuantity;
            private Integer maxPerUser;
            private String saleStartTime;
        }
    }
}
