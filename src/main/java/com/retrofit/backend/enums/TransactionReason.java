package com.retrofit.backend.enums;

public enum TransactionReason {
    INITIAL_BALANCE, // Inventario Inicial (INBOUND)
    PURCHASE,        // Compra / Proveedor (INBOUND)
    CONSUMPTION,     // Consumo en Partida (OUTBOUND)
    LOSS             // Merma, pérdida o daño (OUTBOUND)
}