package com.amzexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeRequest {
    @NotBlank(message = "El email es requerido")
    private String email;

    @NotBlank(message = "El codigo es requerido")
    @Size(min = 6, max = 6, message = "El codigo debe tener 6 digitos")
    private String code;
}
