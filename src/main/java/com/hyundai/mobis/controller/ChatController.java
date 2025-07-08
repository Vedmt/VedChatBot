package com.hyundai.mobis.controller;

import com.hyundai.mobis.dto.ChatRequest;
import com.hyundai.mobis.dto.ChatResponse;
import com.hyundai.mobis.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "Endpoints for Mobis AI Chatbot")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/message")
    @Operation(summary = "Send message to chatbot", description = "Process a user message and get AI response")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        
        try {
            // Set user IP
            String userIp = getClientIpAddress(httpRequest);
            request.setUserIp(userIp);

            ChatResponse response = chatbotService.processMessage(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            ChatResponse errorResponse = ChatResponse.error(
                "An unexpected error occurred. Please try again.", 
                request.getSessionId()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/stream")
    @Operation(summary = "Send message with streaming response", description = "Process a user message and get streaming AI response")
    public ResponseEntity<ChatResponse> sendStreamMessage(
            @Valid @RequestBody ChatRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        
        try {
            String userIp = getClientIpAddress(httpRequest);
            request.setUserIp(userIp);

            ChatResponse response = chatbotService.processStreamMessage(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ChatResponse errorResponse = ChatResponse.error(
                "An unexpected error occurred during streaming.", 
                request.getSessionId()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the chatbot service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Mobis Chatbot is running");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 