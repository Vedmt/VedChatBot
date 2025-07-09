package com.hyundai.mobis.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class DealerDistributorFlow {
    private String type; // "dealer" or "distributor"
    private String currentStep; // "TYPE_SELECTION", "STATE_SELECTION", etc.
    private int currentPage = 0;
    private boolean inSearchMode = false;
    private String searchTerm;
    
    // Selected values
    private String selectedState;
    private String selectedStateId;
    private String selectedCity;
    private String selectedCityId;
    private String selectedEntity; // dealer or distributor name
    private String selectedEntityId;
    private String selectedEntityDetails; // Full info for display
    
    // For navigation
    private Map<String, Integer> pageTracking = new HashMap<>();
    
    public void incrementPage() {
        currentPage++;
    }
    
    public void decrementPage() {
        if (currentPage > 0) currentPage--;
    }
    
    public void setCurrentStep(String step) {
        // Save current page when changing steps
        if (this.currentStep != null) {
            pageTracking.put(this.currentStep, currentPage);
        }
        this.currentStep = step;
        // Restore page for this step
        this.currentPage = pageTracking.getOrDefault(step, 0);
    }
    
    public void goBack() {
        switch (currentStep) {
            case "CITY_SELECTION":
                currentStep = "STATE_SELECTION";
                selectedCity = null;
                selectedCityId = null;
                break;
            case "DEALER_SELECTION":
                currentStep = "CITY_SELECTION";
                selectedEntity = null;
                selectedEntityId = null;
                inSearchMode = false;
                searchTerm = null;
                break;
            case "DISTRIBUTOR_SELECTION":
                currentStep = "STATE_SELECTION";
                selectedEntity = null;
                selectedEntityId = null;
                break;
            case "STATE_SELECTION":
                currentStep = "TYPE_SELECTION";
                selectedState = null;
                selectedStateId = null;
                break;
        }
        currentPage = pageTracking.getOrDefault(currentStep, 0);
    }
    
    public void reset() {
        type = null;
        currentStep = "TYPE_SELECTION";
        currentPage = 0;
        pageTracking.clear();
        inSearchMode = false;
        searchTerm = null;
        selectedState = null;
        selectedStateId = null;
        selectedCity = null;
        selectedCityId = null;
        selectedEntity = null;
        selectedEntityId = null;
        selectedEntityDetails = null;
    }
}