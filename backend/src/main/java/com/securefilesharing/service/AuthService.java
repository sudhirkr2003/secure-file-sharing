package com.securefilesharing.service;


import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefilesharing.dto.LoginRequest;
import com.securefilesharing.dto.RegisterRequest;
import com.securefilesharing.entity.User;
import com.securefilesharing.repository.UserRepository;
import com.securefilesharing.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService) {
        super();
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    // OTP based registration
    public void requestRegistrationOtp(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already registered");
        }

        otpService.requestRegistrationOtp(request);
    }

    public void verifyRegistrationOtp(
            String email,
            String otp) {
        otpService.verifyRegistrationOtp(
                email,
                otp,
                this);
    }

    @Transactional
    public User createUserAfterOtp(
            String name,
            String email,
            String passwordHash) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordHash);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    public void resendRegistrationOtp(RegisterRequest request) {
        requestRegistrationOtp(request);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void requestPasswordResetOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(normalizedEmail)) {
            // Same response for known/unknown emails should be used by frontend.
            return;
        }

        otpService.requestPasswordResetOtp(normalizedEmail);
    }

    public void resendPasswordResetOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        otpService.resendPasswordResetOtp(normalizedEmail);
    }

    public void verifyPasswordResetOtp(
            String email,
            String otp) {
        otpService.verifyPasswordResetOtp(email, otp);
    }

    @Transactional
    public void resetPassword(
            String email,
            String newPassword) {

        String normalizedEmail = email.trim().toLowerCase();

        if (!otpService.isPasswordResetVerified(normalizedEmail)) {
            throw new RuntimeException(
                    "Password reset OTP has not been verified");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        otpService.clearPasswordResetVerification(normalizedEmail);
    }

    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(
                request.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                    new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail());
    }

}
