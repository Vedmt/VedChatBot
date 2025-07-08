package com.hyundai.mobis.dto;

import java.util.List;

public record DealerSearchResponse(
    List<Dealer> dealers,
    int totalResults
) {
    public static record Dealer(
        String id,
        String name,
        String address,
        String city,
        String state,
        String zipCode,
        String phone,
        String email,
        String website,
        double latitude,
        double longitude,
        double distanceKm,
        List<String> services,
        String operatingHours,
        boolean isAuthorized
    ) {}
} 