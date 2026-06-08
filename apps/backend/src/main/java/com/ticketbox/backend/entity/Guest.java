package com.ticketbox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "guests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_guest_concert_email", columnNames = {"concert_id", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guest implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;
    
    private String phone;
    
    private String sponsor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private GuestStatus status = GuestStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
