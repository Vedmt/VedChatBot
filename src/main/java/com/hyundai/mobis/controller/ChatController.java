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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat API", description = "Endpoints for Mobis AI Chatbot")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

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
            // Log the incoming request
            logger.info("Received message from session {}: {}", request.getSessionId(), request.getMessage());
            
            // Set user IP
            String userIp = getClientIpAddress(httpRequest);
            request.setUserIp(userIp);
            
            // Validate session ID
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                request.setSessionId(generateSessionId());
                logger.info("Generated new session ID: {}", request.getSessionId());
            }

            // Process the message - handles both text and button clicks
            ChatResponse response = chatbotService.processMessage(request);
            
            // Ensure response has necessary fields
            if (response.getSessionId() == null) {
                response.setSessionId(request.getSessionId());
            }
            
            if (response.getTimestamp() == null) {
                response.setTimestamp(LocalDateTime.now());
            }
            
            // Set response type if not set
            if (response.getResponseType() == null) {
                if (response.getButtons() != null && !response.getButtons().isEmpty()) {
                    response.setResponseType("buttons");
                } else {
                    response.setResponseType("text");
                }
            }
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error processing message for session {}: {}", request.getSessionId(), e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.error(
                "An unexpected error occurred. Please try again.", 
                request.getSessionId()
            );
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setResponseType("error");
            
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
            
            // Validate session ID
            if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
                request.setSessionId(generateSessionId());
            }

            ChatResponse response = chatbotService.processStreamMessage(request);
            
            // Ensure response has necessary fields
            if (response.getSessionId() == null) {
                response.setSessionId(request.getSessionId());
            }
            
            if (response.getTimestamp() == null) {
                response.setTimestamp(LocalDateTime.now());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing stream message for session {}: {}", request.getSessionId(), e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.error(
                "An unexpected error occurred during streaming.", 
                request.getSessionId()
            );
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setResponseType("error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the chatbot service is running")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Mobis Chatbot is running");
    }
    
    @GetMapping("/session/{sessionId}/info")
    @Operation(summary = "Get session info", description = "Get debug information about a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session info retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<?> getSessionInfo(@PathVariable String sessionId) {
        try {
            var sessionInfo = chatbotService.getSessionInfo(sessionId);
            if (sessionInfo.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            logger.error("Error getting session info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving session information");
        }
    }
    
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Clear session", description = "Clear all data for a specific session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session cleared successfully"),
        @ApiResponse(responseCode = "500", description = "Error clearing session")
    })
    public ResponseEntity<String> clearSession(@PathVariable String sessionId) {
        try {
            chatbotService.clearSessionData(sessionId);
            logger.info("Cleared session: {}", sessionId);
            return ResponseEntity.ok("Session cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error clearing session");
        }
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
    
    private String generateSessionId() {
        return "session_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" +
                System.currentTimeMillis();
    }
}
