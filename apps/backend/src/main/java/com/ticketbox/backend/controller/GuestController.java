package com.ticketbox.backend.controller;

import com.ticketbox.backend.dto.GuestDto;
import com.ticketbox.backend.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping
    public ResponseEntity<List<GuestDto>> getGuests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String concertName) {
        return ResponseEntity.ok(guestService.getAllGuests(search, concertName));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importGuests(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        try {
            int count = guestService.importGuestsFromCsv(file);
            return ResponseEntity.ok(Map.of("message", "Đã import thành công " + count + " khách mời"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGuest(@PathVariable Long id) {
        try {
            guestService.deleteGuest(id);
            return ResponseEntity.ok(Map.of("message", "Xoá khách mời thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
