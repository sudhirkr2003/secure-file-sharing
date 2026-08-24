package com.securefilesharing.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailService {

    private final String brevoApiKey;
    private final String senderEmail;
    private final String senderName;
    private final boolean mailEnabled;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailService(
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.sender-email:}") String senderEmail,
            @Value("${brevo.sender-name:SecureShare}") String senderName,
            @Value("${mail.enabled:false}") boolean mailEnabled,
            ObjectMapper objectMapper) {

        this.brevoApiKey = brevoApiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.mailEnabled = mailEnabled;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendOtp(String email, String otp, String purpose) {

        if (!mailEnabled) {
            System.out.println("[LOCAL OTP] " + email + " -> " + otp);
            return;
        }

        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Brevo API key is not configured");
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "Brevo sender email is not configured");
        }

        String subject = "Secure File Sharing - "
                + ("REGISTRATION".equals(purpose)
                        ? "Verification Code"
                        : "Password Reset Code");

        try {

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);

            Map<String, Object> recipient = new HashMap<>();
            recipient.put("email", email);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", sender);
            body.put("to", new Object[]{recipient});
            body.put("subject", subject);
            body.put("htmlContent", buildHtml(otp, purpose));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "Brevo email sending failed. HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body());
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to send OTP email through Brevo",
                    e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "OTP email sending was interrupted",
                    e);
        }
    }

    private String buildHtml(String otp, String purpose) {

        String title = "REGISTRATION".equals(purpose)
                ? "Verify your email"
                : "Reset your password";

        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#eef4f3;font-family:Arial,sans-serif;color:#173b3a;">

                    <div style="max-width:560px;margin:32px auto;padding:0 16px;">

                        <div style="background:#0f716b;padding:22px 28px;border-radius:18px 18px 0 0;">

                            <div style="
                                color:#ffffff;
                                font-size:24px;
                                font-weight:bold;
                            ">
                                SecureShare
                            </div>

                            <div style="
                                margin-top:16px;
                                color:#d9ffef;
                                font-size:12px;
                                letter-spacing:2px;
                                font-weight:bold;
                            ">
                                SECURE FILE SHARING
                            </div>

                        </div>

                        <div style="
                            background:#ffffff;
                            padding:34px 28px 30px;
                            border-radius:0 0 18px 18px;
                            box-shadow:0 10px 30px rgba(15,113,107,.12);
                        ">

                            <h1 style="
                                margin:0 0 10px;
                                font-size:28px;
                                color:#173b3a;
                            ">
                                %s
                            </h1>

                            <p style="
                                margin:0 0 24px;
                                color:#617572;
                                font-size:15px;
                                line-height:1.6;
                            ">
                                Use the verification code below to continue.
                                This code expires in 10 minutes.
                            </p>

                            <div style="
                                background:#f0faf7;
                                border:1px solid #bce8d7;
                                border-radius:14px;
                                padding:20px;
                                text-align:center;
                            ">

                                <div style="
                                    color:#6a807d;
                                    font-size:11px;
                                    letter-spacing:2px;
                                    font-weight:bold;
                                ">
                                    YOUR ONE-TIME CODE
                                </div>

                                <div style="
                                    margin-top:8px;
                                    color:#0f716b;
                                    font-size:36px;
                                    letter-spacing:8px;
                                    font-weight:bold;
                                ">
                                    %s
                                </div>

                            </div>

                            <p style="
                                margin:24px 0 0;
                                color:#899a97;
                                font-size:12px;
                                line-height:1.6;
                            ">
                                If you did not request this code,
                                you can safely ignore this email.
                            </p>

                        </div>

                        <p style="
                            text-align:center;
                            color:#8aa09c;
                            font-size:11px;
                            margin:16px 0;
                        ">
                            Protected sharing, made simple.
                        </p>

                    </div>

                </body>
                </html>
                """.formatted(title, otp);
    }
}