package com.course.microservices.storageservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "storages", uniqueConstraints = @UniqueConstraint(columnNames = {"bucket", "path"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Storage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 32)
    private StorageType storageType;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false, length = 1024)
    private String path;
}
