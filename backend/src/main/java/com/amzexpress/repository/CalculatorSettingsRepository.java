package com.amzexpress.repository;

import com.amzexpress.entity.CalculatorSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CalculatorSettingsRepository extends JpaRepository<CalculatorSettings, Long> {
    
    Optional<CalculatorSettings> findBySettingKey(String settingKey);
    
    boolean existsBySettingKey(String settingKey);
}
