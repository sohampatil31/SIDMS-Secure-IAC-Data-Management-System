package com.sidms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Member profile containing sensitive personal data.
 * Fields marked as encrypted are stored as AES-256-GCM ciphertext (Base64-encoded)
 * and decrypted at the service layer before being returned to clients.
 */
@Entity
@Table(name = "member_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** Encrypted: AES-256-GCM, Base64-encoded. */
    @Column(columnDefinition = "TEXT")
    private String phoneNumber;

    /** Encrypted: AES-256-GCM, Base64-encoded. */
    @Column(columnDefinition = "TEXT")
    private String address;

    /** Encrypted: AES-256-GCM, Base64-encoded. */
    @Column(columnDefinition = "TEXT")
    private String resumeUrl;

    /** Encrypted: AES-256-GCM, Base64-encoded. */
    @Column(columnDefinition = "TEXT")
    private String governmentId;

    /** Username of the admin/manager who created this profile. */
    @Column(length = 50)
    private String createdBy;

    /** Username of the manager assigned to this member. */
    @Column(length = 50)
    private String assignedManager;

    /** User ID this profile is linked to (nullable for externally-managed profiles). */
    private Long userId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
