package com.genius.repo;

import com.genius.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepo extends JpaRepository<Attendance,Long> {
    boolean existsByMatricNoAndDate(String matricNo, LocalDate date);
    List<Attendance> findByMatricNo(String No);
}
