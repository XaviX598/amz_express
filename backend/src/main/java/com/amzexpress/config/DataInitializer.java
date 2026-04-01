package com.amzexpress.config;

import com.amzexpress.entity.*;
import com.amzexpress.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CalculatorSettingsRepository settingsRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-data:false}")
    private boolean seedDemoData;

    private static final String SUPERADMIN_EMAIL = "admin@amzexpress.com";
    private static final String SUPERADMIN_PASSWORD = "SuperAdmin123!";
    private static final String SUPERADMIN_NAME = "Super Administrador";
    
    // Admin
    private static final String ADMIN_EMAIL = "admin.soporte@amzexpress.com";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String ADMIN_NAME = "Administrador";

    // Test user
    private static final String TEST_USER_EMAIL = "usuario@amzexpress.com";
    private static final String TEST_USER_PASSWORD = "Usuario123!";
    private static final String TEST_USER_NAME = "usuario";
    
    private static final Map<String, String> DEFAULT_SETTINGS = Map.of(
            "TAX_RATE", "0.15",
            "HANDLING_RATE", "0.0927",
            "SHIPPING_RATE_PER_LB", "5",
            "MAX_SHIPPING", "40",
            "CUSTOMS_FEE", "21",
            "MAX_WEIGHT_FOR_STANDARD", "8",
            "MAX_PRICE_FOR_STANDARD", "400",
            "WHATSAPP_NUMBER", "593985295277"
    );

    private record SampleUser(String name, String email, String password) {}

    private static final List<SampleUser> SAMPLE_USERS = List.of(
            new SampleUser("Carlos Mendoza", "carlos.mendoza@gmail.com", "Usuario123!"),
            new SampleUser("María García", "maria.garcia@hotmail.com", "Usuario123!"),
            new SampleUser("Andrés López", "andres.lopez@yahoo.com", "Usuario123!")
    );

    private record SampleOrder(String productName, String asin, BigDecimal price, BigDecimal weight, ShippingOption shipping, OrderStatus status) {}

    private static final List<SampleOrder> SAMPLE_ORDERS = List.of(
            new SampleOrder("Apple AirPods Pro", "B0BDHWDR12", new BigDecimal("249.99"), new BigDecimal("0.51"), ShippingOption.SERVIENTREGA, OrderStatus.ENTREGADO),
            new SampleOrder("Kindle Paperwhite", "B09XRRCTZ1", new BigDecimal("149.99"), new BigDecimal("0.45"), ShippingOption.PICKUP, OrderStatus.LLEGO_BODEGA_ECUADOR),
            new SampleOrder("Samsung Galaxy Watch", "B0C4H3W2VX", new BigDecimal("349.99"), new BigDecimal("1.10"), ShippingOption.UBER, OrderStatus.EN_TRANSITO_ECUADOR),
            new SampleOrder("Sony WH-1000XM5", "B0B6J86J3K", new BigDecimal("349.99"), new BigDecimal("0.82"), ShippingOption.SERVIENTREGA, OrderStatus.PEDIDO_REALIZADO)
    );

    @Override
    public void run(String... args) {
        // SuperAdmin
        if (!userRepository.existsByEmail(SUPERADMIN_EMAIL)) {
            User superAdmin = User.builder()
                    .name(SUPERADMIN_NAME)
                    .email(SUPERADMIN_EMAIL)
                    .password(passwordEncoder.encode(SUPERADMIN_PASSWORD))
                    .role(Role.SUPERADMIN)
                    .isVerified(true)
                    .build();
            userRepository.save(superAdmin);
            log.info("SuperAdmin created: {}", SUPERADMIN_EMAIL);
        }
        
        // Admin user
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            User admin = User.builder()
                    .name(ADMIN_NAME)
                    .email(ADMIN_EMAIL)
                    .password(passwordEncoder.encode(ADMIN_PASSWORD))
                    .role(Role.ADMIN)
                    .isVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin created: {}", ADMIN_EMAIL);
        }

        // Test user
        if (!userRepository.existsByEmail(TEST_USER_EMAIL)) {
            User testUser = User.builder()
                    .name(TEST_USER_NAME)
                    .email(TEST_USER_EMAIL)
                    .password(passwordEncoder.encode(TEST_USER_PASSWORD))
                    .role(Role.USER)
                    .isVerified(true)
                    .build();
            userRepository.save(testUser);
            log.info("Test user created: {}", TEST_USER_EMAIL);
        }

        // Ensure all admin/superadmin accounts can log in without email verification
        normalizePrivilegedUsersVerification();
        
        // Settings
        for (Map.Entry<String, String> entry : DEFAULT_SETTINGS.entrySet()) {
            if (!settingsRepository.existsBySettingKey(entry.getKey())) {
                CalculatorSettings setting = CalculatorSettings.builder()
                        .settingKey(entry.getKey())
                        .settingValue(entry.getValue())
                        .build();
                settingsRepository.save(setting);
            }
        }

        if (!seedDemoData) {
            log.info("Demo data seed disabled (app.seed.demo-data=false)");
            return;
        }

        // Sample users with 4 orders each
        for (SampleUser sampleUser : SAMPLE_USERS) {
            if (!userRepository.existsByEmail(sampleUser.email())) {
                User kevin = User.builder()
                        .name(sampleUser.name())
                        .email(sampleUser.email())
                        .password(passwordEncoder.encode(sampleUser.password()))
                        .role(Role.USER)
                        .isVerified(true)
                        .build();
                kevin = userRepository.save(kevin);
                log.info("Sample user created: {}", sampleUser.email());

                LocalDateTime baseDate = LocalDateTime.now().minusDays(10);
                for (int i = 0; i < SAMPLE_ORDERS.size(); i++) {
                    SampleOrder so = SAMPLE_ORDERS.get(i);
                    BigDecimal taxes = so.price().multiply(new BigDecimal("0.15")).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal handling = so.price().multiply(new BigDecimal("0.0927")).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal shippingCost = so.weight().multiply(new BigDecimal("5")).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal total = so.price().add(taxes).add(handling).add(new BigDecimal("21")).add(shippingCost);

                    Order order = Order.builder()
                            .user(kevin)
                            .productName(so.productName())
                            .productAsin(so.asin())
                            .productPrice(so.price())
                            .weight(so.weight())
                            .shippingOption(so.shipping())
                            .status(so.status())
                            .totalPrice(total)
                            .amazonUrl("https://www.amazon.com/dp/" + so.asin())
                            .createdAt(baseDate.plusDays(i * 2L))
                            .build();
                    orderRepository.save(order);
                }
            }
        }
    }

    private void normalizePrivilegedUsersVerification() {
        List<User> users = userRepository.findAll();
        int updated = 0;
        for (User user : users) {
            if ((user.getRole() == Role.ADMIN || user.getRole() == Role.SUPERADMIN)
                    && (user.getIsVerified() == null || !user.getIsVerified())) {
                user.setIsVerified(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpires(null);
                userRepository.save(user);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Privileged users normalized (verification bypass): {}", updated);
        }
    }
}
