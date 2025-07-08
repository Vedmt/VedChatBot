package com.hyundai.mobis.dto;

public record DealerSearchRequest(
    String location,
    String city,
    String state,
    String zipCode,
    double latitude,
    double longitude,
    int radiusKm
) {
    public DealerSearchRequest {
        if (location != null) {
            location = location.trim();
        }
        if (city != null) {
            city = city.trim();
        }
        if (state != null) {
            state = state.trim();
        }
        if (zipCode != null) {
            zipCode = zipCode.trim();
        }
        if (radiusKm <= 0) {
            radiusKm = 50; // Default radius
        }
    }
} 