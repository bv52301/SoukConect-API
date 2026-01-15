package com.souk.auth.service;

import com.souk.auth.config.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verify your SoukConect email";
        String verifyUrl = emailProperties.getVerifyEmailUrl(token);

        String htmlContent = buildVerificationEmailHtml(verifyUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Reset your SoukConect password";
        String resetUrl = emailProperties.getResetPasswordUrl(token);

        String htmlContent = buildPasswordResetEmailHtml(resetUrl);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String subject = "Welcome to SoukConect!";
        String htmlContent = buildWelcomeEmailHtml(firstName);

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendMfaEnabledEmail(String toEmail) {
        String subject = "Two-Factor Authentication Enabled";
        String htmlContent = buildMfaEnabledEmailHtml();

        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFromAddress(), emailProperties.getFromName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Verify Your Email Address</h2>
                    <p>Thank you for registering with SoukConect. Please click the button below to verify your email address:</p>
                    <p><a href="%s" class="button">Verify Email</a></p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p>%s</p>
                    <p>This link will expire in 24 hours.</p>
                    <div class="footer">
                        <p>If you didn't create an account with SoukConect, please ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verifyUrl, verifyUrl);
    }

    private String buildPasswordResetEmailHtml(String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #007bff; color: white; text-decoration: none; border-radius: 4px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Reset Your Password</h2>
                    <p>We received a request to reset your password. Click the button below to create a new password:</p>
                    <p><a href="%s" class="button">Reset Password</a></p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p>%s</p>
                    <p>This link will expire in 1 hour.</p>
                    <div class="footer">
                        <p>If you didn't request a password reset, please ignore this email or contact support if you're concerned.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetUrl, resetUrl);
    }

    private String buildWelcomeEmailHtml(String firstName) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Welcome to SoukConect!</h2>
                    <p>Hi %s,</p>
                    <p>Thank you for joining SoukConect. We're excited to have you on board!</p>
                    <p>You can now explore our marketplace and discover amazing products from local vendors.</p>
                    <div class="footer">
                        <p>If you have any questions, feel free to contact our support team.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildMfaEnabledEmailHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Two-Factor Authentication Enabled</h2>
                    <p>Two-factor authentication has been successfully enabled on your SoukConect account.</p>
                    <p>From now on, you'll need to enter a verification code from your authenticator app when logging in.</p>
                    <div class="footer">
                        <p>If you didn't enable two-factor authentication, please contact our support team immediately.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
