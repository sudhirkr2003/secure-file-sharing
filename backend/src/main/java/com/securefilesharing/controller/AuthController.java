package com.securefilesharing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.securefilesharing.dto.EmailRequest;
import com.securefilesharing.dto.LoginRequest;
import com.securefilesharing.dto.LoginResponse;
import com.securefilesharing.dto.MessageResponse;
import com.securefilesharing.dto.RegisterRequest;
import com.securefilesharing.dto.ResetPasswordRequest;
import com.securefilesharing.dto.UserResponse;
import com.securefilesharing.dto.VerifyOtpRequest;
import com.securefilesharing.entity.User;
import com.securefilesharing.repository.UserRepository;
import com.securefilesharing.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        super();
        this.authService = authService;
		this.userRepository = userRepository;
    }

    // =====================================================
    // OTP REGISTRATION
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.requestRegistrationOtp(request);

        return ResponseEntity.ok(
                new MessageResponse(
                        "OTP sent. Verify the OTP to complete registration."));
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<MessageResponse> resendRegistrationOtp(
            @Valid @RequestBody RegisterRequest request) {

        authService.resendRegistrationOtp(request);

        return ResponseEntity.ok(
                new MessageResponse("Registration OTP sent."));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<UserResponse> verifyRegistrationOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        authService.verifyRegistrationOtp(
                request.getEmail(),
                request.getOtp());

        // Return the newly created user.
        // The OTP flow creates it only after successful verification.
        // Fetching it is intentionally done through the service login path.
        User user = authService.getUserByEmail(request.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail()));
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.login(request);
        
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(
                new LoginResponse(token, user.getName()));
    }

    // =====================================================
    // FORGOT PASSWORD
    // =====================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody EmailRequest request) {

        authService.requestPasswordResetOtp(request.getEmail());

        return ResponseEntity.ok(
                new MessageResponse(
                        "If the email is registered, a password reset OTP has been sent."));
    }

    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<MessageResponse> resendResetOtp(
            @Valid @RequestBody EmailRequest request) {

                authService.resendPasswordResetOtp(request.getEmail());

        return ResponseEntity.ok(
                new MessageResponse(
                        "If the email is registered, a new password reset OTP has been sent."));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<MessageResponse> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        authService.verifyPasswordResetOtp(
                request.getEmail(),
                request.getOtp());

        return ResponseEntity.ok(
                new MessageResponse(
                        "OTP verified. You can now reset your password."));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
                request.getEmail(),
                request.getPassword());

        return ResponseEntity.ok(
                new MessageResponse("Password reset successfully."));
    }
}
