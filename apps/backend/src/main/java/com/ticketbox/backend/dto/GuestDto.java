package com.ticketbox.backend.dto;

import com.ticketbox.backend.entity.GuestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestDto {
    private String id;
    private String concert;
    private String email;
    private String name;
    private String phone;
    private String sponsor;
    private String status;
    private LocalDateTime importedAt;
}
