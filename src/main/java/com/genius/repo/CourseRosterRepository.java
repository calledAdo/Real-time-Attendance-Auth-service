package com.genius.repo;

import com.genius.model.CourseRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRosterRepository extends JpaRepository<CourseRoster, Long> {
    List<CourseRoster> findByCourseCodeAndConfirmed(String courseCode, boolean confirmed);
    Optional<CourseRoster> findByCourseCodeAndMatricNo(String courseCode, String matricNo);
    boolean existsByCourseCodeAndMatricNo(String courseCode, String matricNo);
    List<CourseRoster> findByMatricNoAndConfirmed(String matricNo, boolean confirmed);
}