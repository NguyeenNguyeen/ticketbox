package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
    java.util.Optional<Concert> findByName(String name);
    java.util.Optional<Concert> findFirstByNameContainingIgnoreCase(String name);
}
