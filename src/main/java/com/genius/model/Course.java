package com.genius.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String courseCode; // e.g., CPE310

    @Column(nullable = false)
    private String courseTitle; // e.g., Computer Architecture

    // Multiple lecturers can teach this course
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "course_lecturers",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> lecturers = new HashSet<>();
}