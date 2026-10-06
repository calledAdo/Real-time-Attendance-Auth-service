package com.genius.service;

import com.genius.config.EmailService;
import com.genius.dto.RegisterRequest;
import com.genius.model.Role;
import com.genius.model.User;
import com.genius.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;

    public User registerUser(RegisterRequest request){
        if(userRepo.existsByEmail(request.getEmail())){
            throw new RuntimeException("Error: Email already in use.");
        }
        if(userRepo.existsByUsername(request.getUsername())){
            throw new RuntimeException("Error: Username already in use.");
        }

        // Student Validation Logic
        if(request.getRole() == Role.STUDENT){
            if(!request.getEmail().endsWith("@student.oauife.edu.ng")){
                throw new IllegalArgumentException("Student email must end with @student.oauife.edu.ng");
            }
            if(request.getMatricNo() == null || request.getMatricNo().trim().isEmpty()) {
                throw new IllegalArgumentException("Matriculation number is required for students");
            }
            if(userRepo.findByMatricNo(request.getMatricNo()).isPresent()){
                throw new RuntimeException("Error: Matric number already registered!");
            }
        }
        // Lecturer Validation Logic
        else if(request.getRole() == Role.LECTURER){
            if(!request.getEmail().endsWith("@oauife.edu.ng") || request.getEmail().endsWith("@student.oauife.edu.ng")){
                throw new IllegalArgumentException("Lecturer email must be a valid staff email ending with @oauife.edu.ng");
            }
            // Lecturers do not use matric numbers, so ensure it's null or clear
            request.setMatricNo(null);
        } else {
            throw new IllegalArgumentException("Invalid user role specified.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setMatricNo(request.getMatricNo());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setFacialEmbedding(request.getFacialEmbedding());

        user.setEmailVerified(false);
        String verificationToken = String.format("%06d", new java.security.SecureRandom().nextInt(900000) + 100000);
        user.setEmailVerificationToken(verificationToken);

        User savedUser = userRepo.save(user);

        emailService.sendVerification(savedUser.getEmail(), verificationToken);

        return savedUser;
    }

    public User authenticateUser(String emailOrUsername, String password) {
        User user = userRepo.findByEmail(emailOrUsername)
                .or(()->userRepo.findByUsername(emailOrUsername))
                .orElseThrow(()-> new RuntimeException("Error: Invalid email/username or password"));


        if(!passwordEncoder.matches(password,user.getPassword())){
            throw new RuntimeException("Error: Invalid username/email or password");
        }

        return user;
    }


    public void resendVerificationToken(String email){
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with this email does not exist"));

        if(user.isEmailVerified()){
            throw new RuntimeException("This email is already verified. Please log in.");
        }

        // Generate a new 6-digit code
        String newCode = String.format("%06d", new java.security.SecureRandom().nextInt(900000) + 100000);
        user.setEmailVerificationToken(newCode);
        userRepo.save(user);

        // Send the new 6-digit code via email
        emailService.sendVerification(user.getEmail(), newCode);
    }

    public Role verifyUserEmail(String email, String token) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified.");
        }

        if (user.getEmailVerificationToken() == null || !user.getEmailVerificationToken().equals(token)) {
            throw new RuntimeException("Invalid verification code.");
        }

        // Mark as verified & clear token
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepo.save(user);

        return user.getRole();
    }

    public void saveFacialEmbedding(String username, String facialEmbeddingJson) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Facial onboarding is only available for students.");
        }

        user.setFacialEmbedding(facialEmbeddingJson);
        userRepo.save(user);
    }
}
