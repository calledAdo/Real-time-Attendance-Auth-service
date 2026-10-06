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
    private static final double EARTH_RADIUS_METERS = 6371000;

    // 1. Lecturer Starts an Attendance Session (with Geofencing coordinates)
    public AttendanceSession startSession(String courseCode, Long lecturerId, int durationMinutes, Double latitude, Double longitude) {
        String upperCode = courseCode.toUpperCase();
        Course course = courseRepo.findByCourseCode(upperCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (course.getStatus() != Course.CourseStatus.ACTIVE) {
            throw new RuntimeException("Cannot start attendance for an inactive or draft course.");
        }

        String sessionCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        AttendanceSession session = new AttendanceSession();
        session.setCourseCode(upperCode);
        session.setSessionCode(sessionCode);
        session.setLecturerId(lecturerId);
        session.setStatus(AttendanceSession.SessionStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes));
        session.setLatitude(latitude);
        session.setLongitude(longitude);

        return sessionRepo.save(session);
    }

    // Overload for legacy payloads lacking coordinates
    public AttendanceSession startSession(String courseCode, Long lecturerId, int durationMinutes) {
        return startSession(courseCode, lecturerId, durationMinutes, null, null);
    }

    // 2. Lecturer Closes a Session
    public AttendanceSession closeSession(Long sessionId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setStatus(AttendanceSession.SessionStatus.CLOSED);
        return sessionRepo.save(session);
    }

    // 3. Legacy Facial Verification (Username-based)
    public Attendance verifyAndRecordAttendanceLegacy(String sessionCode, String username, String liveEmbeddingJson) {
        AttendanceSession session = sessionRepo.findBySessionCode(sessionCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Error: Invalid attendance session code."));

        validateSessionState(session);

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return processAttendanceRecord(session, user, liveEmbeddingJson);
    }

    // 4. Secure Session ID Check-in Method with Geofence & Facial Verification
    public Attendance verifyAndRecordAttendanceById(Long sessionId, String userEmail, String code, double latitude, double longitude, String liveEmbeddingJson) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Error: Attendance session not found."));

        validateSessionState(session);

        if (code == null || !session.getSessionCode().equalsIgnoreCase(code.trim())) {
            throw new RuntimeException("Error: Invalid attendance session code.");
        }

        // Validate Geofence (100-meter radius check)
        if (session.getLatitude() != null && session.getLongitude() != null) {
            double distance = calculateDistance(session.getLatitude(), session.getLongitude(), latitude, longitude);
            double allowedRadiusMeters = 100.0;

            if (distance > allowedRadiusMeters) {
                throw new RuntimeException("Geofence validation failed: You are " + Math.round(distance) + "m away from the lecture venue.");
            }
        }

        // Fetch user securely via the JWT email principal
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return processAttendanceRecord(session, user, liveEmbeddingJson);
    }

    // Helper to validate time window and closed status
    private void validateSessionState(AttendanceSession session) {
        if (session.getStatus() != AttendanceSession.SessionStatus.ACTIVE) {
            throw new RuntimeException("Error: This attendance session has been closed.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (session.getExpiresAt() != null && now.isAfter(session.getExpiresAt())) {
            session.setStatus(AttendanceSession.SessionStatus.CLOSED);
            sessionRepo.save(session);
            throw new RuntimeException("Error: This attendance session has expired.");
        }
        if (now.isBefore(session.getCreatedAt())) {
            throw new RuntimeException("Error: Attendance session has not started yet.");
        }
    }

    // Shared internal helper to enforce roster, duplicate checks, and face verification
    private Attendance processAttendanceRecord(AttendanceSession session, User user, String liveEmbeddingJson) {
        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Access denied: Facial verification is restricted to students.");
        }

        if (user.getMatricNo() == null || user.getMatricNo().trim().isEmpty()) {
            throw new RuntimeException("Student profile is missing a matriculation number.");
        }

        if (user.getFacialEmbedding() == null || user.getFacialEmbedding().isEmpty()) {
            throw new RuntimeException("No facial embedding registered for this student.");
        }

        boolean isEnrolled = rosterRepo.existsByCourseCodeAndMatricNo(session.getCourseCode(), user.getMatricNo());
        if (!isEnrolled) {
            throw new RuntimeException("Error: You are not registered on the official roster for " + session.getCourseCode() + ".");
        }

        boolean alreadyCheckedIn = attendanceRepo.existsBySessionIdAndMatricNo(session.getId(), user.getMatricNo());
        if (alreadyCheckedIn) {
            throw new RuntimeException("Attendance already marked for this session.");
        }

        boolean isMatch = compareEmbeddings(user.getFacialEmbedding(), liveEmbeddingJson);
        if (!isMatch) {
            throw new RuntimeException("Face mismatch. Verification failed.");
        }

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

    // Fetch records for a session (Lecturer view)
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

    // Haversine Distance Calculator
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}