package com.securefilesharing.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securefilesharing.dto.RegisterRequest;
import com.securefilesharing.entity.OtpVerification;
import com.securefilesharing.repository.OtpVerificationRepository;

@Service
public class OtpService {

    public static final String REGISTRATION = "REGISTRATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 120;

    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpEmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpVerificationRepository otpRepository,
            PasswordEncoder passwordEncoder,
            OtpEmailService emailService) {

        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

	@Transactional
	public void requestRegistrationOtp(RegisterRequest request) {
		String email = request.getEmail().trim().toLowerCase();
		OtpVerification otp = new OtpVerification();
		otp.setEmail(email);
		otp.setPurpose(REGISTRATION);
		otp.setName(request.getName().trim());
		otp.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		createAndSend(otp);
	}

	@Transactional
	public void verifyRegistrationOtp(String email, String code, AuthService authService) {
		OtpVerification otp = getValidOtp(email, REGISTRATION, code);
		authService.createUserAfterOtp(otp.getName(), otp.getEmail(), otp.getPasswordHash());
		otp.setVerified(true);
		otpRepository.save(otp);
	}

	@Transactional
	public void requestPasswordResetOtp(String email) {
		OtpVerification otp = new OtpVerification();
		otp.setEmail(email.trim().toLowerCase());
		otp.setPurpose(PASSWORD_RESET);
		createAndSend(otp);
	}

	@Transactional
	public void verifyPasswordResetOtp(String email, String code) {
		OtpVerification otp = getValidOtp(email, PASSWORD_RESET, code);
		otp.setVerified(true);
		otpRepository.save(otp);
	}

	@Transactional
	public void resendRegistrationOtp(RegisterRequest request) {
		requestRegistrationOtp(request);
	}

	@Transactional
	public void resendPasswordResetOtp(String email) {
		requestPasswordResetOtp(email);
	}

	public boolean isPasswordResetVerified(String email) {
		return otpRepository.findTopByEmailAndPurposeOrderByIdDesc(
				email.trim().toLowerCase(), PASSWORD_RESET)
				.map(OtpVerification::isVerified)
				.orElse(false);
	}

	@Transactional
	public void clearPasswordResetVerification(String email) {
		otpRepository.findTopByEmailAndPurposeOrderByIdDesc(
				email.trim().toLowerCase(), PASSWORD_RESET)
				.ifPresent(otp -> {
					otp.setVerified(false);
					otpRepository.save(otp);
				});
	}

	private void createAndSend(OtpVerification otp) {
		ensureOtpRequestAllowed(otp.getEmail(), otp.getPurpose());
		LocalDateTime createdAt = LocalDateTime.now();
		String code = String.format("%06d", secureRandom.nextInt(1_000_000));
		otp.setOtpHash(passwordEncoder.encode(code));
		otp.setCreatedAt(createdAt);
		otp.setExpiresAt(createdAt.plusMinutes(OTP_EXPIRY_MINUTES));
		otp.setAttempts(0);
		otp.setVerified(false);
		otpRepository.save(otp);
		emailService.sendOtp(otp.getEmail(), code, otp.getPurpose());
	}

	private void ensureOtpRequestAllowed(String email, String purpose) {
		otpRepository.findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
				.ifPresent(latestOtp -> {
					LocalDateTime sentAt = latestOtp.getCreatedAt();
					if (sentAt == null) {
						return;
					}

					long elapsedSeconds = java.time.Duration
							.between(sentAt, LocalDateTime.now()).getSeconds();
					if (elapsedSeconds < RESEND_COOLDOWN_SECONDS) {
						long remainingSeconds = RESEND_COOLDOWN_SECONDS - elapsedSeconds;
						throw new RuntimeException(
								"Please wait " + remainingSeconds
										+ " seconds before requesting another OTP");
					}
				});
	}

	private OtpVerification getValidOtp(String email, String purpose, String code) {
		OtpVerification otp = otpRepository.findTopByEmailAndPurposeOrderByIdDesc(
				email.trim().toLowerCase(), purpose)
				.orElseThrow(() -> new RuntimeException("OTP not found"));

		if (otp.isVerified()) {
			throw new RuntimeException("OTP has already been used");
		}
		if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
			throw new RuntimeException("OTP has expired");
		}
		if (otp.getAttempts() >= MAX_ATTEMPTS) {
			throw new RuntimeException("Maximum OTP attempts exceeded");
		}

		otp.setAttempts(otp.getAttempts() + 1);
		if (!passwordEncoder.matches(code, otp.getOtpHash())) {
			otpRepository.save(otp);
			throw new RuntimeException("Invalid OTP");
		}

		otpRepository.save(otp);
		return otp;
	}
}
