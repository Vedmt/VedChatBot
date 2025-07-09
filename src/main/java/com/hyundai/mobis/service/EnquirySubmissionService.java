package com.hyundai.mobis.service;

import com.hyundai.mobis.dto.EnquiryForm;
import com.hyundai.mobis.model.EnquiryEntity;
import com.hyundai.mobis.repository.EnquiryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class EnquirySubmissionService {
    
    @Autowired
    private EnquiryRepository enquiryRepository;
    
    @Autowired
    private EnquiryMapper enquiryMapper;
    
    @Transactional
    public EnquirySubmissionResult submitEnquiry(EnquiryForm form, String sessionId) {
        try {
            // Generate reference number
            String referenceNumber = generateReferenceNumber();
            form.setReferenceNumber(referenceNumber);
            form.setCreatedAt(LocalDateTime.now());
            form.setStatus("submitted");
            
            // Convert to entity and save
            EnquiryEntity entity = enquiryMapper.toEntity(form, sessionId);
            enquiryRepository.save(entity);
            
            log.info("Enquiry submitted: Reference={}, Type={}, Customer={}", 
                referenceNumber, form.getItemType(), form.getCustomerName());
            
            return EnquirySubmissionResult.builder()
                .success(true)
                .referenceNumber(referenceNumber)
                .message("Your enquiry has been submitted successfully!")
                .enquiryId(entity.getId())
                .build();
                
        } catch (Exception e) {
            log.error("Error submitting enquiry", e);
            return EnquirySubmissionResult.builder()
                .success(false)
                .message("Sorry, there was an error submitting your enquiry. Please try again.")
                .build();
        }
    }
    
    public Optional<EnquiryForm> getEnquiryByReference(String referenceNumber) {
        return enquiryRepository.findByReferenceNumber(referenceNumber)
            .map(enquiryMapper::toDto);
    }
    
    private String generateReferenceNumber() {
        // Format: ENQ-YYYYMMDD-XXXXX (where X is random alphanumeric)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = generateRandomString(5);
        return String.format("ENQ-%s-%s", dateStr, randomStr);
    }
    
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    @Data
    @Builder
    public static class EnquirySubmissionResult {
        private boolean success;
        private String referenceNumber;
        private String message;
        private Long enquiryId;
    }
}