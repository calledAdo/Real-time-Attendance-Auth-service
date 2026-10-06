package com.genius.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String courseCode; // e.g. CSC301

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String semester; // e.g., "First Semester 2026/2027"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(nullable = false)
    private Long lecturerId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CourseStatus {
        DRAFT, ACTIVE
    }
}