package com.genius.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "course_rosters", indexes = {
        @Index(name = "idx_course_matric", columnList = "courseCode, matricNo")
})
@Data
public class CourseRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String matricNo;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean confirmed = false; // False during staging upload, true after lecturer confirms
}