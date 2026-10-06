package com.genius.service;

import com.genius.model.*;
import com.genius.repo.*;
import tools.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AttendanceService {

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private AttendanceSessionRepository sessionRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private CourseRosterRepository rosterRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Lecturer Starts an Attendance Session
    public AttendanceSession startSession(String courseCode, Long lecturerId, int durationMinutes) {
        String upperCode = courseCode.toUpperCase();
        Course course = courseRepo.findByCourseCode(upperCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (course.getStatus() != Course.CourseStatus.ACTIVE) {
            throw new RuntimeException("Cannot start attendance for an inactive or draft course.");
        }

        // Generate a 6-character session code
        String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        AttendanceSession session = new AttendanceSession();
        session.setCourseCode(upperCode);
        session.setSessionCode(sessionCode);
        session.setLecturerId(lecturerId);
        session.setStatus(AttendanceSession.SessionStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));

        return sessionRepo.save(session);
    }

    // 2. Lecturer Closes a Session
    public AttendanceSession closeSession(Long sessionId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(AttendanceSession.SessionStatus.CLOSED);
        return sessionRepo.save(session);
    }

    // 3. Facial Verification & Attendance Marking (Session-aware)
    public Attendance verifyAndRecordAttendance(String sessionCode, String username, String liveEmbeddingJson) {
        // Validate Session
        AttendanceSession session = sessionRepo.findBySessionCode(sessionCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Error: Invalid attendance session code."));

        if (session.getStatus() != AttendanceSession.SessionStatus.ACTIVE) {
            throw new RuntimeException("Error: This attendance session has been closed.");
        }

        if (session.getExpiresAt() != null && LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(AttendanceSession.SessionStatus.CLOSED);
            sessionRepo.save(session);
            throw new RuntimeException("Error: This attendance session has expired.");
        }

        // Validate User
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Access denied: Facial verification is restricted to students.");
        }

        if (user.getMatricNo() == null || user.getMatricNo().trim().isEmpty()) {
            throw new RuntimeException("Student profile is missing a matriculation number.");
        }

        if (user.getFacialEmbedding() == null || user.getFacialEmbedding().isEmpty()) {
            throw new RuntimeException("No facial embedding registered for this student.");
        }

        // Verify Student is on the Confirmed Course Roster
        boolean isEnrolled = rosterRepo.existsByCourseCodeAndMatricNo(session.getCourseCode(), user.getMatricNo());
        if (!isEnrolled) {
            throw new RuntimeException("Error: You are not registered on the official roster for " + session.getCourseCode() + ".");
        }

        // Check Duplicate Attendance for this Session
        boolean alreadyCheckedIn = attendanceRepo.existsBySessionIdAndMatricNo(session.getId(), user.getMatricNo());
        if (alreadyCheckedIn) {
            throw new RuntimeException("Attendance already marked for this session.");
        }

        // Perform Cosine Similarity Facial Verification
        boolean isMatch = compareEmbeddings(user.getFacialEmbedding(), liveEmbeddingJson);
        if (!isMatch) {
            throw new RuntimeException("Face mismatch. Verification failed.");
        }

        // Record Attendance
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setMatricNo(user.getMatricNo());
        attendance.setCourseCode(session.getCourseCode());
        attendance.setSessionId(session.getId());
        attendance.setDate(LocalDate.now());
        attendance.setTimestamp(LocalDateTime.now());
        attendance.setStatus("PRESENT");

        return attendanceRepo.save(attendance);
    }

    // 4. Fetch records for a session (Lecturer view)
    public List<Attendance> getSessionRecords(Long sessionId) {
        return attendanceRepo.findBySessionId(sessionId);
    }

    // Cosine Similarity Algorithm
    private boolean compareEmbeddings(String storedEmbeddingJson, String liveEmbeddingJson) {
        try {
            double[] stored = objectMapper.readValue(storedEmbeddingJson, double[].class);
            double[] live = objectMapper.readValue(liveEmbeddingJson, double[].class);
            if (stored.length != live.length) {
                return false;
            }

            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;

            for (int i = 0; i < stored.length; i++) {
                dotProduct += stored[i] * live[i];
                normA += Math.pow(stored[i], 2);
                normB += Math.pow(live[i], 2);
            }

            if (normA == 0 || normB == 0) {
                return false;
            }

            double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
            double threshold = 0.65;

            return similarity >= threshold;
        } catch (Exception e) {
            return false;
        }
    }
}