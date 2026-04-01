package com.amzexpress.entity;

public enum OrderStatus {
    // Estados de pago
    PENDIENTE_PAGO,
    PAGADO,
    // Estados de envío
    PEDIDO_REALIZADO,
    LLEGO_BODEGA_USA,
    EN_TRANSITO_ECUADOR,
    LLEGO_BODEGA_ECUADOR,
    LISTO_ENTREGA,
    ENTREGADO,
    // Estado cancelado
    CANCELADO
}
