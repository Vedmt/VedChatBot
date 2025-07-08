package com.hyundai.mobis.repository;

import com.hyundai.mobis.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId);

    Page<ChatMessage> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.timestamp BETWEEN :start AND :end")
    Long countMessagesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(c.responseTimeMs) FROM ChatMessage c WHERE c.timestamp BETWEEN :start AND :end")
    Double getAverageResponseTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM ChatMessage c WHERE c.userMessage LIKE %:keyword% OR c.botResponse LIKE %:keyword%")
    List<ChatMessage> findByKeyword(@Param("keyword") String keyword);

    @Query("SELECT COUNT(DISTINCT c.sessionId) FROM ChatMessage c")
    Long countDistinctSessions();

    @Query("SELECT c FROM ChatMessage c ORDER BY c.timestamp DESC")
    Page<ChatMessage> findAllOrderByTimestampDesc(Pageable pageable);
} 