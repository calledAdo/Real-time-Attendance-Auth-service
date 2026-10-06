package com.genius.repo;

import com.genius.model.CourseAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAccessRequestRepository extends JpaRepository<CourseAccessRequest, Long> {
    List<CourseAccessRequest> findByCourseCodeAndStatus(String courseCode, CourseAccessRequest.RequestStatus status);
    Optional<CourseAccessRequest> findByCourseCodeAndStudentId(String courseCode, Long studentId);
}