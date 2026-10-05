package com.genius.service;

import com.genius.model.Attendance;
import com.genius.model.Role;
import com.genius.model.User;
import com.genius.repo.AttendanceRepo;
import com.genius.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepo attendanceRepo;

    @Autowired
    private UserRepository userRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Attendance verifyAndRecoreAttendance(String username, String liveEmbeddingJson,String courseCode){
        User user = userRepo.findByUsername(username)
                .orElseThrow(()->new RuntimeException("User not found"));

        if(user.getRole() != Role.STUDENT){
            throw new RuntimeException("Access denied, Facial verification is restricted to only student.");
        }

        if(user.getFacialEmbedding() == null | user.getFacialEmbedding().isEmpty()){
            throw new RuntimeException("No facial embedding registered for this student");
        };

        LocalDate today = LocalDate.now();
        boolean alreadyCheckedIn = attendanceRepo.existsByMatricNoAndDate(user.getMatricNo(), today);
        if(alreadyCheckedIn){
            throw new RuntimeException("Attendance for marked for this course "+courseCode+".");
        }

        boolean isMatch = compareEmbeddings(user.getFacialEmbedding(), liveEmbeddingJson);
        if(!isMatch){
            throw new RuntimeException("Face mismatch. Verification failed.");
        }

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setMatricNo(user.getMatricNo());
        attendance.setCourseCode(courseCode);
        attendance.setDate(today);
        attendance.setTimestamp(LocalDateTime.now());
        attendance.setStatus("PRESENT");

        return attendanceRepo.save(attendance);
    }

    private boolean compareEmbeddings(String storedEmbeddingJson, String liveEmbeddingJson){
        try{
            double[] stored = objectMapper.readValue(storedEmbeddingJson,double[].class);
            double[] live = objectMapper.readValue(liveEmbeddingJson,double[].class);
            if(stored.length != live.length){
                return false;
            }

            double dotProduct = 0.0;
            double normA = 0.0;
            double normB = 0.0;

            for(int i=0;i<stored.length;i++){
                dotProduct += stored[i] * live[i];
                normA += Math.pow(stored[i],2);
                normB += Math.pow(live[i],2);
            }

            if(normA == 0 || normB == 0){
                return false;
            }

            double similarity = dotProduct/(Math.sqrt(normA) * Math.sqrt(normB));

            double threshold = 0.65;

            return similarity >= threshold;
        } catch (Exception e) {
            return false;
        }
    }
}
