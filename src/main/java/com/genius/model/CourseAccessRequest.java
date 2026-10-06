package com.genius.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_access_requests")
@Data
public class CourseAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String matricNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime requestedAt = LocalDateTime.now();

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}