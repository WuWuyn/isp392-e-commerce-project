package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Date;

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
    private int userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Tên cột khóa ngoại trong CSDL
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false) // Tên cột khóa ngoại trong CSDL
    private Role role;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "role_assignment_date", nullable = false, updatable = false)
    private Timestamp roleAssignmentDate;

    @Column(name = "is_role_active_for_user", nullable = false)
    private boolean isRoleActiveForUser = true;

    public UserRole() {
        this.roleAssignmentDate = new Timestamp(System.currentTimeMillis());
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.roleAssignmentDate = new Timestamp(System.currentTimeMillis());
    }
}
