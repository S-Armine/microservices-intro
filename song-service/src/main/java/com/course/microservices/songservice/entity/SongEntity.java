package com.course.microservices.songservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "songs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongEntity {
    @Id
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "artist", nullable = false)
    private String artist;
    @Column(name = "album", nullable = false)
    private String album;
    @Column(name = "duration", nullable = false)
    private String duration;
    @Column(name = "year", nullable = false)
    private String year;
}
