package com.course.microservices.resourceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongMetadataDto {
    private Long id;
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;
}
