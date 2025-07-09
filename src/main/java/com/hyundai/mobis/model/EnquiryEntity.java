package com.hyundai.mobis.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "enquiries")
@Data
public class EnquiryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String referenceNumber;
    
    // Type information
    @Column(nullable = false)
    private String itemType; // "accessory" or "part"
    
    @Column(nullable = false)
    private String contactType; // "dealer" or "distributor"
    
    // Dealer/Distributor information
    private String dealerDistributorId;
    private String dealerDistributorName;
    
    @Column(columnDefinition = "TEXT")
    private String dealerDistributorDetails; // Full address/contact as JSON or text
    
    // Location details
    private String stateId;
    private String stateName;
    private String cityId;
    private String cityName;
    
    // Customer information
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String mobileNo;
    
    @Column(columnDefinition = "TEXT")
    private String query; // Optional
    
    // Item specific - Accessory
    private String modelId;
    private String modelName;
    private String accessoryId;
    private String accessoryName;
    
    // Item specific - Part  
    private String partId;
    private String partName;
    
    // Metadata
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private String status; // "submitted", "contacted", "completed"
    
    private String sessionId; // To track which chat session
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "submitted";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}