package com.medibridge.lab_service_medibridge.client.rest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import com.fasterxml.jackson.databind.JsonNode;

@FeignClient(name = "user-service", url = "${application.config.user-service-url:http://localhost:8081}")
public interface UserServiceClient {

    @GetMapping("/api/v1/organization/address")
    // Returning JsonNode creates flexibility to parse the "data" field manually
    // without duplicating the full ApiResponse<T> class structure across services
    // for now.
    JsonNode getOrganizationAddress();
}
