package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "role_id"})
})
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id")
    private Integer userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Tên cột khóa ngoại trong CSDL
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false) // Tên cột khóa ngoại trong CSDL
    private Role role;

    @Column(name = "role_assignment_date", nullable = false, updatable = false)
    private LocalDateTime roleAssignmentDate;

    @Column(name = "is_role_active_for_user", nullable = false)
    private boolean isRoleActiveForUser = true;

    public UserRole() {
        this.roleAssignmentDate = LocalDateTime.now();
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.roleAssignmentDate = LocalDateTime.now();
    }
}
