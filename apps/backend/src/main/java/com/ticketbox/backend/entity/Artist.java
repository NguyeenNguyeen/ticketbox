package com.ticketbox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "artists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artist implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ManyToMany(mappedBy = "artists")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    private Set<Concert> concerts = new HashSet<>();
}
