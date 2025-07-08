package com.hyundai.mobis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    
    private String message;
    private String sessionId;
    private LocalDateTime timestamp;
    private String functionsCalled;
    private Long responseTimeMs;
    private boolean success;
    private String errorMessage;
    
    // New fields for conversational flow
    private String question;
    private List<String> options;
    private boolean isConversationEnd;
    private String conversationType;

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String message, String sessionId, boolean success) {
        this.message = message;
        this.sessionId = sessionId;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public static ChatResponse success(String message, String sessionId) {
        ChatResponse response = new ChatResponse(message, sessionId, true);
        return response;
    }

    public static ChatResponse error(String message, String sessionId) {
        ChatResponse response = new ChatResponse(message, sessionId, false);
        return response;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFunctionsCalled() {
        return functionsCalled;
    }

    public void setFunctionsCalled(String functionsCalled) {
        this.functionsCalled = functionsCalled;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public boolean isConversationEnd() {
        return isConversationEnd;
    }

    public void setConversationEnd(boolean conversationEnd) {
        isConversationEnd = conversationEnd;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }
} 