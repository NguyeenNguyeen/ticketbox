package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.OrderItem;
import com.ticketbox.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Returns the total quantity of tickets a user has already purchased (COMPLETED orders)
     * for a specific ticket category — used to enforce per-user limits.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           "AND oi.ticketCategory.id = :categoryId " +
           "AND oi.order.status = :status")
    int sumQuantityByUserAndCategoryAndStatus(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("status") OrderStatus status);
}
