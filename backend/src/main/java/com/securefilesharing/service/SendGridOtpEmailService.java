package com.securefilesharing.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Sends OTP emails through SendGrid's HTTP API (v3 /mail/send).
 *
 * Why this instead of Mailgun/SMTP:
 * - Render's free web services block outbound SMTP (ports 25/465/587),
 *   so any SMTP-based sender (Gmail SMTP, etc.) silently fails in prod
 *   even though it works fine locally. This service talks plain HTTPS,
 *   which is not blocked.
 * - SendGrid lets you verify a single sender EMAIL ADDRESS (Settings ->
 *   Sender Authentication -> Single Sender Verification) instead of a
 *   whole DOMAIN. Once that one address is verified you can send to any
 *   recipient, unlike Mailgun's sandbox domain or Resend's onboarding@
 *   address, which only deliver to the account owner's own inbox.
 * - No credit card is required to create a SendGrid account and use the
 *   free tier (100 emails/day, 60-day trial to start).
 */
@Service
@Profile("prod")
public class SendGridOtpEmailService implements OtpEmailService {

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    private final HttpClient httpClient;

    public SendGridOtpEmailService(
            @Value("${sendgrid.api-key}") String apiKey,
            @Value("${sendgrid.sender-email}") String senderEmail,
            @Value("${sendgrid.sender-name:SecureShare}") String senderName) {

        this.apiKey = apiKey;
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

        String html = buildHtml(otp, purpose);

        String plainText = "Your SecureShare verification code is "
                + otp
                + ". This code expires in 10 minutes.";

        String jsonBody = "{"
                + "\"personalizations\":[{\"to\":[{\"email\":\"" + jsonEscape(email) + "\"}]}],"
                + "\"from\":{\"email\":\"" + jsonEscape(senderEmail) + "\",\"name\":\"" + jsonEscape(senderName) + "\"},"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"content\":["
                +   "{\"type\":\"text/plain\",\"value\":\"" + jsonEscape(plainText) + "\"},"
                +   "{\"type\":\"text/html\",\"value\":\"" + jsonEscape(html) + "\"}"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {

                throw new IllegalStateException(
                        "SendGrid email sending failed. HTTP "
                                + response.statusCode()
                                + ": "
                                + response.body());
            }

            System.out.println(
                    "[SENDGRID] OTP email sent successfully to " + email);

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to send OTP email through SendGrid",
                    e);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "OTP email sending was interrupted",
                    e);
        }
    }

    private String jsonEscape(String value) {

        StringBuilder sb = new StringBuilder();

        for (char c : value.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }

        return sb.toString();
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
