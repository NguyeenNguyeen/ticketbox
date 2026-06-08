package com.ticketbox.backend.controller;

import com.ticketbox.backend.service.AdminStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Autowired
    private AdminStatsService statsService;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<AdminStatsService.AdminStatsDto> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }
}
