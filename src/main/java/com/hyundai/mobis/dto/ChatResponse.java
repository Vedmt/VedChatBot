package com.hyundai.mobis.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String sessionId;
    private String responseType; // "text", "buttons", "form", "confirmation"

    private boolean success;
    private String question;
    private List<String> options;
    private boolean conversationEnd;
    private String conversationType;
    private String errorMessage;
    
    private LocalDateTime timestamp;

    // Button support
    private List<ButtonResponse.Button> buttons;
    private List<ButtonResponse.Button> navigationButtons;
    private List<ButtonResponse.Button> actionButtons;
    
    // Form support
    private EnquiryForm enquiryForm;
    private String formStage; // "collecting", "review", "submitted"
    
    // Additional metadata
    private boolean allowTextInput;
    private String inputHint;
    private Object metadata; // For any additional data
    
    // Backward compatibility - simple text response
    public static ChatResponse text(String message) {
        return ChatResponse.builder()
            .message(message)
            .responseType("text")
            .build();
    }


    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public void setOptions(List<String> options) {
        this.options = options;
    }
    
    public void setConversationEnd(boolean conversationEnd) {
        this.conversationEnd = conversationEnd;
    }
    
    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static ChatResponse error(String errorMessage, String sessionId) {
        return ChatResponse.builder()
            .sessionId(sessionId)
            .success(false)
            .message(errorMessage)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .responseType("error")
            .build();
    }
    
    // Button response helper
    public static ChatResponse withButtons(String message, List<ButtonResponse.Button> buttons) {
        return ChatResponse.builder()
            .message(message)
            .buttons(buttons)
            .responseType("buttons")
            .build();
    }
}