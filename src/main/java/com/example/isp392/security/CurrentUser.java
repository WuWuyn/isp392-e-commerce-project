package com.example.isp392.security;

import com.example.isp392.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Lớp CurrentUser dùng để wrap thông tin User cho Spring Security.
 */
public class CurrentUser implements UserDetails {

    private final User user;

    public CurrentUser(User user) {
        this.user = user;
    }

    /**
     * Lấy đối tượng User gốc.
     */
    public User getUser() {
        return user;
    }

    /**
     * Lấy danh sách quyền của user (dùng cho Spring Security).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getUserRoles().stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getRoleName()))
                .collect(Collectors.toSet());
    }

    /**
     * Lấy password của user.
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Lấy username (ở đây dùng email làm username).
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Kiểm tra user có role tương ứng không.
     * @param roleName tên role (ví dụ: "ADMIN", "SELLER")
     * @return true nếu user có role này
     */
    public boolean hasRole(String roleName) {
        return user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getRoleName().equalsIgnoreCase(roleName));
    }
}
