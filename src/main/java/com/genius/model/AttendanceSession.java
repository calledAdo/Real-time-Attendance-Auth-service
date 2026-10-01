package com.genius.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "attendance_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User lecturer;

    @Column(nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime expiryTime;

    @Column(nullable = false)
    private boolean isActive = true;

    // Lecturer's GPS coordinates at session start
    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double radiusMeters = 50.0; // Allowed geofence radius

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<AttendanceRecord> records = new HashSet<>();
}