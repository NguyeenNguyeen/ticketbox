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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/concerts")
public class ConcertController {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private TicketCategoryRepository ticketCategoryRepository;

    @GetMapping
    public ResponseEntity<List<ConcertListItemDto>> getAllConcerts() {
        List<Concert> concerts = concertService.getAllConcerts();
        List<ConcertListItemDto> dtos = concerts.stream().map(c -> {
            ConcertListItemDto dto = new ConcertListItemDto();
            dto.setId(c.getId().toString());
            dto.setTitle(c.getName());
            dto.setVenue(c.getLocation());
            dto.setDate(c.getStartTime() != null ? c.getStartTime().toString() : "");
            // Map the sample concert names to their banner URLs
            if (c.getName().contains("Anh Trai Say Hi")) dto.setBannerUrl("/concert-anh-trai-say-hi.png");
            else if (c.getName().contains("Anh Trai Vượt Ngàn")) dto.setBannerUrl("/concert-anh-trai-vuot-ngan.png");
            else if (c.getName().contains("Em Xinh")) dto.setBannerUrl("/concert-em-xinh-say-hi.png");
            else if (c.getName().contains("Chị Đẹp")) dto.setBannerUrl("/concert-chi-dep-dap-gio.png");
            else dto.setBannerUrl("/concert-anh-trai-say-hi.png");

            dto.setStatus("ON_SALE");
            
            // Get price from
            List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(c.getId());
            BigDecimal minPrice = categories.stream()
                    .map(TicketCategory::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            dto.setPriceFrom(minPrice);
            
            if (c.getArtistBiography() != null) {
                dto.setArtists(Arrays.asList(c.getArtistBiography().split(",\\s*")));
            } else {
                dto.setArtists(List.of());
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
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
        if (categoryName.toUpperCase().contains("SVIP") || categoryName.toUpperCase().contains("VIP")) {
            return 2;
        }
        return 4;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConcertDetailDto> getConcertById(@PathVariable Long id) {
        Concert c = concertService.getConcertById(id);
        
        ConcertDetailDto dto = new ConcertDetailDto();
        dto.setId(c.getId().toString());
        dto.setTitle(c.getName());
        dto.setDescription(c.getDescription());
        dto.setArtistBio(c.getArtistBiography() != null ? c.getArtistBiography() : "");
        
        if (c.getArtistBiography() != null) {
            dto.setArtists(Arrays.asList(c.getArtistBiography().split(",\\s*")));
        } else {
            dto.setArtists(List.of());
        }
        
        dto.setVenue(c.getLocation());
        dto.setAddress(c.getLocation());
        dto.setDate(c.getStartTime() != null ? c.getStartTime().toString() : "");
        dto.setDoors("18:00");
        dto.setShowTime("19:30");
        
        if (c.getName().contains("Anh Trai Say Hi")) dto.setBannerUrl("/concert-anh-trai-say-hi.png");
        else if (c.getName().contains("Anh Trai Vượt Ngàn")) dto.setBannerUrl("/concert-anh-trai-vuot-ngan.png");
        else if (c.getName().contains("Em Xinh")) dto.setBannerUrl("/concert-em-xinh-say-hi.png");
        else if (c.getName().contains("Chị Đẹp")) dto.setBannerUrl("/concert-chi-dep-dap-gio.png");
        else dto.setBannerUrl("/concert-anh-trai-say-hi.png");
        
        dto.setStatus("ON_SALE");
        
        List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(c.getId());
        List<TicketCategoryDto> categoryDtos = categories.stream().map(cat -> {
            TicketCategoryDto catDto = new TicketCategoryDto();
            catDto.setId(cat.getId());
            catDto.setName(cat.getName());
            catDto.setPrice(cat.getPrice());
            catDto.setTotalQuantity(cat.getTotalQuantity());
            catDto.setAvailableQuantity(cat.getAvailableQuantity());
            catDto.setMaxPerUser(getMaxPerUser(cat.getName()));
            catDto.setSaleStartTime("2026-06-01T00:00:00");
            catDto.setColor(getZoneColor(cat.getName()));
            return catDto;
        }).collect(Collectors.toList());
        
        dto.setTicketCategories(categoryDtos);
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/categories")
    public ResponseEntity<List<TicketCategoryDto>> getConcertCategories(@PathVariable Long id) {
        List<TicketCategory> categories = ticketCategoryRepository.findByConcertId(id);
        List<TicketCategoryDto> dtos = categories.stream().map(cat -> {
            TicketCategoryDto dto = new TicketCategoryDto();
            dto.setId(cat.getId());
            dto.setName(cat.getName());
            dto.setPrice(cat.getPrice());
            dto.setTotalQuantity(cat.getTotalQuantity());
            dto.setAvailableQuantity(cat.getAvailableQuantity());
            dto.setMaxPerUser(getMaxPerUser(cat.getName()));
            dto.setSaleStartTime("2026-06-01T00:00:00");
            dto.setColor(getZoneColor(cat.getName()));
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

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
        private String artistBio;
        private List<String> artists;
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
}
