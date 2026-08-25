package com.securefilesharing.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class MailgunOtpEmailService implements OtpEmailService {

    private final String apiKey;
    private final String domain;
    private final String senderEmail;
    private final String senderName;

    private final HttpClient httpClient;

    public MailgunOtpEmailService(
            @Value("${mailgun.api-key}") String apiKey,
            @Value("${mailgun.domain}") String domain,
            @Value("${mailgun.sender-email}") String senderEmail,
            @Value("${mailgun.sender-name:SecureShare}") String senderName) {

        this.apiKey = apiKey;
        this.domain = domain;
        this.senderEmail = senderEmail;
        this.senderName = senderName;

        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void sendOtp(String email, String otp, String purpose) {

        String subject = "Secure File Sharing - "
                + ("REGISTRATION".equals(purpose)
                        ? "Verification Code"
                        : "Password Reset Code");

        String from = senderName + " <" + senderEmail + ">";

        String html = buildHtml(otp, purpose);

        String formData =
                "from=" + encode(from)
                + "&to=" + encode(email)
                + "&subject=" + encode(subject)
                + "&html=" + encode(html)
                + "&text=" + encode(
                        "Your SecureShare verification code is "
                                + otp
                                + ". This code expires in 10 minutes.");

        String credentials = Base64.getEncoder()
                .encodeToString(
                        ("api:" + apiKey)
                                .getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.mailgun.net/v3/"
                                + domain
                                + "/messages"))
                .header(
                        "Authorization",
                        "Basic " + credentials)
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded")
                .POST(
                        HttpRequest.BodyPublishers
                                .ofString(formData))
                .build();

        try {

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "Mailgun email sending failed. HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body());
            }

            System.out.println(
                    "[MAILGUN] OTP email sent successfully to "
                            + email);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to send OTP email through Mailgun",
                    e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "OTP email sending was interrupted",
                    e);
        }
    }

    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8);
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