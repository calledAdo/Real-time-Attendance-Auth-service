package com.genius.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "student_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User student;

    @Column(nullable = false)
    private LocalDateTime checkInTime = LocalDateTime.now();

    @Column(nullable = false)
    private boolean faceVerified = false; // Biometric check flag

    // Student's GPS coordinates at check-in time
    private double studentLatitude;
    private double studentLongitude;
}