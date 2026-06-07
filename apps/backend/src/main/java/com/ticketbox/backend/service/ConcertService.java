package com.ticketbox.backend.service;

import com.ticketbox.backend.entity.Concert;
import com.ticketbox.backend.repository.ConcertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConcertService {

    @Autowired
    private ConcertRepository concertRepository;

    @Cacheable(value = "concerts", key = "#id")
    public Concert getConcertById(Long id) {
        return concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concert not found"));
    }

    @Cacheable(value = "concertsList")
    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }

    @CacheEvict(value = {"concerts", "concertsList"}, allEntries = true)
    public Concert createConcert(Concert concert) {
        return concertRepository.save(concert);
    }
}
