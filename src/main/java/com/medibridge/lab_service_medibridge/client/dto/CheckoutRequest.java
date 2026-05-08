package com.medibridge.lab_service_medibridge.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private Long invoiceId;
    private String successUrl;
    private String cancelUrl;
}
