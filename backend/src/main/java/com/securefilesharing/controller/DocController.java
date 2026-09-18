package com.securefilesharing.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocController {

    @GetMapping("/api/doc")
    public ApiDocumentation documentation() {
        return new ApiDocumentation(
                "SecureShare API",
                "Public endpoint documentation for the SecureShare backend.",
                18,
                List.of(
                        new EndpointDoc("POST", "/api/auth/register", "Public", "Request registration OTP", "JSON: name, email, password", "Returns a message confirming that the OTP was sent."),
                        new EndpointDoc("POST", "/api/auth/register/resend-otp", "Public", "Resend registration OTP", "JSON: name, email, password", "Returns a message confirming that the registration OTP was sent."),
                        new EndpointDoc("POST", "/api/auth/register/verify-otp", "Public", "Verify registration OTP and create account", "JSON: email, otp (6 digits)", "Returns the created user with HTTP 201."),
                        new EndpointDoc("POST", "/api/auth/login", "Public", "Authenticate a user", "JSON: email, password", "Returns a JWT token and the user name."),
                        new EndpointDoc("POST", "/api/auth/forgot-password", "Public", "Request password reset OTP", "JSON: email", "Returns a password reset message."),
                        new EndpointDoc("POST", "/api/auth/forgot-password/resend-otp", "Public", "Resend password reset OTP", "JSON: email", "Returns a password reset message."),
                        new EndpointDoc("POST", "/api/auth/forgot-password/verify-otp", "Public", "Verify password reset OTP", "JSON: email, otp (6 digits)", "Returns a message allowing the password reset step."),
                        new EndpointDoc("POST", "/api/auth/forgot-password/reset", "Public", "Reset password", "JSON: email, password, confirmPassword", "Password must be at least 8 characters and include a letter, number, and special character."),
                        new EndpointDoc("GET", "/api/health", "Public", "Check backend health", "No request body", "Returns the plain text message Service is running!."),
                        new EndpointDoc("POST", "/api/files/quick-share", "Public", "Create a quick share", "Multipart: file, expiryHours (1-24, default 24)", "Creates a link that can be downloaded without login."),
                        new EndpointDoc("GET", "/api/files/download/{shareToken}", "Public route, conditional access", "Download a shared file", "Path: shareToken", "Quick shares are public; secure shares require an authenticated and authorized user."),
                        new EndpointDoc("POST", "/api/files/upload", "Private", "Upload a file", "Bearer JWT; multipart: file", "Returns uploaded file metadata."),
                        new EndpointDoc("POST", "/api/files/share", "Private", "Create a secure share", "Bearer JWT; multipart: file, expiryHours (1-720), maxDownloads, allowedUserEmails (optional)", "Only the owner can manage the resulting share."),
                        new EndpointDoc("POST", "/api/files/share/{shareToken}/expiry", "Private", "Update secure share expiry", "Bearer JWT; path: shareToken; query: expiryHours", "Only the owner can extend an active secure share, up to 30 days from creation."),
                        new EndpointDoc("POST", "/api/files/share/{shareToken}/revoke", "Private", "Revoke a secure share", "Bearer JWT; path: shareToken", "Only the owner can revoke an active secure share."),
                        new EndpointDoc("GET", "/api/files/shares/history", "Private", "List owned shares", "Bearer JWT", "Returns shares created by the logged-in user."),
                        new EndpointDoc("GET", "/api/files/download-history", "Private", "List download history", "Bearer JWT", "Returns downloads made by the logged-in user."),
                        new EndpointDoc("GET", "/api/test/secure", "Private", "Test JWT authentication", "Bearer JWT", "Returns JWT authentication is working when the token is valid.")
                )
        );
    }

    public record ApiDocumentation(
            String title,
            String description,
            int endpointCount,
            List<EndpointDoc> endpoints) {
    }

    public record EndpointDoc(
            String method,
            String path,
            String access,
            String summary,
            String request,
            String notes) {
    }
}
