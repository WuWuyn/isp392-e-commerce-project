package com.example.isp392.service;

import com.example.isp392.model.SystemSetting;
import com.example.isp392.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Lấy một nhóm các cài đặt theo key
     * @param keys Danh sách các key cần lấy
     * @return Map chứa cặp key-value của các cài đặt
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettings(List<String> keys) {
        return settingRepository.findAllById(keys).stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    /**
     * Lưu một nhóm các cài đặt
     * @param settings Map chứa các cài đặt cần lưu
     */
    @Transactional
    public void saveSettings(Map<String, String> settings) {
        List<SystemSetting> settingEntities = settings.entrySet().stream()
                .map(entry -> new SystemSetting(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        settingRepository.saveAll(settingEntities);
    }
}