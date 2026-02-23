package com.sidms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sidms.entity.MemberProfile;

@Repository
public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    List<MemberProfile> findByAssignedManager(String managerUsername);

    Optional<MemberProfile> findByUserId(Long userId);

    Optional<MemberProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserId(Long userId);
}
