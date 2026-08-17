package com.course.microservices.songservice.controller;

import com.course.microservices.songservice.dto.SongDto;
import com.course.microservices.songservice.service.SongService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
@Validated
public class SongController {

    private final SongService songService;

    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getSongById(@PathVariable @Min(value = 1, message = "Invalid value '${validatedValue}' for ID. Must be a positive integer") Long id) {
        return ResponseEntity.ok(songService.getSongById(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> createSong(@Valid @RequestBody SongDto songDto) {
        return ResponseEntity.ok(Map.of("id", songService.createSong(songDto)));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Long>>> deleteSongs(
            HttpServletRequest request,
            @RequestParam(name = "id") List<@Valid @Min(value = 1, message = "Invalid ID format: '${validatedValue}'. Only positive integers are allowed") Long> id) {
        List<Long> deletedIds = songService.deleteSongs(id, request.getParameter("id"));
        return ResponseEntity.ok(Map.of("ids", deletedIds));
    }
}
