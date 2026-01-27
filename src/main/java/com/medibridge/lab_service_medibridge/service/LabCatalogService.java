package com.medibridge.lab_service_medibridge.service;

import com.medibridge.lab_service_medibridge.domain.LabTestCatalog;
import com.medibridge.lab_service_medibridge.repository.LabTestCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabCatalogService {

    private final LabTestCatalogRepository repository;

    @Transactional(readOnly = true)
    public List<LabTestCatalog> getAllTests() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public LabTestCatalog getTestByCode(String code) {
        return repository.findByTestCode(code).orElse(null);
    }
}
