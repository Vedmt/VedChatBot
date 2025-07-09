package com.hyundai.mobis.dto;

import lombok.Data;

@Data
public class StateInfo {
    private Long id;
    private String code;
    private String description;
    private Integer displaySeq;
    private Integer version;
}