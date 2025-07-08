package com.hyundai.mobis.dto;

import java.util.List;

public record AccessoryTypesResponse(
    List<String> types,
    boolean success,
    String message
) {} 