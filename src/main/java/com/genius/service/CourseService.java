package com.genius.service;

import com.genius.dto.CourseRequest;
import com.genius.model.Course;
import com.genius.model.Role;
import com.genius.model.StudentCourseRegistration;
import com.genius.model.User;
import com.genius.repo.CourseRepository;
import com.genius.repo.StudentCourseRegistrationRepository;
import com.genius.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final StudentCourseRegistrationRepository registrationRepository;

    // 1. Create Course with normalization and duplicate checking
    public Course createCourse(CourseRequest request, String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        if (request.getCourseCode() == null || request.getCourseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Course code cannot be empty.");
        }

        // Normalize: Uppercase and remove all spaces (e.g., "CSC 301" -> "CSC301")
        String normalizedCourseCode = request.getCourseCode().toUpperCase().replaceAll("\\s+", "");

        // Check if course already exists
        if (courseRepository.findByCourseCode(normalizedCourseCode).isPresent()) {
            throw new RuntimeException("Course already exists! A primary lecturer has already registered this course. Please contact the primary lecturer or HOD to assign you to this course.");
        }

        Course course = Course.builder()
                .courseCode(normalizedCourseCode)
                .courseTitle(request.getCourseTitle())
                .primaryLecturer(lecturer)
                .build();

        // Add creator as initial lecturer/co-teacher
        course.getLecturers().add(lecturer);

        return courseRepository.save(course);
    }

    // 2. Assign Supporting Lecturer (Restricted to Primary Lecturer / HOD)
    public void assignSupportingLecturer(String courseCode, String targetLecturerEmail, String currentLecturerEmail) {
        User currentLecturer = userRepository.findByEmail(currentLecturerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String normalizedCode = courseCode.toUpperCase().replaceAll("\\s+", "");
        Course course = courseRepository.findByCourseCode(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Course not found: " + normalizedCode));

        // Verify authorization
        if (!course.getPrimaryLecturer().equals(currentLecturer)) {
            throw new RuntimeException("Only the primary course coordinator/HOD can assign supporting lecturers.");
        }

        User supportLecturer = userRepository.findByEmail(targetLecturerEmail)
                .orElseThrow(() -> new RuntimeException("Target lecturer not found"));

        if (supportLecturer.getRole() != Role.LECTURER) {
            throw new RuntimeException("User is not a lecturer.");
        }

        course.getLecturers().add(supportLecturer);
        courseRepository.save(course);
    }

    // 3. Bulk Enroll Students via List Upload
    public Map<String, Object> bulkEnrollStudents(String courseCode, List<String> matricNumbers, String lecturerEmail) {
        User lecturer = userRepository.findByEmail(lecturerEmail)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        String normalizedCode = courseCode.toUpperCase().replaceAll("\\s+", "");
        Course course = courseRepository.findByCourseCode(normalizedCode)
                .orElseThrow(() -> new RuntimeException("Course not found: " + normalizedCode));

        // Verify that the logged-in lecturer teaches this course
        if (!course.getLecturers().contains(lecturer)) {
            throw new RuntimeException("You are not authorized to enroll students for this course.");
        }

        int enrolledCount = 0;
        int skippedCount = 0;

        for (String matricNo : matricNumbers) {
            User student = userRepository.findByMatricNo(matricNo.trim()).orElse(null);

            // Skip invalid users or non-students
            if (student == null || student.getRole() != Role.STUDENT) {
                skippedCount++;
                continue;
            }

            boolean exists = registrationRepository.existsByStudentAndCourse(student, course);
            if (!exists) {
                StudentCourseRegistration registration = StudentCourseRegistration.builder()
                        .student(student)
                        .course(course)
                        .build();
                registrationRepository.save(registration);
                enrolledCount++;
            }
        }

        return Map.of(
                "message", "Bulk enrollment completed.",
                "enrolledCount", enrolledCount,
                "skippedOrNotFoundCount", skippedCount
        );
    }
}