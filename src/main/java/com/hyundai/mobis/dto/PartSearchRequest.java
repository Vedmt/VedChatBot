package com.hyundai.mobis.dto;

public record PartSearchRequest(
    String partNumber,
    String vehicleModel,
    String vehicleYear,
    String category,
    String keyword
) {
    public PartSearchRequest {
        // Validation can be added here if needed
        if (partNumber != null) {
            partNumber = partNumber.trim().toUpperCase();
        }
        if (vehicleModel != null) {
            vehicleModel = vehicleModel.trim();
        }
        if (vehicleYear != null) {
            vehicleYear = vehicleYear.trim();
        }
    }
} 