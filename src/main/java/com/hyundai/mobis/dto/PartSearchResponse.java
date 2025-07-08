package com.hyundai.mobis.dto;

import java.math.BigDecimal;
import java.util.List;

public record PartSearchResponse(
    List<Part> parts,
    int totalResults,
    boolean hasMore
) {
    public static record Part(
        String partNumber,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        boolean inStock,
        List<String> compatibleVehicles,
        String imageUrl,
        String warrantyPeriod
    ) {}
} 