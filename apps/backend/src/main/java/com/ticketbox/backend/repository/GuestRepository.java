package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    
    boolean existsByConcertIdAndEmail(Long concertId, String email);

    java.util.Optional<Guest> findByConcertIdAndEmail(Long concertId, String email);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Guest g JOIN FETCH g.concert WHERE " +
            "(:concertName = '' OR :concertName = 'all' OR g.concert.name = :concertName) AND " +
            "(:search = '' OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(g.sponsor) LIKE LOWER(CONCAT('%', :search, '%')))")
    java.util.List<Guest> searchGuests(@org.springframework.data.repository.query.Param("concertName") String concertName,
                                       @org.springframework.data.repository.query.Param("search") String search);
}
