package com.hyundai.mobis.dto;

import java.math.BigDecimal;
import java.util.List;

public record MobisAccessoriesResponse(
    List<Accessory> accessories,
    int totalResults,
    boolean success,
    String message
) {
    public record Accessory(
        Long id,
        String accessoryName,
        String accessoryCode,
        String body,
        Long typeId,
        String type,
        Long subTypeId,
        String subType,
        BigDecimal mrp,
        String url,
        String urlText,
        String title,
        String image
    ) {}
} 