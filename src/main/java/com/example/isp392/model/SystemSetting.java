package com.example.isp392.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_settings")
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey; // Ví dụ: "hero_title", "contact_email"

    @Column(name = "setting_value", columnDefinition = "NVARCHAR(MAX)")
    private String settingValue; // Ví dụ: "Welcome to ReadHub", "readhub@hotmail.com"
}