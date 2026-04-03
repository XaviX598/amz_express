package com.amzexpress.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SecurityCodeService {

    @Value("${app.master-security-code:}")
    private String masterSecurityCode;

    @PostConstruct
    public void validateConfiguration() {
        if (masterSecurityCode == null || masterSecurityCode.isBlank()) {
            throw new IllegalStateException("Falta configurar app.master-security-code");
        }
    }

    public boolean isValid(String providedCode) {
        if (providedCode == null || providedCode.isBlank()) {
            return false;
        }

        byte[] expectedBytes = masterSecurityCode.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedCode.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    public void validateOrThrow(String providedCode, String messageIfInvalid) {
        if (!isValid(providedCode)) {
            throw new RuntimeException(messageIfInvalid);
        }
    }
}
