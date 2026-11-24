package com.course.microservices.storageservice.controller;

import com.course.microservices.storageservice.dto.StorageDto;
import com.course.microservices.storageservice.dto.StorageRequest;
import com.course.microservices.storageservice.service.StorageService;
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
@RequestMapping("/storages")
@RequiredArgsConstructor
@Validated
public class StorageController {

    private final StorageService storageService;

    @GetMapping
    public ResponseEntity<List<StorageDto>> getStorages() {
        return ResponseEntity.ok(storageService.getStorages());
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> createStorage(@Valid @RequestBody StorageRequest request) {
        return ResponseEntity.ok(Map.of("id", storageService.createStorage(request)));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Long>>> deleteStorages(
            HttpServletRequest request,
            @RequestParam(name = "id") List<@Min(value = 1, message = "Invalid ID format: '${validatedValue}'. Only positive integers are allowed") Long> id) {
        return ResponseEntity.ok(Map.of("ids", storageService.deleteStorages(id, request.getParameter("id"))));
    }
}
