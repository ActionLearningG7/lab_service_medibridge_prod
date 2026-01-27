package com.medibridge.lab_service_medibridge.web.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LabOrderItemDto {
    private String testCode;
    private String testName;
    private BigDecimal price;
    private boolean fastingRequired;
}
