package com.tinasheGomo.EventManagementSystem.entity.organization;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@Getter @Setter @NoArgsConstructor
public class OrganizationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private String id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String logo;

    /**
     * Set after the founding user is created (see AuthService.register), so it
     * cannot be NOT NULL at insert time. @Transactional guarantees the row is
     * never committed without an owner.
     */
    private String ownerId;

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
