package com.medibridge.lab_service_medibridge.web.dto;

import com.medibridge.lab_service_medibridge.domain.enums.LabReportStatus;
import lombok.Data;

@Data
public class ReportPublishRequest {
    private String reportUrl;
    private String reportDataJson;
    private LabReportStatus status;
}
