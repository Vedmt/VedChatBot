package com.hyundai.mobis.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class SessionData {
    private String sessionId;
    private String currentState; // Current conversation state
    private Map<String, Object> context = new HashMap<>();
    
    // Flow tracking
    private boolean isAccessoryFlow;
    private String flowStage; // "MODEL", "TYPE", "SUBTYPE", "PRODUCT", etc.
    
    // Selected items
    private ModelInfo selectedModel;
    private AccessoryTypeInfo selectedType;
    private AccessorySubTypeInfo selectedSubType;
    private AccessoryInfo selectedAccessory;
    private PartTypeInfo selectedPartType;
    private PartInfo selectedPart;
    
    // Enquiry related fields
    private EnquiryForm enquiryForm;
    private DealerDistributorFlow dealerDistributorFlow;
    private String enquiryStage; // "INIT", "DEALER_DISTRIBUTOR", "CONTACT_DETAILS", "QUERY", "REVIEW", "SUBMITTED"
    private boolean hasAskedForQuery;
    
    // Pagination states
    private int currentPage = 0;
    private Map<String, Integer> pageTracking = new HashMap<>();
    
    // Constructor
    public SessionData(String sessionId) {
        this.sessionId = sessionId;
    }
    
    // Helper methods
    public void clearEnquiryForm() {
        this.enquiryForm = null;
        this.dealerDistributorFlow = null;
        this.enquiryStage = null;
        this.hasAskedForQuery = false;
    }
    
    public boolean isInEnquiryFlow() {
        return enquiryForm != null || enquiryStage != null;
    }
    
    public void resetFlow() {
        this.flowStage = null;
        this.selectedModel = null;
        this.selectedType = null;
        this.selectedSubType = null;
        this.selectedAccessory = null;
        this.selectedPartType = null;
        this.selectedPart = null;
        this.currentPage = 0;
        this.pageTracking.clear();
        clearEnquiryForm();
    }
    
    public void setPageForStage(String stage, int page) {
        pageTracking.put(stage, page);
    }
    
    public int getPageForStage(String stage) {
        return pageTracking.getOrDefault(stage, 0);
    }
    
    // Inner classes for model info (if not already defined elsewhere)
    @Data
    public static class ModelInfo {
        private Long id;
        private String name;
        private String code;
    }
    
    @Data
    public static class AccessoryTypeInfo {
        private Long id;
        private String name;
        private String description;
    }
    
    @Data
    public static class AccessorySubTypeInfo {
        private Long id;
        private String name;
        private String description;
    }
    
    @Data
    public static class AccessoryInfo {
        private Long id;
        private String name;
        private String description;
        private String price;
        private String imageUrl;
    }
    
    @Data
    public static class PartTypeInfo {
        private Long id;
        private String name;
        private String description;
    }
    
    @Data
    public static class PartInfo {
        private Long id;
        private String name;
        private String partNumber;
        private String description;
    }
}