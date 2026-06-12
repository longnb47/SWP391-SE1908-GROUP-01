package com.se1908.group01.repository;

import com.se1908.group01.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserIdAndVerificationTypeOrderByCreatedAtDesc(
            Long userId,
            String verificationType
    );

    void deleteAllByUserIdAndVerificationType(
            Long userId,
            String verificationType
    );


}
