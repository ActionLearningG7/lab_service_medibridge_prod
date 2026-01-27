package com.medibridge.lab_service_medibridge.web.mapper;

import com.medibridge.lab_service_medibridge.domain.LabOrder;
import com.medibridge.lab_service_medibridge.domain.LabOrderItem;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderItemDto;
import com.medibridge.lab_service_medibridge.web.dto.LabOrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LabOrderMapper {

    @Mapping(source = "deliveryLat", target = "latitude")
    @Mapping(source = "deliveryLng", target = "longitude")
    LabOrderResponse toResponse(LabOrder entity);

    LabOrderItemDto toItemDto(LabOrderItem entity);
}
