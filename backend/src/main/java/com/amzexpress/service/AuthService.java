package com.amzexpress.service;

import com.amzexpress.dto.AuthResponse;
import com.amzexpress.dto.LoginRequest;
import com.amzexpress.dto.RegisterRequest;
import com.amzexpress.dto.VerifyCodeRequest;
import com.amzexpress.entity.Role;
import com.amzexpress.entity.User;
import com.amzexpress.repository.UserRepository;
import com.amzexpress.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        String verificationCode = emailService.generateVerificationCode();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isVerified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpires(emailService.getCodeExpiration())
                .verificationCodeSentAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Send verification email asynchronously
        emailService.sendVerificationCode(user.getEmail(), verificationCode);

        // Return response indicating verification needed
        return Map.of(
            "requiresVerification", true,
            "email", user.getEmail(),
            "message", "Se envio un codigo de verificacion a tu correo"
        );
    }

    @Transactional
    public Map<String, Object> verifyCode(VerifyCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getIsVerified() != null && user.getIsVerified()) {
            throw new RuntimeException("El usuario ya esta verificado");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            throw new RuntimeException("Codigo invalido");
        }

        if (user.getVerificationCodeExpires() != null && user.getVerificationCodeExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El codigo ha expirado");
        }

        // Mark as verified and clear code
        user.setIsVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpires(null);
        userRepository.save(user);

        // Generate token
        String token = tokenProvider.generateToken(user.getEmail());

        return Map.of(
            "token", token,
            "userId", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole().name()
        );
    }

    @Transactional
    public Map<String, Object> resendCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Si ya está verificado o es null (usuario antiguo), no enviar código
        if (user.getIsVerified() != null && user.getIsVerified()) {
            throw new RuntimeException("El usuario ya esta verificado");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getVerificationCodeSentAt() != null && user.getVerificationCodeSentAt().plusMinutes(1).isAfter(now)) {
            long secondsLeft = java.time.Duration.between(now, user.getVerificationCodeSentAt().plusMinutes(1)).getSeconds();
            throw new RuntimeException("Debes esperar " + Math.max(secondsLeft, 1) + " segundos para reenviar el codigo");
        }

        String verificationCode = emailService.generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpires(emailService.getCodeExpiration());
        user.setVerificationCodeSentAt(now);
        userRepository.save(user);

        emailService.sendVerificationCode(user.getEmail(), verificationCode);

        return Map.of(
            "message", "Se envio un nuevo codigo de verificacion",
            "email", user.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Solo usuarios normales requieren verificacion por correo
        if (user.getRole() == Role.USER && user.getIsVerified() != null && !user.getIsVerified()) {
            throw new RuntimeException("Debes verificar tu correo antes de iniciar sesion");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public Map<String, Object> requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = generateResetToken();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpires(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);

            emailService.sendPasswordResetLink(user.getEmail(), user.getName(), resetToken);
        }

        return Map.of(
                "message", "Si el correo existe, te enviamos un enlace para restablecer la contraseña."
        );
    }

    public Map<String, Object> validatePasswordResetToken(String token) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElse(null);

        if (user == null || user.getPasswordResetExpires() == null) {
            return Map.of(
                    "valid", false,
                    "message", "El enlace no es válido o ya fue usado."
            );
        }

        if (user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            return Map.of(
                    "valid", false,
                    "message", "El enlace expiró. Solicita uno nuevo."
            );
        }

        return Map.of(
                "valid", true,
                "message", "Token valido"
        );
    }

    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("La contraseña debe tener al menos 6 caracteres");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("El enlace no es válido o ya fue usado."));

        if (user.getPasswordResetExpires() == null || user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El enlace expiró. Solicita uno nuevo.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        return Map.of(
                "message", "Contraseña actualizada correctamente. Ya puedes iniciar sesión."
        );
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
