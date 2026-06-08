package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.OrderStatus;
import com.ticketbox.backend.entity.Ticket;
import com.ticketbox.backend.entity.TicketCategory;
import com.ticketbox.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderId(Long orderId);


    /**
     * Counts tickets already purchased by a user for a given category,
     * counting only tickets from COMPLETED (successfully paid) orders.
     * This prevents users from bypassing the per-account limit by splitting into multiple small orders.
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.owner = :owner AND t.category = :category AND t.order.status = :status")
    int countByOwnerAndCategoryAndOrderStatus(
            @Param("owner") User owner,
            @Param("category") TicketCategory category,
            @Param("status") OrderStatus status
    );

}
