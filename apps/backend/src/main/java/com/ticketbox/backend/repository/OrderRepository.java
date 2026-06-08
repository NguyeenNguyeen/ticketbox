package com.ticketbox.backend.repository;

import com.ticketbox.backend.entity.Order;
import com.ticketbox.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
    
    List<Order> findByUserOrderByCreatedAtDesc(com.ticketbox.backend.entity.User user);
    
    List<Order> findAllByOrderByCreatedAtDesc();
}
