package com.amzexpress.service;

import com.amzexpress.dto.UserResponse;
import com.amzexpress.entity.Role;
import com.amzexpress.entity.User;
import com.amzexpress.repository.OrderRepository;
import com.amzexpress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityCodeService securityCodeService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return toUserResponse(user);
    }

    public UserResponse updateUserRole(Long userId, Role newRole, String adminEmail) {
        // Verify admin permissions
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        // SUPERADMIN can only be assigned by SUPERADMIN
        if (newRole == Role.SUPERADMIN && admin.getRole() != Role.SUPERADMIN) {
            throw new RuntimeException("Solo un SUPERADMIN puede asignar el rol SUPERADMIN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setRole(newRole);
        if (newRole == Role.ADMIN || newRole == Role.SUPERADMIN) {
            user.setIsVerified(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpires(null);
        }
        user = userRepository.save(user);

        return toUserResponse(user);
    }

    public void deleteUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        if (admin.getRole() != Role.SUPERADMIN) {
            throw new RuntimeException("Solo un SUPERADMIN puede eliminar usuarios");
        }

        userRepository.deleteById(userId);
    }

    public UserResponse createAdmin(String name, String email, String password, String securityCode, String adminEmail) {
        // Verify superadmin permissions
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        if (admin.getRole() != Role.SUPERADMIN) {
            throw new RuntimeException("Solo un SUPERADMIN puede crear administradores");
        }

        securityCodeService.validateOrThrow(
                securityCode,
                "Código de seguridad inválido para crear administradores"
        );

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("El email ya está registrado");
        }

        User newAdmin = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .isVerified(true)
                .build();

        newAdmin = userRepository.save(newAdmin);
        return toUserResponse(newAdmin);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .orderCount(orderRepository.countByUserId(user.getId()))
                .build();
    }
}
