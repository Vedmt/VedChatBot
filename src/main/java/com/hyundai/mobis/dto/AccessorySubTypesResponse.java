package com.hyundai.mobis.dto;

import java.util.List;

public record AccessorySubTypesResponse(
    List<String> subTypes,
    boolean success,
    String message
) {} 