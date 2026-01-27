package com.medibridge.lab_service_medibridge.web.dto;

import com.medibridge.lab_service_medibridge.domain.enums.SampleType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LabTestDto {
    private String testCode;
    private String testName;
    private String description;
    private SampleType sampleType;
    private boolean fastingRequired;
    private Integer expectedReportTimeHours;
    private BigDecimal price;
    private boolean active;
}
