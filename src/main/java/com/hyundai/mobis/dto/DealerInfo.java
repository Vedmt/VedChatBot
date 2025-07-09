package com.hyundai.mobis.dto;

import lombok.Data;

@Data
public class DealerInfo {
    private Long id;
    private String dealerCode;
    private String dealerName;
    private Integer dealerCategoryId;
    private String dealerCategoryDesc;
    private String dealerTypeDesc;
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
    private String email1;
    private String email2;
    private String email3;
    private String fax;
    private String enquiryEmail;
    private String website;
    private String webpage;
    private String phone;
    private String email;
    private String address1;
    private String address2;
    private String dealerStatus;
    private String merchant;
    private Boolean hyBuy;
    private Boolean onlineBooking;
    
    // Helper method to get display name
    public String getDisplayName() {
        return dealerName + (location != null ? " - " + location : "");
    }
    
    // Helper method to get contact info
    public String getContactInfo() {
        StringBuilder info = new StringBuilder();
        if (phone != null && !phone.isEmpty()) {
            info.append("📞 ").append(phone).append("\n");
        }
        if (email != null && !email.isEmpty()) {
            info.append("✉️ ").append(email);
        }
        return info.toString();
    }
}