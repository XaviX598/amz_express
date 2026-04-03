package com.amzexpress.controller;

import com.amzexpress.dto.SecurityCodeRequest;
import com.amzexpress.service.SecurityCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/security-code")
@RequiredArgsConstructor
public class SecurityCodeController {

    private final SecurityCodeService securityCodeService;

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@Valid @RequestBody SecurityCodeRequest request) {
        boolean valid = securityCodeService.isValid(request.getSecurityCode());
        if (!valid) {
            throw new RuntimeException("Código de seguridad inválido");
        }
        return ResponseEntity.ok(Map.of("valid", true));
    }
}
