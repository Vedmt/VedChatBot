package com.hyundai.mobis.dto;

import java.util.List;

public record StatesResponse(
    List<String> states,
    boolean success,
    String message
) {} 