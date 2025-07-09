package com.hyundai.mobis.dto;

import lombok.Data;

@Data
public class DistributorInfo {
    private Long id;
    private String code;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private String location;
    private String city;
    private String state;
    private String postCode;
    private String stdCode;
    private String phone1;
    private String phone2;
    private String mobile1;
    private String mobile2;
    private String email;
    private String email1;
    private String email2;
    private String email3;
    private String fax;
    private String website;
    private String phone;
    private String address1;
    private String address2;
    private String status;
    
    // Helper method to get display name
    public String getDisplayName() {
        return name + (location != null ? " - " + location : "");
    }
    
    // Helper method to get contact info
    public String getContactInfo() {
        StringBuilder info = new StringBuilder();
        if (phone != null && !phone.isEmpty()) {
            info.append("📞 ").append(phone).append("\n");
        } else if (mobile1 != null && !mobile1.isEmpty()) {
            info.append("📞 ").append(mobile1).append("\n");
        }
        if (email1 != null && !email1.isEmpty()) {
            info.append("✉️ ").append(email1);
        } else if (email != null && !email.isEmpty()) {
            info.append("✉️ ").append(email);
        }
        return info.toString();
    }
}