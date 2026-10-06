package com.genius.controller;

import com.genius.model.Attendance;
import com.genius.model.AttendanceSession;
import com.genius.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> payload) {
        try {
            String courseCode = (String) payload.get("courseCode");
            Long lecturerId = Long.valueOf(payload.get("lecturerId").toString());
            int durationMinutes = payload.containsKey("durationMinutes")
                    ? Integer.parseInt(payload.get("durationMinutes").toString())
                    : 15;

            AttendanceSession session = attendanceService.startSession(courseCode, lecturerId, durationMinutes);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<?> closeSession(@PathVariable Long sessionId) {
        try {
            AttendanceSession session = attendanceService.closeSession(sessionId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Legacy legacy/staging endpoint if still needed
    @PostMapping("/verify-face")
    public ResponseEntity<?> verifyAndCheckInLegacy(@RequestBody Map<String, String> payload) {
        try {
            String sessionCode = payload.get("sessionCode");
            String username = payload.get("username");
            String facialEmbedding = payload.get("facialEmbedding");

            Attendance attendance = attendanceService.verifyAndRecordAttendanceLegacy(sessionCode, username, facialEmbedding);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Attendance marked successfully for " + attendance.getMatricNo(),
                    "courseCode", attendance.getCourseCode(),
                    "timestamp", attendance.getTimestamp().toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/sessions/{sessionId}/records")
    public ResponseEntity<?> getSessionRecords(@PathVariable Long sessionId) {
        try {
            List<Attendance> records = attendanceService.getSessionRecords(sessionId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Authoritative Frontend Check-In Endpoint matching contract
    @PostMapping("/sessions/{sessionId}/check-ins")
    public ResponseEntity<?> checkIn(
            @PathVariable Long sessionId,
            @RequestBody Map<String, Object> payload,
            Principal principal) {
        try {
            String userEmail = principal.getName(); // Securely pulled from JWT principal token
            String code = (String) payload.get("code");
            double latitude = payload.containsKey("latitude") ? Double.parseDouble(payload.get("latitude").toString()) : 0.0;
            double longitude = payload.containsKey("longitude") ? Double.parseDouble(payload.get("longitude").toString()) : 0.0;
            String facialEmbedding = (String) payload.get("facialEmbedding");

            // Delegate ID lookup and verification cleanly to the service layer
            Attendance attendance = attendanceService.verifyAndRecordAttendanceById(
                    sessionId, userEmail, code, latitude, longitude, facialEmbedding
            );

            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "status", "PRESENT",
                    "checkedInAt", attendance.getTimestamp().toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "ATTENDANCE_FAILED",
                    "message", e.getMessage(),
                    "details", Map.of()
            ));
        }
    }
}