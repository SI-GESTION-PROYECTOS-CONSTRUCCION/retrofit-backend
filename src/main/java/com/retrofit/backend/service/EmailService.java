package com.retrofit.backend.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String recipientName, String resetUrl);
    void sendHtmlEmail(String toEmail, String subject, String htmlContent);
}
