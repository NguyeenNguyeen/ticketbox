package com.ticketbox.backend.service;

import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.repository.ConcertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConcertService {

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private com.ticketbox.backend.repository.TicketCategoryRepository ticketCategoryRepository;

    private Concert cloneConcert(Concert c) {
        return Concert.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .startTime(c.getStartTime())
                .endTime(c.getEndTime())
                .location(c.getLocation())
                .address(c.getAddress())
                .doorsTime(c.getDoorsTime())
                .saleStartTime(c.getSaleStartTime())
                .forcedStatus(c.getForcedStatus())
                .cancelledStatus(c.getCancelledStatus())
                .artists(c.getArtists() != null ? new java.util.HashSet<>(c.getArtists()) : new java.util.HashSet<>())
                .build();
    }

    @Cacheable(value = "concertsV7", key = "#id")
    public Concert getConcertById(Long id) {
        Concert c = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        return cloneConcert(c);
    }

    @Cacheable(value = "concertsListV7")
    public List<Concert> getAllConcerts() {
        List<Concert> list = concertRepository.findAll();
        return list.stream().map(this::cloneConcert).collect(java.util.stream.Collectors.toList());
    }

    @CacheEvict(value = {"concertsV7", "concertsListV7"}, allEntries = true)
    public Concert createConcert(Concert concert) {
        return cloneConcert(concertRepository.save(concert));
    }

    @CacheEvict(value = {"concertsV7", "concertsListV7"}, allEntries = true)
    public Concert updateConcert(Long id, Concert updated) {
        Concert existing = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());
        existing.setLocation(updated.getLocation());
        existing.setAddress(updated.getAddress());
        existing.setDoorsTime(updated.getDoorsTime());
        existing.setForcedStatus(updated.getForcedStatus());
        if (updated.getSaleStartTime() != null) {
            existing.setSaleStartTime(updated.getSaleStartTime());
        }
        if (updated.getArtists() != null) {
            if (existing.getArtists() == null) {
                existing.setArtists(new java.util.HashSet<>());
            }
            existing.getArtists().clear();
            existing.getArtists().addAll(updated.getArtists());
        }
        return cloneConcert(concertRepository.save(existing));
    }

    @CacheEvict(value = {"concertsV7", "concertsListV7"}, allEntries = true)
    public void cancelConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        concert.setCancelledStatus("CANCELLED");
        concertRepository.save(concert);
    }

    @CacheEvict(value = {"concertsV7", "concertsListV7"}, allEntries = true)
    public void resumeConcert(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
        concert.setCancelledStatus(null);
        concertRepository.save(concert);
    }

    @CacheEvict(value = {"concertsV7", "concertsListV7"}, allEntries = true)
    public void deleteConcert(Long id) {
        java.util.List<com.ticketbox.backend.entity.TicketCategory> categories = ticketCategoryRepository.findByConcertId(id);
        ticketCategoryRepository.deleteAll(categories);
        concertRepository.deleteById(id);
    }
}
