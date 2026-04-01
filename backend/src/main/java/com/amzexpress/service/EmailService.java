package com.amzexpress.service;

import com.amzexpress.entity.OrderStatus;
import com.amzexpress.entity.ShippingOption;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    private static final String FROM_EMAIL = "kevinjkevps4@gmail.com";

    @Value("${app.admin.notifications.email:kevinjkevps4@gmail.com}")
    private String adminNotificationEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public record TransferProofOrderItem(
            Long orderId,
            String productName,
            BigDecimal totalPrice,
            BigDecimal weight
    ) {}

    @Async
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject("Codigo de verificacion - Amz Express");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 500px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; }
                        .header { background: #14b8a6; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; text-align: center; }
                        .code { font-size: 32px; font-weight: bold; color: #14b8a6; letter-spacing: 8px; margin: 20px 0; }
                        .footer { background: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">Amz Express</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333; margin-top: 0;">Verifica tu correo</h2>
                            <p style="color: #666; font-size: 14px;"> Usa el siguiente codigo para verificar tu cuenta:</p>
                            <div class="code">%s</div>
                            <p style="color: #999; font-size: 12px;">Este codigo expira en 15 minutos</p>
                        </div>
                        <div class="footer">
                            <p style="margin: 0;">Si no solicitaste este codigo, puedes ignorar este correo.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(code);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification code sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetLink(String to, String userName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject("Recuperar contraseña - Amz Express");

            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 500px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; }
                        .header { background: #14b8a6; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; }
                        .button { display: inline-block; background: #14b8a6; color: #ffffff !important; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 12px; }
                        .footer { background: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">Amz Express</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333; margin-top: 0;">Recuperación de contraseña</h2>
                            <p style="color: #666; font-size: 14px;">Hola %s, recibimos una solicitud para restablecer tu contraseña.</p>
                            <p style="color: #666; font-size: 14px;">Haz clic en el botón para crear una nueva contraseña:</p>
                            <div style="text-align: center;">
                                <a href="%s" class="button">Cambiar contraseña</a>
                            </div>
                            <p style="color: #999; font-size: 12px; margin-top: 16px;">Este enlace expira en 15 minutos.</p>
                            <p style="color: #999; font-size: 12px;">Si no solicitaste este cambio, puedes ignorar este correo.</p>
                        </div>
                        <div class="footer">
                            <p style="margin: 0;">Amz Express</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName != null ? userName : "usuario", resetUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset link sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset link to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOrderStatusUpdate(String to, String userName, Long orderId, String productName,
                                       OrderStatus newStatus, BigDecimal totalPrice, ShippingOption shippingOption) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject("Tu pedido #" + orderId + " ha sido actualizado - Amz Express");

            String statusMessage = getStatusMessage(newStatus, shippingOption);
            String statusIcon = getStatusIcon(newStatus);
            String statusColor = getStatusColor(newStatus);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 500px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; }
                        .header { background: #14b8a6; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; }
                        .status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-size: 14px; font-weight: bold; margin: 10px 0; }
                        .order-info { background: #f8f8f8; padding: 15px; border-radius: 8px; margin: 15px 0; }
                        .order-info p { margin: 5px 0; color: #555; }
                        .footer { background: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #666; }
                        .button { display: inline-block; background: #14b8a6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 15px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">Amz Express</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333; margin-top: 0;">Hola %s</h2>
                            <p style="color: #666; font-size: 14px;">Te informamos que el estado de tu pedido ha cambiado:</p>
                            
                            <div style="text-align: center;">
                                <span class="status-badge" style="background: %s; color: white;">
                                    %s %s
                                </span>
                            </div>
                            
                            <div class="order-info">
                                <p><strong>Pedido #%d</strong></p>
                                <p>%s</p>
                                <p style="font-size: 16px; color: #14b8a6;"><strong>Total: $%s</strong></p>
                            </div>
                            
                            <p style="color: #666; font-size: 14px;">%s</p>
                            
                            <div style="text-align: center;">
                                <a href="http://localhost:5173/ordenes/%d" class="button">Ver mis pedidos</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p style="margin: 0;">Si no solicitaste este correo, puedes ignorarlo.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    userName,
                    statusColor,
                    statusIcon,
                    getStatusLabel(newStatus),
                    orderId,
                    productName,
                    totalPrice.toString(),
                    statusMessage,
                    orderId
                );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Order status update sent to: {} for order: {}", to, orderId);
        } catch (MessagingException e) {
            log.error("Failed to send order status email to {}: {}", to, e.getMessage());
        }
    }

    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case PENDIENTE_PAGO -> "Pendiente de Pago";
            case PAGADO -> "Pagado";
            case PEDIDO_REALIZADO -> "Pedido Realizado";
            case LLEGO_BODEGA_USA -> "Llegó a bodega USA";
            case EN_TRANSITO_ECUADOR -> "En tránsito a Ecuador";
            case LLEGO_BODEGA_ECUADOR -> "Llegó a Ecuador";
            case LISTO_ENTREGA -> "Listo para entrega";
            case ENTREGADO -> "Entregado";
            case CANCELADO -> "Cancelado";
        };
    }

    private String getStatusMessage(OrderStatus status, ShippingOption shippingOption) {
        return switch (status) {
            case PENDIENTE_PAGO -> "Tu pedido esta pendiente de pago. Por favor realiza el pago para continuar.";
            case PAGADO -> "Tu pago ha sido confirmado. Tu pedido esta en proceso.";
            case PEDIDO_REALIZADO -> "Tu pedido ha sido confirmado y esta en proceso.";
            case LLEGO_BODEGA_USA -> "Tu pedido ha llegado a nuestra bodega en USA. Pronto sera enviado a Ecuador.";
            case EN_TRANSITO_ECUADOR -> "Tu pedido esta en camino hacia Ecuador. Mantente atento a las actualizaciones.";
            case LLEGO_BODEGA_ECUADOR -> "Tu pedido ha llegado a Ecuador. Esta listo para ser entregado.";
            case LISTO_ENTREGA -> shippingOption == ShippingOption.PICKUP
                    ? "Tu pedido esta listo para retiro en bodega. Direccion: Alfonso Mora ss23 y Quero, en rompevelocidades."
                    : "Tu pedido esta listo para ser entregado. Nos contactaremos contigo para coordinar la entrega.";
            case ENTREGADO -> "Tu pedido ha sido entregado. Gracias por confiar en Amz Express. Esperamos que disfrutes tu producto!";
            case CANCELADO -> "Tu pedido ha sido cancelado.";
        };
    }

    private String getStatusIcon(OrderStatus status) {
        return switch (status) {
            case PENDIENTE_PAGO -> "⏳";
            case PAGADO -> "💳";
            case PEDIDO_REALIZADO -> "📦";
            case LLEGO_BODEGA_USA -> "🏭";
            case EN_TRANSITO_ECUADOR -> "✈️";
            case LLEGO_BODEGA_ECUADOR -> "🇪🇨";
            case LISTO_ENTREGA -> "🚚";
            case ENTREGADO -> "✅";
            case CANCELADO -> "❌";
        };
    }

    private String getStatusColor(OrderStatus status) {
        return switch (status) {
            case PENDIENTE_PAGO -> "#f59e0b";
            case PAGADO -> "#10b981";
            case PEDIDO_REALIZADO -> "#3b82f6";
            case LLEGO_BODEGA_USA -> "#f59e0b";
            case EN_TRANSITO_ECUADOR -> "#f97316";
            case LLEGO_BODEGA_ECUADOR -> "#8b5cf6";
            case LISTO_ENTREGA -> "#10b981";
            case ENTREGADO -> "#14b8a6";
            case CANCELADO -> "#ef4444";
        };
    }

    public String generateVerificationCode() {
        int code = random.nextInt(900000) + 100000; // 100000 to 999999
        return String.valueOf(code);
    }

    public LocalDateTime getCodeExpiration() {
        return LocalDateTime.now().plusMinutes(15);
    }

    @Async
    public void sendObservationNotification(String to, String userName, Long orderId, String observation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject("Nueva observación en tu pedido #" + orderId + " - Amz Express");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 500px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; }
                        .header { background: #14b8a6; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; }
                        .observation-box { background: #f0f9ff; border-left: 4px solid #14b8a6; padding: 15px; margin: 15px 0; border-radius: 4px; }
                        .observation-text { color: #333; font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
                        .footer { background: #f9f9f9; padding: 15px; text-align: center; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin: 0;">Amz Express</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333; margin-top: 0;">Nueva observación</h2>
                            <p style="color: #666; font-size: 14px;">Hola %s,</p>
                            <p style="color: #666; font-size: 14px;">El administrador ha agregado una observación a tu pedido #%d:</p>
                            
                            <div class="observation-box">
                                <p class="observation-text">%s</p>
                            </div>
                            
                            <p style="color: #666; font-size: 14px;">Puedes ver más detalles de tu pedido en la plataforma.</p>
                            
                            <div style="text-align: center; margin-top: 20px;">
                                <a href="http://localhost:5173/ordenes/%d" style="display: inline-block; background: #14b8a6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px;">Ver mi pedido</a>
                            </div>
                        </div>
                        <div class="footer">
                            <p style="margin: 0;">Si no solicitaste este correo, puedes ignorarlo.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, orderId, observation, orderId);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Observation notification sent to: {} for order: {}", to, orderId);
        } catch (MessagingException e) {
            log.error("Failed to send observation notification to {}: {}", to, e.getMessage());
        }
    }
    @Async
    public void sendTransferProofToAdmin(
            String customerName,
            String customerEmail,
            List<Long> orderIds,
            List<TransferProofOrderItem> orderItems,
            String bankName,
            String accountNumber,
            String accountHolder,
            MultipartFile proofFile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(adminNotificationEmail);
            helper.setSubject("Nuevo comprobante de transferencia - " + orderIds.size() + " paquete(s)");

            List<TransferProofOrderItem> safeOrderItems = orderItems != null ? orderItems : List.of();
            BigDecimal totalAmount = safeOrderItems.stream()
                    .map(TransferProofOrderItem::totalPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String orderDetailsHtml = safeOrderItems.stream()
                    .map(item -> """
                        <tr>
                            <td style="padding: 8px; border: 1px solid #e5e7eb;">#%s</td>
                            <td style="padding: 8px; border: 1px solid #e5e7eb;">%s</td>
                            <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">%s</td>
                            <td style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">%s lbs</td>
                        </tr>
                        """.formatted(
                            item.orderId(),
                            escapeHtml(item.productName()),
                            formatUsd(item.totalPrice()),
                            item.weight() != null ? item.weight().setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00"
                    ))
                    .collect(Collectors.joining());

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 720px; margin: 30px auto; background: #ffffff; border-radius: 8px; overflow: hidden; }
                        .header { background: #14b8a6; color: white; padding: 20px; text-align: center; }
                        .content { padding: 24px; color: #333; }
                        .box { background: #f8f8f8; padding: 12px 14px; border-radius: 6px; margin: 8px 0; }
                        .label { color: #666; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
                        .value { font-size: 14px; margin-top: 4px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin: 0;">Comprobante de Transferencia</h2>
                        </div>
                        <div class="content">
                            <div class="box">
                                <div class="label">Cliente</div>
                                <div class="value">%s (%s)</div>
                            </div>
                            <div class="box">
                                <div class="label">Pedidos</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Numero de paquetes</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Total de compra</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Banco</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Cuenta</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Titular</div>
                                <div class="value">%s</div>
                            </div>
                            <div class="box">
                                <div class="label">Detalle de paquetes</div>
                                <div class="value">
                                    <table style="width: 100%%; border-collapse: collapse; margin-top: 8px;">
                                        <thead>
                                            <tr style="background: #f3f4f6;">
                                                <th style="padding: 8px; border: 1px solid #e5e7eb; text-align: left;">Pedido</th>
                                                <th style="padding: 8px; border: 1px solid #e5e7eb; text-align: left;">Productos</th>
                                                <th style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">Total</th>
                                                <th style="padding: 8px; border: 1px solid #e5e7eb; text-align: right;">Peso</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            %s
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                            <p style="margin-top: 16px; font-size: 13px; color: #666;">
                                El comprobante va adjunto en este correo para revision.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    customerName,
                    customerEmail,
                    orderIds,
                    safeOrderItems.size(),
                    formatUsd(totalAmount),
                    bankName,
                    accountNumber,
                    accountHolder,
                    orderDetailsHtml
                );

            helper.setText(htmlContent, true);

            if (proofFile != null && !proofFile.isEmpty()) {
                String filename = proofFile.getOriginalFilename() != null ? proofFile.getOriginalFilename() : "comprobante.jpg";
                helper.addAttachment(filename, new ByteArrayResource(proofFile.getBytes()));
            }

            mailSender.send(message);
            log.info("Transfer proof notification sent to admin for orders {}", orderIds);
        } catch (Exception e) {
            log.error("Failed to send transfer proof email to admin: {}", e.getMessage());
        }
    }

    private String formatUsd(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return "$" + safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeHtml(String raw) {
        if (raw == null) return "";
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

