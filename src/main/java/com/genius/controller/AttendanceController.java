package com.genius.controller;

import com.genius.model.Attendance;
import com.genius.model.AttendanceSession;
import com.genius.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/verify-face")
    public ResponseEntity<?> verifyAndCheckIn(@RequestBody Map<String, String> payload) {
        try {
            String sessionCode = payload.get("sessionCode");
            String username = payload.get("username");
            String facialEmbedding = payload.get("facialEmbedding");

            Attendance attendance = attendanceService.verifyAndRecordAttendance(sessionCode, username, facialEmbedding);

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
}