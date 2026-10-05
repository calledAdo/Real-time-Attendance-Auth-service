package com.genius.controller;

import com.genius.model.Attendance;
import com.genius.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/verify-face")
    public ResponseEntity<?> verifyAndCheckIn(@RequestBody Map<String,String> payload){
        try{
            String username = payload.get("username");
            String facialEmbedding = payload.get("facialEmbedding");
            String courseCode = payload.get("courseCode");


            Attendance attendance = attendanceService.verifyAndRecoreAttendance(username,facialEmbedding,courseCode);
            return ResponseEntity.ok(Map.of(
                    "success",true,
                    "message", "Attendance marked successfully for "+ attendance.getMatricNo(),
                    "timestamp", attendance.getTimestamp().toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message",e.getMessage()
            ));
        }
    }
}
