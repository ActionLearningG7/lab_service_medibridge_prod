package com.medibridge.lab_service_medibridge.client.rest;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAddressResponse {
    private String name;
    private String street;
    private String city;
    private String zipCode;
    private String country;
    private Double latitude;
    private Double longitude;
}
