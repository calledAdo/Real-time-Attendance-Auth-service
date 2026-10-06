package com.genius.controller;

import com.genius.dto.BulkEnrollmentRequest;
import com.genius.dto.CourseRequest;
import com.genius.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> createCourse(@RequestBody CourseRequest request, Principal principal) {
        try {
            courseService.createCourse(request, principal.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Course created successfully. You are set as the primary lecturer."
            ));
        } catch (Exception e) {
            // Catches duplicates or validation errors with the custom message
            int status = e.getMessage().contains("already exists") ? 409 : 400;
            return ResponseEntity.status(status).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{courseCode}/assign-lecturer")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> assignSupportingLecturer(
            @PathVariable String courseCode,
            @RequestParam String lecturerEmail,
            Principal principal) {
        try {
            courseService.assignSupportingLecturer(courseCode, lecturerEmail, principal.getName());
            return ResponseEntity.ok(Map.of("success", true, "message", "Successfully added co-teacher."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{courseCode}/bulk-enroll")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<?> bulkEnrollStudents(
            @PathVariable String courseCode,
            @RequestBody BulkEnrollmentRequest request,
            Principal principal) {
        try {
            Map<String, Object> result = courseService.bulkEnrollStudents(courseCode, request.getMatricNumbers(), principal.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}