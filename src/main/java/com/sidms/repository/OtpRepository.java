package com.sidms.repository;

import com.sidms.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByUserIdAndVerifiedFalseOrderByExpiryTimeDesc(Long userId);

    void deleteByUserId(Long userId);
}
