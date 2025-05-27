package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "User_Roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"UserID", "RoleID"}) // Đảm bảo UserID và RoleID là duy nhất
})
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "role_assignment_date", nullable = false, updatable = false)
    private Date roleAssignmentDate; // Ngày vai trò được gán

    @Column(name = "is_role_active", nullable = false)
    private boolean isRoleActive = true; // Vai trò này có active cho user này không

    // Constructors
    public UserRole() {
        this.roleAssignmentDate = new Date();
    }

    public UserRole(User user, Role role) {
        this();
        this.user = user;
        this.role = role;
    }

}