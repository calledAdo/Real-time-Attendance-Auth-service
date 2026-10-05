package com.genius.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerification(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("christopheradegoke4@gmail.com");
        message.setTo(toEmail);
        message.setSubject("Email Verification Code");
        message.setText("Your verification code is: " + token);
        mailSender.send(message);
    }
}