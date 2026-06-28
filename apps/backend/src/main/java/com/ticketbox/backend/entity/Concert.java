package com.ticketbox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "concerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concert implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private String location;

    private String address;

    @Column(name = "doors_time")
    private String doorsTime;

    /** Optional: explicit sale start date. If null, defaults to 7 days before startTime. */
    private LocalDateTime saleStartTime;

    /** Nullable: set to "CANCELLED" to manually cancel. Otherwise computed from time. */
    private String cancelledStatus;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
        name = "concert_artists",
        joinColumns = @JoinColumn(name = "concert_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    @Builder.Default
    @ToString.Exclude
    private java.util.Set<Artist> artists = new java.util.HashSet<>();

    /**
     * Computes effective status at call time.
     * Priority: explicit CANCELLED override → time-based logic.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getEffectiveStatus() {
        if ("CANCELLED".equals(cancelledStatus)) return "CANCELLED";
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime saleStart = saleStartTime != null ? saleStartTime : startTime.minusDays(7);
        if (now.isAfter(endTime)) return "ENDED";
        if (now.isAfter(saleStart)) return "ON_SALE";
        return "UPCOMING";
    }
}
