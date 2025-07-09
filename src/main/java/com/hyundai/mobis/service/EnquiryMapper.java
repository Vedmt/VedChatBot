package com.hyundai.mobis.service;

import com.hyundai.mobis.dto.EnquiryForm;
import com.hyundai.mobis.model.EnquiryEntity;
import org.springframework.stereotype.Service;

@Service
public class EnquiryMapper {
    
    public EnquiryEntity toEntity(EnquiryForm form, String sessionId) {
        EnquiryEntity entity = new EnquiryEntity();
        
        // Basic info
        entity.setReferenceNumber(form.getReferenceNumber());
        entity.setItemType(form.getItemType());
        entity.setContactType(form.getDealerOrDistributor());
        entity.setSessionId(sessionId);
        
        // Dealer/Distributor info
        entity.setDealerDistributorId(form.getDealerDistributorId());
        entity.setDealerDistributorName(form.getDealerDistributorName());
        entity.setDealerDistributorDetails(form.getDealerDistributorDetails());
        
        // Customer info
        entity.setCustomerName(form.getCustomerName());
        entity.setEmail(form.getEmail());
        entity.setMobileNo(form.getMobileNo());
        entity.setQuery(form.getQuery());
        
        // Item specific
        if ("accessory".equals(form.getItemType())) {
            entity.setModelId(form.getModelId());
            entity.setModelName(form.getModel());
            entity.setAccessoryId(form.getAccessoryId());
            entity.setAccessoryName(form.getAccessoryName());
        } else {
            entity.setPartId(form.getPartId());
            entity.setPartName(form.getPartName());
        }
        
        entity.setStatus(form.getStatus() != null ? form.getStatus() : "submitted");
        
        return entity;
    }
    
    public EnquiryForm toDto(EnquiryEntity entity) {
        EnquiryForm form = new EnquiryForm();
        
        form.setReferenceNumber(entity.getReferenceNumber());
        form.setItemType(entity.getItemType());
        form.setDealerOrDistributor(entity.getContactType());
        form.setDealerDistributorId(entity.getDealerDistributorId());
        form.setDealerDistributorName(entity.getDealerDistributorName());
        form.setDealerDistributorDetails(entity.getDealerDistributorDetails());
        
        form.setCustomerName(entity.getCustomerName());
        form.setEmail(entity.getEmail());
        form.setMobileNo(entity.getMobileNo());
        form.setQuery(entity.getQuery());
        
        if ("accessory".equals(entity.getItemType())) {
            form.setModelId(entity.getModelId());
            form.setModel(entity.getModelName());
            form.setAccessoryId(entity.getAccessoryId());
            form.setAccessoryName(entity.getAccessoryName());
        } else {
            form.setPartId(entity.getPartId());
            form.setPartName(entity.getPartName());
        }
        
        form.setCreatedAt(entity.getCreatedAt());
        form.setStatus(entity.getStatus());
        
        return form;
    }
}