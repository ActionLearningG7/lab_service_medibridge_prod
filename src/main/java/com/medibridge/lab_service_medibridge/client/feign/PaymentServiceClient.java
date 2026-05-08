package com.medibridge.lab_service_medibridge.client.feign;

import com.medibridge.lab_service_medibridge.client.dto.CheckoutRequest;
import com.medibridge.lab_service_medibridge.client.dto.CheckoutResponse;
import com.medibridge.lab_service_medibridge.client.dto.CreateInvoiceRequest;
import com.medibridge.lab_service_medibridge.client.dto.InvoiceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.feign.payment-service-url:http://localhost:8087/api/v1}")
public interface PaymentServiceClient {

    @PostMapping("/payments/invoices")
    InvoiceDTO createInvoice(@RequestBody CreateInvoiceRequest request);

    @PostMapping("/payments/checkout-link")
    CheckoutResponse getCheckoutLink(@RequestBody CheckoutRequest request);
}
