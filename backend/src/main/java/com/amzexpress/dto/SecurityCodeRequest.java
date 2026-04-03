package com.amzexpress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityCodeRequest {

    @NotBlank(message = "El código de seguridad es obligatorio")
    private String securityCode;
}
