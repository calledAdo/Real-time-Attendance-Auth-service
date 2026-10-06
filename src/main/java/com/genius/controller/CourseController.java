package com.genius.controller;

import com.genius.model.Course;
import com.genius.model.CourseAccessRequest;
import com.genius.model.User;
import com.genius.service.AuthService;
import com.genius.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            String courseCode = (String) payload.get("courseCode");
            String title = (String) payload.get("title");
            String semester = (String) payload.get("semester");

            // Securely derive lecturer from JWT Principal instead of payload
            User lecturer = authService.getUserByEmailOrUsername(principal.getName());

            Course course = courseService.createCourse(courseCode, title, semester, lecturer.getId());
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/mine") // Replaces insecure /student/{studentId}
    public ResponseEntity<?> getMyCourses(Principal principal) {
        try {
            User student = authService.getUserByEmailOrUsername(principal.getName());
            List<Course> courses = courseService.getStudentEnrolledCourses(student.getId());
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{courseCode}/roster-upload")
    public ResponseEntity<?> uploadRoster(
            @PathVariable String courseCode,
            @RequestParam("file") MultipartFile file) {
        try {
            courseService.uploadRosterCsv(courseCode, file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Roster staged successfully from CSV."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{courseCode}/confirm-roster")
    public ResponseEntity<?> confirmRoster(@PathVariable String courseCode) {
        try {
            Course course = courseService.confirmRoster(courseCode);
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentCourses(@PathVariable Long studentId) {
        try {
            List<Course> courses = courseService.getStudentEnrolledCourses(studentId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{courseCode}/request-access")
    public ResponseEntity<?> requestAccess(
            @PathVariable String courseCode,
            @RequestParam Long studentId) {
        try {
            CourseAccessRequest req = courseService.requestCourseAccess(courseCode, studentId);
            return ResponseEntity.ok(req);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/requests/{requestId}/action")
    public ResponseEntity<?> handleRequest(
            @PathVariable Long requestId,
            @RequestParam boolean approve) {
        try {
            courseService.handleAccessRequest(requestId, approve);
            return ResponseEntity.ok(Map.of("success", true, "message", "Request processed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}