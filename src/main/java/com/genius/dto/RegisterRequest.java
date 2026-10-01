package com.genius.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String matricNo;       // Required if student
    private boolean isLecturer;    // False = Student, True = Lecturer
    private Long departmentId;     // Department ID
}