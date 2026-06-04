package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    
    boolean existsByConcertIdAndEmail(Long concertId, String email);
}
