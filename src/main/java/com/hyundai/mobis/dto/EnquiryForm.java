package com.hyundai.mobis.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EnquiryForm {
    // Common fields
    private String itemType; // "accessory" or "part"
    private String dealerOrDistributor; // "dealer" or "distributor"
    private String dealerDistributorId;
    private String dealerDistributorName;
    private String dealerDistributorDetails; // Full address/contact
    
    // Customer details
    private String customerName;
    private String email;
    private String mobileNo;
    private String query; // Optional
    
    // Accessory specific
    private String model;
    private String modelId;
    private String accessoryName;
    private String accessoryId;
    
    // Part specific
    private String partName;
    private String partId;
    

    private String stateId;
    private String stateName;
    private String cityId;
    private String cityName;
    
    // Metadata
    private LocalDateTime createdAt;
    private String referenceNumber;
    private String status; // "draft", "submitted"
    
    // Helper methods
    public boolean isComplete() {
        boolean hasRequiredFields = dealerDistributorName != null && 
                                   customerName != null && 
                                   email != null && 
                                   mobileNo != null;
        
        if ("accessory".equals(itemType)) {
            return hasRequiredFields && model != null && accessoryName != null;
        } else {
            return hasRequiredFields && partName != null;
        }
    }
}