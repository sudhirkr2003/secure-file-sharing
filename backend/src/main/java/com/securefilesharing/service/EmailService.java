package com.securefilesharing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${mail.enabled:false}") boolean mailEnabled) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
    }

    public void sendOtp(String email, String otp, String purpose) {

        if (!mailEnabled) {
            System.out.println("[LOCAL OTP] " + email + " -> " + otp);
            return;
        }

            String subject = "Secure File Sharing - "
                + ("REGISTRATION".equals(purpose)
                    ? "Verification Code"
                    : "Password Reset Code");

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(buildHtml(otp, purpose), true);
                helper.addInline(
                    "secure-share-logo",
                    new ClassPathResource("email/File_Sharing.png"),
                    "image/png");
                mailSender.send(message);
            } catch (MessagingException ex) {
                throw new IllegalStateException("Unable to create OTP email", ex);
        }
            }

            private String buildHtml(String otp, String purpose) {
            String title = "REGISTRATION".equals(purpose)
                ? "Verify your email"
                : "Reset your password";
            return """
                                <!doctype html>
                                <html><body style="margin:0;background:#eef4f3;font-family:Arial,sans-serif;color:#173b3a;">
                                    <div style="max-width:560px;margin:32px auto;padding:0 16px;">
                                        <div style="background:#0f716b;padding:22px 28px;border-radius:18px 18px 0 0;">
                                              <img src="cid:secure-share-logo" alt="Secure File Sharing" width="52" height="52" style="display:block;border-radius:12px;background:#ffffff;padding:6px;object-fit:contain;">
                                            <div style="margin-top:16px;color:#d9ffef;font-size:12px;letter-spacing:2px;font-weight:bold;">SECURE FILE SHARING</div>
                                        </div>
                                        <div style="background:#ffffff;padding:34px 28px 30px;border-radius:0 0 18px 18px;box-shadow:0 10px 30px rgba(15,113,107,.12);">
                                            <h1 style="margin:0 0 10px;font-size:28px;color:#173b3a;">%s</h1>
                                            <p style="margin:0 0 24px;color:#617572;font-size:15px;line-height:1.6;">Use the verification code below to continue. This code expires in 10 minutes.</p>
                                            <div style="background:#f0faf7;border:1px solid #bce8d7;border-radius:14px;padding:20px;text-align:center;">
                                                <div style="color:#6a807d;font-size:11px;letter-spacing:2px;font-weight:bold;">YOUR ONE-TIME CODE</div>
                                                <div style="margin-top:8px;color:#0f716b;font-size:36px;letter-spacing:8px;font-weight:bold;">%s</div>
                                            </div>
                                            <p style="margin:24px 0 0;color:#899a97;font-size:12px;line-height:1.6;">If you did not request this code, you can safely ignore this email.</p>
                                        </div>
                                        <p style="text-align:center;color:#8aa09c;font-size:11px;margin:16px 0;">Protected sharing, made simple.</p>
                                    </div>
                                </body></html>
                """.formatted(title, otp);
    }
}
