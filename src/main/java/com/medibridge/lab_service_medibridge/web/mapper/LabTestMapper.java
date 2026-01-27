package com.medibridge.lab_service_medibridge.web.mapper;

import com.medibridge.lab_service_medibridge.domain.LabTestCatalog;
import com.medibridge.lab_service_medibridge.web.dto.LabTestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabTestMapper {
    LabTestDto toDto(LabTestCatalog entity);

    LabTestCatalog toEntity(LabTestDto dto);
}
