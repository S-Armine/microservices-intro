package com.course.microservices.resourceservice.controller;

import com.course.microservices.resourceservice.service.ResourceService;
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
@RequestMapping("resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping(value = "/{id}", produces = "audio/mpeg")
    public ResponseEntity<byte[]> getResources(@PathVariable @Min(value = 1, message = "Invalid value '${validatedValue}' for ID. Must be a positive integer") Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, List<Long>>> deleteResources(
            HttpServletRequest request,
            @RequestParam(name = "id") List<@Valid @Min(value = 1, message = "Invalid ID format: '${validatedValue}'. Only positive integers are allowed") Long> id) {
        List<Long> deletedIds = resourceService.deleteResources(id, request.getParameter("id"));
        return ResponseEntity.ok(Map.of("ids", deletedIds));
    }

    @PostMapping(consumes = "audio/mpeg")
    public ResponseEntity<Map<String, Long>> uploadResource(@RequestBody byte[] audioData) {
        Long resourceId = resourceService.uploadResource(audioData);
        return ResponseEntity.ok(Map.of("id", resourceId));
    }
}


