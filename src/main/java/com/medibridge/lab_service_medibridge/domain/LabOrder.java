package com.medibridge.lab_service_medibridge.domain;

import com.medibridge.lab_service_medibridge.domain.enums.CollectionType;
import com.medibridge.lab_service_medibridge.domain.enums.LabOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "lab_orders", indexes = {
        @Index(name = "idx_order_number", columnList = "orderNumber", unique = true),
        @Index(name = "idx_patient_id", columnList = "patientId"),
        @Index(name = "idx_doctor_id", columnList = "doctorId"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 36)
    private String patientId;

    @Column(length = 36)
    private String doctorId; // Nullable if self-booked

    @Column(length = 36)
    private String appointmentId; // Optional link

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LabOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionType collectionType;

    private LocalDateTime preferredSlotStart;
    private LocalDateTime preferredSlotEnd;

    // Address Snapshot (Embeddable or plain fields - using plain for simplicity)
    private String addressLine1;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    @Column(name = "delivery_lat", precision = 10, scale = 7)
    private BigDecimal deliveryLat;

    @Column(name = "delivery_lng", precision = 10, scale = 7)
    private BigDecimal deliveryLng;

    private String contactPhone;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    // Total price of order (sum of all items)
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<LabOrderItem> items = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(length = 36)
    private String createdBy; // User ID

    @Version
    private Long version;

    @Column(name = "invoice_id", length = 50)
    private String invoiceId;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // e.g., PENDING, PAID, FAILED

    public void addItem(LabOrderItem item) {
        items.add(item);
        item.setLabOrder(this);
        updateTotalPrice();
    }

    /**
     * Calculate total price from all items
     */
    public void updateTotalPrice() {
        if (items == null || items.isEmpty()) {
            this.totalPrice = BigDecimal.ZERO;
        } else {
            this.totalPrice = items.stream()
                    .map(LabOrderItem::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * Get total price of order
     */
    public BigDecimal getTotalPrice() {
        if (this.totalPrice != null) {
            return this.totalPrice;
        }
        updateTotalPrice();
        return this.totalPrice;
    }
}
