package com.hyundai.mobis.controller;

import com.hyundai.mobis.model.ChatMessage;
import com.hyundai.mobis.repository.ChatMessageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat/management")
@Tag(name = "Chat Management API", description = "Endpoints for managing chat sessions and analytics")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatManagementController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get conversation history", description = "Retrieve conversation history for a specific session")
    public ResponseEntity<List<ChatMessage>> getConversationHistory(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent conversations", description = "Get recent chat messages with pagination")
    public ResponseEntity<Page<ChatMessage>> getRecentConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime now = LocalDateTime.now();
        
        Page<ChatMessage> messages = chatMessageRepository.findByTimestampBetween(yesterday, now, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get chat analytics", description = "Get basic analytics about chat usage")
    public ResponseEntity<Map<String, Object>> getChatAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7); // Default to last 7 days
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Long totalMessages = chatMessageRepository.countMessagesBetween(startDate, endDate);
        Double averageResponseTime = chatMessageRepository.getAverageResponseTimeBetween(startDate, endDate);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalMessages", totalMessages);
        analytics.put("averageResponseTimeMs", averageResponseTime);
        analytics.put("period", Map.of("start", startDate, "end", endDate));

        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/search")
    @Operation(summary = "Search conversations", description = "Search conversations by keyword")
    public ResponseEntity<List<ChatMessage>> searchConversations(@RequestParam String keyword) {
        List<ChatMessage> messages = chatMessageRepository.findByKeyword(keyword);
        return ResponseEntity.ok(messages);
    }
} 