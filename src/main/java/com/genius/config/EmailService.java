package com.genius.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerification(String toEmail, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Smart Attendance - Email Verification Code");
        message.setText("Your 6-digit verification code for Smart Attendance is: " + code +
                "\n\nPlease enter this code on the verification page to complete your registration.");

        mailSender.send(message);
    }
}