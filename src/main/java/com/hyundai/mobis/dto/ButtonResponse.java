package com.hyundai.mobis.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ButtonResponse {
    private String message;
    private List<Button> buttons;
    private List<Button> navigationButtons; // For pagination
    private List<Button> actionButtons; // Back, Start Over, etc.
    private boolean allowTextInput;
    private String inputHint;
    private String responseType; // "buttons", "text", "mixed"
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Button {
        private String id;
        private String label;
        private String description;
        private String type; // "primary", "secondary", "navigation"
        private boolean disabled;
        
        public static Button create(String id, String label) {
            return Button.builder()
                .id(id)
                .label(label)
                .type("primary")
                .disabled(false)
                .build();
        }
        
        public static Button create(String id, String label, String description) {
            return Button.builder()
                .id(id)
                .label(label)
                .description(description)
                .type("primary")
                .disabled(false)
                .build();
        }
    }
    
    // Helper method to add all button types
    public static ButtonResponseBuilder withAllButtons() {
        return ButtonResponse.builder()
            .buttons(new ArrayList<>())
            .navigationButtons(new ArrayList<>())
            .actionButtons(new ArrayList<>());
    }
}