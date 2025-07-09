package com.hyundai.mobis.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Data
public class SessionData {
    private String sessionId;
    private String currentState; // Current conversation state
    private Map<String, Object> context = new HashMap<>();
    private boolean showingAccessories;
    private int accessoriesCount;
    private List<AccessoryInfo> accessoriesList;
    private Map<String, AccessoryTypeInfo> typeInfoMap;
    private Map<String, AccessorySubTypeInfo> subTypeInfoMap;
    private boolean showingParts;
private int partsCount;
private List<PartInfo> partsList;
private Map<String, PartTypeInfo> partTypeInfoMap;
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
    
    public boolean isShowingParts() {
        return showingParts;
    }
    
    public void setShowingParts(boolean showingParts) {
        this.showingParts = showingParts;
    }
    
    public int getPartsCount() {
        return partsCount;
    }
    
    public void setPartsCount(int count) {
        this.partsCount = count;
    }
    
    public PartInfo getPartByIndex(int index) {
        if (partsList != null && index >= 0 && index < partsList.size()) {
            return partsList.get(index);
        }
        return null;
    }
    
    public void clearPartsList() {
        if (partsList != null) {
            partsList.clear();
        }
    }
    
    public void addPartToList(PartInfo part) {
        if (partsList == null) {
            partsList = new ArrayList<>();
        }
        partsList.add(part);
    }
    
    public void clearPartTypeInfoMap() {
        if (partTypeInfoMap != null) {
            partTypeInfoMap.clear();
        }
    }
    
    public void addPartTypeInfo(String typeName, PartTypeInfo typeInfo) {
        if (partTypeInfoMap == null) {
            partTypeInfoMap = new HashMap<>();
        }
        partTypeInfoMap.put(typeName, typeInfo);
    }
    
    public PartTypeInfo getPartTypeInfo(String typeName) {
        return partTypeInfoMap != null ? partTypeInfoMap.get(typeName) : null;
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
    
    // Add these methods to SessionData.java
    public boolean isShowingAccessories() {
        return showingAccessories;
    }

    public void setShowingAccessories(boolean showingAccessories) {
        this.showingAccessories = showingAccessories;
    }

    public int getAccessoriesCount() {
        return accessoriesCount;
    }

    public void setAccessoriesCount(int count) {
        this.accessoriesCount = count;
    }

    public AccessoryInfo getAccessoryByIndex(int index) {
        if (accessoriesList != null && index >= 0 && index < accessoriesList.size()) {
            return accessoriesList.get(index);
        }
        return null;
    }

    public void clearAccessoriesList() {
        if (accessoriesList != null) {
            accessoriesList.clear();
        }
    }

    public void addAccessoryToList(AccessoryInfo accessory) {
        if (accessoriesList == null) {
            accessoriesList = new ArrayList<>();
        }
        accessoriesList.add(accessory);
    }

    public void setIsAccessoryFlow(boolean isAccessoryFlow) {
        this.isAccessoryFlow = isAccessoryFlow;
    }

public void addTypeInfo(String typeName, AccessoryTypeInfo typeInfo) {
    if (typeInfoMap == null) {
        typeInfoMap = new HashMap<>();
    }
    typeInfoMap.put(typeName, typeInfo);
}

public AccessoryTypeInfo getTypeInfo(String typeName) {
    return typeInfoMap != null ? typeInfoMap.get(typeName) : null;
}

public void addSubTypeInfo(String subTypeName, AccessorySubTypeInfo subTypeInfo) {
    if (subTypeInfoMap == null) {
        subTypeInfoMap = new HashMap<>();
    }
    subTypeInfoMap.put(subTypeName, subTypeInfo);
}

public AccessorySubTypeInfo getSubTypeInfo(String subTypeName) {
    return subTypeInfoMap != null ? subTypeInfoMap.get(subTypeName) : null;
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