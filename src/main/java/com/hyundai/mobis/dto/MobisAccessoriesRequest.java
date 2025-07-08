package com.hyundai.mobis.dto;

public record MobisAccessoriesRequest(
    String modelId,
    String year,
    String accessoryType,
    String subType
) {} 