package com.hyundai.mobis.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String sessionId;
    private String timestamp;
    private String responseType; // "text", "buttons", "form", "confirmation"
    
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
    
    // Button response helper
    public static ChatResponse withButtons(String message, List<ButtonResponse.Button> buttons) {
        return ChatResponse.builder()
            .message(message)
            .buttons(buttons)
            .responseType("buttons")
            .build();
    }
}