package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.TicketCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
    
    // Pessimistic Write Lock to prevent oversell during concurrent purchases
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TicketCategory tc WHERE tc.id = :id")
    Optional<TicketCategory> findByIdWithPessimisticLock(@Param("id") Long id);
}
