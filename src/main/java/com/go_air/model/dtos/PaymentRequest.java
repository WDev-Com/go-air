package com.go_air.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String productName; // Name of the product or service
    private Long amount;        // Amount in your base currency (e.g. USD)
}
