package com.hyundai.mobis.controller;

import com.hyundai.mobis.dto.AnalyticsResponse;
import com.hyundai.mobis.model.ChatMessage;
import com.hyundai.mobis.repository.ChatMessageRepository;
import com.hyundai.mobis.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        try {
            AnalyticsResponse analytics = analyticsService.generateAnalytics();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/chat-history")
    public ResponseEntity<Page<ChatMessage>> getChatHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ChatMessage> chatHistory = chatMessageRepository.findAllOrderByTimestampDesc(pageable);
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/chat-history/session/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistoryBySession(
            @PathVariable String sessionId) {
        try {
            List<ChatMessage> sessionHistory = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            return ResponseEntity.ok(sessionHistory);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChatMessage>> searchChatHistory(
            @RequestParam String keyword) {
        try {
            List<ChatMessage> searchResults = chatMessageRepository.findByKeyword(keyword);
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 