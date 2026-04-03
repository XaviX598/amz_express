package com.amzexpress.service;

import com.amzexpress.dto.*;
import com.amzexpress.entity.*;
import com.amzexpress.repository.OrderRepository;
import com.amzexpress.repository.OrderStatusHistoryRepository;
import com.amzexpress.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CustomUserDetailsService userDetailsService;
    private final CalculatorSettingsService settingsService;
    private final EmailService emailService;
    private final SecurityCodeService securityCodeService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String userEmail) {
        securityCodeService.validateOrThrow(
                request.getSecurityCode(),
                "Código de seguridad inválido para comprar"
        );

        User user = userDetailsService.getUserByEmail(userEmail);

        // Calculate pricing
        PricingCalculationResponse pricing = calculatePricing(
                request.getProductPrice(),
                request.getWeight(),
                request.getShippingOption().name()
        );

        // Determine initial status based on payment method and payment confirmation
        OrderStatus initialStatus = determineInitialStatus(request);

        Order order = Order.builder()
                .user(user)
                .productName(request.getProductName())
                .productAsin(request.getProductAsin())
                .productPrice(request.getProductPrice())
                .weight(request.getWeight())
                .shippingOption(request.getShippingOption())
                .paymentMethod(request.getPaymentMethod())
                .paymentReference(request.getPaymentReference())
                .amazonUrl(request.getAmazonUrl())
                .notes(request.getNotes())
                .status(initialStatus)
                .totalPrice(pricing.getTotalPrice())
                .build();

        order = orderRepository.save(order);

        // Create initial status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(initialStatus)
                .build();
        statusHistoryRepository.save(history);

        return toOrderResponse(order);
    }

    private OrderStatus determineInitialStatus(CreateOrderRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.PAYPAL) {
            String paymentReference = request.getPaymentReference();
            if (paymentReference != null && !paymentReference.isBlank()) {
                return OrderStatus.PAGADO;
            }
        }
        return OrderStatus.PENDIENTE_PAGO;
    }

    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userDetailsService.getUserByEmail(userEmail);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long id, String userEmail) {
        User user = userDetailsService.getUserByEmail(userEmail);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // Verify ownership unless admin
        if (!order.getUser().getId().equals(user.getId()) 
                && user.getRole() == Role.USER) {
            throw new RuntimeException("No tienes permiso para ver esta orden");
        }

        return toOrderResponse(order);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, String adminEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        boolean statusChanged = !order.getStatus().equals(request.getStatus());
        String oldNotes = order.getNotes();
        String newNotes = request.getNotes();
        boolean notesChanged = (newNotes != null && !newNotes.equals(oldNotes)) || (newNotes == null && oldNotes != null);

        order.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            order.setNotes(request.getNotes());
        }
        order = orderRepository.save(order);

        // Create status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(request.getStatus())
                .build();
        statusHistoryRepository.save(history);

        // Send email notifications to user
        User user = order.getUser();
        
        // Send status change notification
        if (statusChanged) {
            emailService.sendOrderStatusUpdate(
                user.getEmail(),
                user.getName(),
                order.getId(),
                order.getProductName() != null ? order.getProductName() : "Producto Amazon",
                request.getStatus(),
                order.getTotalPrice(),
                order.getShippingOption()
            );
        }
        
        // Send observation notification if notes were added/updated
        if (notesChanged && newNotes != null && !newNotes.isBlank()) {
            emailService.sendObservationNotification(
                user.getEmail(),
                user.getName(),
                order.getId(),
                newNotes
            );
        }

        return toOrderResponse(order);
    }
    @Transactional
    public void submitTransferProof(
            String userEmail,
            List<Long> orderIds,
            String bankCode,
            String bankName,
            String accountNumber,
            String accountHolder,
            MultipartFile proofFile) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new RuntimeException("No se enviaron pedidos para el comprobante");
        }
        if (bankCode == null || bankCode.isBlank()) {
            throw new RuntimeException("Debes seleccionar un banco");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new RuntimeException("Banco invalido");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new RuntimeException("Cuenta invalida");
        }
        if (accountHolder == null || accountHolder.isBlank()) {
            throw new RuntimeException("Titular invalido");
        }
        if (proofFile == null || proofFile.isEmpty()) {
            throw new RuntimeException("Debes adjuntar la imagen del comprobante");
        }

        String contentType = proofFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("El comprobante debe ser una imagen");
        }

        User user = userDetailsService.getUserByEmail(userEmail);
        List<Order> orders = orderRepository.findAllById(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new RuntimeException("Una o mas ordenes no existen");
        }

        for (Order order : orders) {
            if (!Objects.equals(order.getUser().getId(), user.getId()) && user.getRole() == Role.USER) {
                throw new RuntimeException("No tienes permiso para adjuntar comprobantes en esta orden");
            }
            if (order.getPaymentMethod() != PaymentMethod.TRANSFERENCIA) {
                throw new RuntimeException("Solo se permiten comprobantes para pagos por transferencia");
            }
            if (order.getStatus() != OrderStatus.PENDIENTE_PAGO) {
                throw new RuntimeException("La orden #" + order.getId() + " ya no esta pendiente de pago");
            }

            order.setPaymentReference("Comprobante (" + bankCode + ")");
            String proofNote = "Comprobante enviado. Banco: " + bankName + " - Cuenta: " + accountNumber + " - Titular: " + accountHolder;
            if (order.getNotes() == null || order.getNotes().isBlank()) {
                order.setNotes(proofNote);
            } else if (!order.getNotes().contains("Comprobante enviado")) {
                order.setNotes(order.getNotes() + "\n" + proofNote);
            }
        }

        orderRepository.saveAll(orders);
        List<EmailService.TransferProofOrderItem> orderItems = orders.stream()
                .map(order -> new EmailService.TransferProofOrderItem(
                        order.getId(),
                        order.getProductName(),
                        order.getTotalPrice(),
                        order.getWeight()
                ))
                .toList();

        emailService.sendTransferProofToAdmin(
                user.getName(),
                user.getEmail(),
                orderIds,
                orderItems,
                bankName,
                accountNumber,
                accountHolder,
                proofFile
        );
    }
    public PricingCalculationResponse calculatePricing(BigDecimal productPrice, BigDecimal weight, String shippingOption) {
        BigDecimal taxRate = settingsService.getTaxRate();
        BigDecimal handlingRate = settingsService.getHandlingRate();
        BigDecimal shippingRatePerLb = settingsService.getShippingRatePerLb();
        BigDecimal maxShipping = settingsService.getMaxShipping();
        BigDecimal customsFee = settingsService.getCustomsFee();
        BigDecimal maxWeight = settingsService.getMaxWeightForStandard();
        BigDecimal maxPrice = settingsService.getMaxPriceForStandard();
        
        BigDecimal taxes = productPrice.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal handling = productPrice.multiply(handlingRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Shipping calculation
        BigDecimal shippingCost = BigDecimal.ZERO;
        if (weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            shippingCost = weight.multiply(shippingRatePerLb)
                    .setScale(2, RoundingMode.HALF_UP);
            if (shippingCost.compareTo(maxShipping) > 0) {
                shippingCost = maxShipping;
            }
        }

        // Calculate total
        BigDecimal total = productPrice
                .add(taxes)
                .add(handling)
                .add(customsFee)
                .add(shippingCost);

        // Check Category C
        boolean isCategoryC = (weight != null && weight.compareTo(maxWeight) > 0)
                || productPrice.compareTo(maxPrice) > 0;

        String categoryCMessage = null;
        if (isCategoryC) {
            String whatsappNumber = settingsService.getWhatsappNumber();
            categoryCMessage = "Categoría C: Peso > " + maxWeight + " lbs o valor > $" + maxPrice + 
                    ". Contactar por WhatsApp (" + whatsappNumber + ") para cotización personalizada.";
        }

        // Get shipping description
        String shippingDescription = "";
        if (shippingOption != null) {
            try {
                ShippingOption option = ShippingOption.valueOf(shippingOption);
                shippingDescription = option.getDescription();
            } catch (IllegalArgumentException e) {
                shippingDescription = shippingOption;
            }
        }

        return PricingCalculationResponse.builder()
                .productPrice(productPrice)
                .taxes(taxes)
                .handling(handling)
                .customs(customsFee)
                .shippingCost(shippingCost)
                .totalPrice(total.setScale(2, RoundingMode.HALF_UP))
                .categoryC(isCategoryC)
                .categoryCMessage(categoryCMessage)
                .shippingDescription(shippingDescription)
                .build();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderStatusHistory> history = statusHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .userEmail(order.getUser().getEmail())
                .productName(order.getProductName())
                .productAsin(order.getProductAsin())
                .productPrice(order.getProductPrice())
                .weight(order.getWeight())
                .shippingOption(order.getShippingOption())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentReference(order.getPaymentReference())
                .totalPrice(order.getTotalPrice())
                .notes(order.getNotes())
                .amazonUrl(order.getAmazonUrl())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .statusHistory(history.stream()
                        .map(h -> OrderResponse.StatusHistoryItem.builder()
                                .id(h.getId())
                                .status(h.getStatus())
                                .createdAt(h.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

