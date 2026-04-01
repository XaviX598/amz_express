package com.amzexpress.controller;

import com.amzexpress.dto.CalculatorSettingsDto;
import com.amzexpress.dto.UpdateSettingsRequest;
import com.amzexpress.service.CalculatorSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class CalculatorSettingsController {

    private final CalculatorSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<CalculatorSettingsDto> getAllSettings() {
        Map<String, String> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(CalculatorSettingsDto.builder()
                .settings(settings)
                .build());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<CalculatorSettingsDto> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request) {
        Map<String, String> updatedSettings = settingsService.updateSettings(request.toSettingsMap());
        return ResponseEntity.ok(CalculatorSettingsDto.builder()
                .settings(updatedSettings)
                .build());
    }
}
