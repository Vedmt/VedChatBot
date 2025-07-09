package com.hyundai.mobis.repository;

import com.hyundai.mobis.model.EnquiryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnquiryRepository extends JpaRepository<EnquiryEntity, Long> {
    
    Optional<EnquiryEntity> findByReferenceNumber(String referenceNumber);
    
    List<EnquiryEntity> findBySessionId(String sessionId);
    
    List<EnquiryEntity> findByEmailOrMobileNo(String email, String mobileNo);

    // Check for duplicate accessory enquiry
    boolean existsByEmailAndMobileNoAndAccessoryIdAndItemTypeAndCreatedAtAfter(
        String email, 
        String mobileNo, 
        String accessoryId, 
        String itemType, 
        LocalDateTime createdAt
    );
    
    // Check for duplicate part enquiry
    boolean existsByEmailAndMobileNoAndPartIdAndItemTypeAndCreatedAtAfter(
        String email, 
        String mobileNo, 
        String partId, 
        String itemType, 
        LocalDateTime createdAt
    );
    
    @Query("SELECT e FROM EnquiryEntity e WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate")
    List<EnquiryEntity> findEnquiriesInDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT COUNT(e) FROM EnquiryEntity e WHERE e.itemType = :itemType AND e.createdAt >= :startDate")
    Long countByItemTypeAfterDate(String itemType, LocalDateTime startDate);
    
    // For analytics
    @Query("SELECT e.stateName, COUNT(e) FROM EnquiryEntity e GROUP BY e.stateName")
    List<Object[]> getEnquiriesByState();
}