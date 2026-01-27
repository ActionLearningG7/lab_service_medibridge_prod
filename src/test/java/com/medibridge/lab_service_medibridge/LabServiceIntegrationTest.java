package com.medibridge.lab_service_medibridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibridge.lab_service_medibridge.domain.*;
import com.medibridge.lab_service_medibridge.domain.enums.*;
import com.medibridge.lab_service_medibridge.domain.model.SampleCollectionRequest;
import com.medibridge.lab_service_medibridge.repository.*;
import com.medibridge.lab_service_medibridge.domain.repository.CollectionTaskRepository;
import com.medibridge.lab_service_medibridge.service.CollectionTaskService;
import com.medibridge.lab_service_medibridge.util.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LabServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LabOrderRepository labOrderRepository;

    @Autowired
    private CollectionTaskRepository collectionTaskRepository;

    @Autowired
    private CollectionTaskService collectionTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private String patientToken;
    private String doctorToken;
    private String phlebToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        patientToken = generateToken("patient123", "PATIENT");
        doctorToken = generateToken("doctor456", "DOCTOR");
        phlebToken = generateToken("phleb789", "PHLEBOTOMIST");
        adminToken = generateToken("admin000", "ADMIN");

        collectionTaskRepository.deleteAll();
        labOrderRepository.deleteAll();
    }

    private String generateToken(String userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void fullLogisticsFlow() throws Exception {
        // 1. Create Patient Lab Order
        LabOrder order = new LabOrder();
        order.setPatientId("patient123");
        order.setDoctorId("doctor456");
        order.setOrderNumber("ORD-TEST-001");
        order.setStatus(LabOrderStatus.CREATED);
        order.setPreferredSlotStart(LocalDateTime.now().plusDays(1));
        order = labOrderRepository.save(order);

        // Auto-create task (usually done by service hook, triggering manually for test
        // speed if async is involved)
        collectionTaskService.createTaskFromOrder(order);

        UUID taskId = collectionTaskRepository.findByLabOrderId(order.getId()).get().getId();

        // 2. Admin Assigns Phlebotomist
        mockMvc.perform(post("/api/v1/admin/lab/tasks/" + taskId + "/assign")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phlebotomistId\":\"phleb789\"}"))
                .andExpect(status().isOk());

        // 3. Phlebotomist Accepts
        mockMvc.perform(post("/api/v1/phleb/tasks/" + taskId + "/accept")
                .header("Authorization", "Bearer " + phlebToken))
                .andExpect(status().isOk());

        // 4. Phlebotomist Collects Sample
        SampleCollectionRequest collectionRequest = new SampleCollectionRequest();
        collectionRequest.setOtpVerified(true);
        collectionRequest.setTemperatureCelsius(4.0);

        mockMvc.perform(post("/api/v1/phleb/tasks/" + taskId + "/collect")
                .header("Authorization", "Bearer " + phlebToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(collectionRequest)))
                .andExpect(status().isOk());

        // 5. Verify Order Status Updated
        mockMvc.perform(get("/api/v1/patient/lab-orders/" + order.getId() + "/tracking")
                .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAMPLES_COLLECTED"));
    }
}
