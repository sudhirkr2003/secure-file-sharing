package com.securefilesharing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securefilesharing.entity.OtpVerification;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeOrderByIdDesc(
            String email, String purpose);
}
