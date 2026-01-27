package com.medibridge.lab_service_medibridge.web.controller;

import com.medibridge.lab_service_medibridge.domain.LabTestCatalog;
import com.medibridge.lab_service_medibridge.service.LabCatalogService;
import com.medibridge.lab_service_medibridge.web.dto.LabTestDto;
import com.medibridge.lab_service_medibridge.web.mapper.LabTestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/lab-tests")
@RequiredArgsConstructor
public class CatalogController {

    private final LabCatalogService catalogService;
    private final LabTestMapper mapper;

    /**
     * GET /api/v1/lab-tests
     * Get all lab tests with optional filtering and sorting
     *
     * @param search Search by name or code
     * @param sampleType Filter by sample type (blood, urine, saliva, etc)
     * @param sortBy Sort by: popularity, price, name (default: popularity)
     * @param page Page number (0-indexed, default: 0)
     * @param size Page size (default: 20)
     * @return List of lab tests
     */
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<LabTestDto>> getAllTests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sampleType,
            @RequestParam(required = false) Boolean fastingRequired,
            @RequestParam(defaultValue = "popularity") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Get all tests from service
        var tests = catalogService.getAllTests().stream()
                // Search filter - check name or code
                .filter(test -> search == null ||
                        test.getTestName().toLowerCase().contains(search.toLowerCase()) ||
                        test.getTestCode().toLowerCase().contains(search.toLowerCase()))
                // Sample type filter - compare enum
                .filter(test -> sampleType == null ||
                        (test.getSampleType() != null && test.getSampleType().name().equalsIgnoreCase(sampleType)))
                // Fasting filter
                .filter(test -> fastingRequired == null || test.isFastingRequired() == fastingRequired)
                // Only active tests
                .filter(LabTestCatalog::isActive)
                // Apply sorting
                .sorted((t1, t2) -> {
                    switch (sortBy.toLowerCase()) {
                        case "price":
                            return (t1.getPrice() != null ? t1.getPrice() : BigDecimal.ZERO)
                                    .compareTo(t2.getPrice() != null ? t2.getPrice() : BigDecimal.ZERO);
                        case "name":
                            return (t1.getTestName() != null ? t1.getTestName() : "")
                                    .compareTo(t2.getTestName() != null ? t2.getTestName() : "");
                        case "popularity":
                        default:
                            // Default: return in order (no specific popularity score in domain)
                            return 0;
                    }
                })
                // Apply pagination
                .skip((long) page * size)
                .limit(size)
                // Convert to DTO
                .map(mapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(tests);
    }

    /**
     * GET /api/v1/lab-tests/{testCode}
     * Get specific test by code
     *
     * @param testCode Unique test code (e.g., CBC, THYROID)
     * @return Lab test details
     */
    @GetMapping("/{testCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<LabTestDto> getTest(@PathVariable String testCode) {
        var test = catalogService.getTestByCode(testCode);
        return test != null ? ResponseEntity.ok(mapper.toDto(test)) : ResponseEntity.notFound().build();
    }
}
