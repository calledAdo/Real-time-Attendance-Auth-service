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
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email; // Enforced to end with .edu.ng for students

    @Column(nullable = false)
    private String password;

    // Student specific field
    @Column(unique = true)
    private String matricNo;

    // Role flag: False = Student, True = Lecturer
    @Column(nullable = false)
    private boolean isLecturer = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    // Courses taken by a Student
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Course> courses = new HashSet<>();

    // Courses taught by a Lecturer
    @ManyToMany(mappedBy = "lecturers", fetch = FetchType.EAGER)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Course> teachingCourses = new HashSet<>();
}