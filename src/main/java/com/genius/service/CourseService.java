package com.genius.service;

import com.genius.model.Course;
import com.genius.model.CourseAccessRequest;
import com.genius.model.CourseRoster;
import com.genius.model.User;
import com.genius.repo.CourseAccessRequestRepository;
import com.genius.repo.CourseRepository;
import com.genius.repo.CourseRosterRepository;
import com.genius.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    @Autowired
    private CourseRepository courseRepo;

    @Autowired
    private CourseRosterRepository rosterRepo;

    @Autowired
    private CourseAccessRequestRepository accessRequestRepo;

    @Autowired
    private UserRepository userRepo;

    // 1. Lecturer creates course in DRAFT state
    public Course createCourse(String courseCode, String title, String semester, Long lecturerId) {
        if (courseRepo.findByCourseCode(courseCode.toUpperCase()).isPresent()) {
            throw new RuntimeException("Course code already exists!");
        }

        Course course = new Course();
        course.setCourseCode(courseCode.toUpperCase());
        course.setTitle(title);
        course.setSemester(semester);
        course.setLecturerId(lecturerId);
        course.setStatus(Course.CourseStatus.DRAFT);

        return courseRepo.save(course);
    }

    // 2. Upload CSV Roster (Populates staging table without making fake accounts)
    public void uploadRosterCsv(String courseCode, MultipartFile file) {
        String upperCode = courseCode.toUpperCase();
        Course course = courseRepo.findByCourseCode(upperCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (course.getStatus() == Course.CourseStatus.ACTIVE) {
            throw new RuntimeException("Cannot modify roster for an active course. Switch back to draft or manage manually.");
        }

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            List<CourseRoster> rosterList = new ArrayList<>();

            for (CSVRecord csvRecord : csvRecords) {
                String fullName = csvRecord.get("name");
                String matricNo = csvRecord.get("matricNo");
                String email = csvRecord.get("email");

                // Check if already in staging to avoid duplicates
                if (!rosterRepo.existsByCourseCodeAndMatricNo(upperCode, matricNo)) {
                    CourseRoster roster = new CourseRoster();
                    roster.setCourseCode(upperCode);
                    roster.setFullName(fullName);
                    roster.setMatricNo(matricNo);
                    roster.setEmail(email);
                    roster.setConfirmed(false); // Staging state
                    rosterList.add(roster);
                }
            }
            rosterRepo.saveAll(rosterList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }

    // 3. Lecturer confirms roster and flips course to ACTIVE
    public Course confirmRoster(String courseCode) {
        String upperCode = courseCode.toUpperCase();
        Course course = courseRepo.findByCourseCode(upperCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<CourseRoster> stagingRows = rosterRepo.findByCourseCodeAndConfirmed(upperCode, false);
        for (CourseRoster row : stagingRows) {
            row.setConfirmed(true);
        }
        rosterRepo.saveAll(stagingRows);

        course.setStatus(Course.CourseStatus.ACTIVE);
        return courseRepo.save(course);
    }

    // 4. Student Auto-Discovery (Fetches active courses matching student's matric number)
    public List<Course> getStudentEnrolledCourses(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getMatricNo() == null || student.getMatricNo().trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Find confirmed roster entries matching this student's matric number
        List<CourseRoster> rosters = rosterRepo.findByMatricNoAndConfirmed(student.getMatricNo(), true);
        List<Course> activeCourses = new ArrayList<>();

        for (CourseRoster roster : rosters) {
            courseRepo.findByCourseCode(roster.getCourseCode())
                    .filter(course -> course.getStatus() == Course.CourseStatus.ACTIVE)
                    .ifPresent(activeCourses::add);
        }

        return activeCourses;
    }

    // 5. Fallback: Request Access if missing from CSV
    public CourseAccessRequest requestCourseAccess(String courseCode, Long studentId) {
        String upperCode = courseCode.toUpperCase();
        Course course = courseRepo.findByCourseCode(upperCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getMatricNo() == null || student.getMatricNo().trim().isEmpty()) {
            throw new RuntimeException("Student must have a registered matriculation number to request access.");
        }

        if (rosterRepo.existsByCourseCodeAndMatricNo(upperCode, student.getMatricNo())) {
            throw new RuntimeException("You are already on the course roster!");
        }

        // Check if a request already exists
        Optional<CourseAccessRequest> existing = accessRequestRepo.findByCourseCodeAndStudentId(upperCode, studentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        CourseAccessRequest request = new CourseAccessRequest();
        request.setCourseCode(upperCode);
        request.setStudentId(studentId);
        request.setMatricNo(student.getMatricNo());
        request.setStatus(CourseAccessRequest.RequestStatus.PENDING);

        return accessRequestRepo.save(request);
    }

    // 6. Lecturer Approves/Rejects Access Request
    public void handleAccessRequest(Long requestId, boolean approve) {
        CourseAccessRequest request = accessRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (approve) {
            User student = userRepo.findById(request.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            // Check if already on roster just in case
            if (!rosterRepo.existsByCourseCodeAndMatricNo(request.getCourseCode(), student.getMatricNo())) {
                CourseRoster roster = new CourseRoster();
                roster.setCourseCode(request.getCourseCode());
                roster.setFullName(student.getFullName());
                roster.setMatricNo(student.getMatricNo());
                roster.setEmail(student.getEmail());
                roster.setConfirmed(true); // Automatically confirmed since lecturer approved it
                rosterRepo.save(roster);
            }

            request.setStatus(CourseAccessRequest.RequestStatus.APPROVED);
        } else {
            request.setStatus(CourseAccessRequest.RequestStatus.REJECTED);
        }
        accessRequestRepo.save(request);
    }
}