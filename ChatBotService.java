package com.hyundai.mobis.service;

import com.hyundai.mobis.config.HyundaiMobisConfig; // Change this if you named it Configuration.java
import com.hyundai.mobis.dto.ChatRequest;
import com.hyundai.mobis.dto.ChatResponse;
import com.hyundai.mobis.dto.MobisAccessoriesRequest;
import com.hyundai.mobis.dto.MobisAccessoriesResponse;
import com.hyundai.mobis.dto.AccessoryTypesResponse;
import com.hyundai.mobis.dto.AccessorySubTypesResponse;
import com.hyundai.mobis.dto.StatesResponse;
import com.hyundai.mobis.dto.DealerSearchRequest;
import com.hyundai.mobis.dto.DealerSearchResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersResponse;
import com.hyundai.mobis.model.ChatMessage;
import com.hyundai.mobis.repository.ChatMessageRepository;
import com.hyundai.mobis.service.MobisApiService.AccessoryTypesForModelResponse;
import com.hyundai.mobis.service.MobisApiService.AccessorySubTypesForTypeResponse;
import com.hyundai.mobis.service.MobisApiService.TypeInfo;
import com.hyundai.mobis.service.MobisApiService.SubTypeInfo;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MobisApiService mobisApiService;

    @Autowired
    private HyundaiMobisConfig config; // Change this if you named it Configuration

    private static final ConcurrentHashMap<String, String> sessionState = new ConcurrentHashMap<>();

    // Dynamic system prompt that uses config
    // In ChatbotService.java, find this method and update it:

private String getSystemPrompt() {
    String modelsList = String.join(", ", config.getModelNames());
    
    return String.format("""
        You are an AI assistant for Hyundai Mobis Genuine Accessories and Parts. You MUST answer ONLY using the available functions below. If a question cannot be answered using these functions, reply: 'I can only help you with Hyundai Mobis genuine accessories, parts, dealer locations, and related services.'
        
        Available functions:
        - getAccessoryTypesForModelFunction: Get accessory categories for a specific model (returns typeId and typeName)
        - getAccessorySubTypesForTypeFunction: Get subcategories for a specific type (needs modelName and typeId)
        - getAccessoriesFilteredFunction: Get accessories filtered by type and/or subtype
        - getAccessoriesByModelFunction: Get all accessories for a specific Hyundai model
        - getAllPartTypesFunction: Get all available parts categories (NEW)
        - getPartsByTypeFunction: Get parts for a specific type/category (needs typeId) (NEW)
        - findDealersFunction: Find authorized Hyundai dealers by location
        - findDistributorsFunction: Find parts distributors in specific regions
        - getStatesFunction: Get all supported states for product distribution
        - getDistributorStatesFunction: Get states with distributor coverage
        - getOffersFunction: Get current offers and promotions
        
        For accessories browsing, follow this flow:
        1. First ask for vehicle model
        2. Use getAccessoryTypesForModelFunction to show available types
        3. After type selection, use getAccessorySubTypesForTypeFunction to show subtypes
        4. Finally use getAccessoriesFilteredFunction to show specific accessories
        
        For parts browsing, follow this flow:
        1. Use getAllPartTypesFunction to show parts categories
        2. After category selection, use getPartsByTypeFunction to show parts in that category
        3. Parts don't have prices - direct users to enquire with dealers
        
        Available models: %s
        
        If the function returns no data, say so. Do NOT use your own knowledge or browse the web.
        """, modelsList);
}

    public ChatResponse processMessage(ChatRequest request) {
        try {
            logger.info("USER [{}]: {}", request.getSessionId(), request.getMessage());
            String userMessage = request.getMessage().toLowerCase().trim();

            // Log session state
            String lastState = sessionState.get(request.getSessionId());
            logger.info("Session [{}] last state: {}", request.getSessionId(), lastState);

            // Check for stateful follow-up
            if (lastState != null) {
                if (lastState.equals("awaiting_vehicle_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createTypeSelectionResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_type_selection")) {
                    sessionState.remove(request.getSessionId());
                    return handleTypeSelection(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_part_type_selection")) {
                    sessionState.remove(request.getSessionId());
                    return handlePartTypeSelection(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_subtype_selection")) {
                    sessionState.remove(request.getSessionId());
                    return handleSubTypeSelection(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_location_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createDealersForLocationResponse(request.getSessionId(), request.getMessage());
                }
            }

            // Handle conversational flow
            if (isConversationStart(userMessage)) {
                return createConversationStartResponse(request.getSessionId());
            } else if (isOptionSelection(userMessage)) {
                return handleOptionSelection(request);
            } else {
                // Handle direct questions using existing logic
                return handleDirectQuestion(request);
            }
        } catch (Exception e) {
            logger.error("Error processing chat message for session {}: {}", request.getSessionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process chat message: " + e.getMessage(), e);
        }
    }

    private boolean isConversationStart(String message) {
        return message.contains("start") || message.contains("begin") ||
                message.contains("help") || message.contains("hello") ||
                message.contains("hi") || message.contains("what can you do");
    }

    private boolean isOptionSelection(String message) {
        return message.matches("^\\d+$") ||
                message.contains("browse accessories") || message.contains("view accessory") ||
                message.contains("find dealers") || message.contains("check offers") ||
                message.contains("product support");
    }

    private ChatResponse createConversationStartResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Hello! I'm your Hyundai Mobis Genuine Accessories assistant. I can help you explore genuine accessories for your Hyundai vehicle and find the best prices.");
        response.setQuestion("What would you like to explore?");
        response.setOptions(Arrays.asList(
                "Browse Accessories",
                "Browse Parts",
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Get Product Support"
        ));
        response.setConversationEnd(false);
        response.setConversationType("main_menu");

        saveChatMessage(sessionId, "Conversation start", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse handleOptionSelection(ChatRequest request) {
        String userMessage = request.getMessage().trim();
        String sessionId = request.getSessionId();

        logger.info("Handling option selection: [{}]", userMessage);

        // Handle main menu options
        if (userMessage.equalsIgnoreCase("Browse Accessories")) {
            sessionState.put(sessionId, "awaiting_vehicle_selection");
            return createVehicleSelectionResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Browse Parts")) {
            return createPartsTypesResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Find Dealers & Distributors")) {
            sessionState.put(sessionId, "awaiting_location_selection");
            return createLocationSelectionResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Enquire About Parts")) {
            return createPartsEnquiryResponse(sessionId);
        }else if (userMessage.equalsIgnoreCase("Check Current Offers")) {
            return createOffersResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Get Product Support")) {
            return createProductSupportResponse(sessionId);
        }else if (userMessage.equalsIgnoreCase("Browse Another Category")) {
    // Check if we're in parts or accessories context
    String modelName = sessionState.get(sessionId + "_model");
    if (modelName != null) {
        // We're in accessories context
        return createTypeSelectionResponse(sessionId, modelName);
    } else {
        // We're in parts context
        return createPartsTypesResponse(sessionId);
    }
} else if (userMessage.equalsIgnoreCase("Find Nearest Dealer")) {
    sessionState.put(sessionId, "awaiting_location_selection");
    return createLocationSelectionResponse(sessionId);
} else if (userMessage.equalsIgnoreCase("Browse More Parts")) {
    return createPartsTypesResponse(sessionId);
} else if (userMessage.equalsIgnoreCase("Start over")) {
    // Clear session state
    sessionState.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId));
    return createConversationStartResponse(sessionId);
}

        // Handle numeric selection
        if (userMessage.matches("^\\d+$")) {
            int selection = Integer.parseInt(userMessage);
            return handleNumericSelection(selection, sessionId);
        }

        return createInvalidSelectionResponse(sessionId);
    }

    private ChatResponse handleNumericSelection(int selection, String sessionId) {
        switch (selection) {
            case 1:
                sessionState.put(sessionId, "awaiting_vehicle_selection");
                return createVehicleSelectionResponse(sessionId);
            case 2:
                sessionState.put(sessionId, "awaiting_location_selection");
                return createLocationSelectionResponse(sessionId);
            case 3:
                return createOffersResponse(sessionId);
            case 4:
                return createProductSupportResponse(sessionId);
            default:
                return createInvalidSelectionResponse(sessionId);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private ChatResponse createVehicleSelectionResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Great! Let's find the perfect accessories for your Hyundai. Which vehicle model do you own?");
        response.setQuestion("Select your vehicle model:");

        // Get model names from config
        response.setOptions(config.getModelNames());

        response.setConversationEnd(false);
        response.setConversationType("vehicle_selection");

        saveChatMessage(sessionId, "Vehicle selection prompt", response.getMessage(), null, null);
        return response;
    }

    // New method to handle type selection after vehicle
    private ChatResponse createTypeSelectionResponse(String sessionId, String vehicleModel) {
        try {
            // Store selected model in session
            sessionState.put(sessionId + "_model", vehicleModel);

            logger.info("API CALL: getAccessoryTypesForModelFunction (model: {})", vehicleModel);
            AccessoryTypesForModelResponse typesResponse = mobisApiService.getAccessoryTypesForModel(vehicleModel);

            if (!typesResponse.success() || typesResponse.types().isEmpty()) {
                return createErrorResponse(sessionId, "No accessories found for " + vehicleModel);
            }

            StringBuilder message = new StringBuilder();
            message.append(String.format("🚗 **Accessories for %s**\n\n", vehicleModel));
            message.append("Select a category to explore:\n\n");

            List<String> options = new ArrayList<>();
            for (TypeInfo type : typesResponse.types()) {
                String icon = getCategoryIcon(type.typeName());
                message.append(String.format("%s **%s**\n", icon, type.typeName()));
                options.add(type.typeName());

                // Store typeId mapping in session
                sessionState.put(sessionId + "_type_" + type.typeName(),String.valueOf(type.typeId()));
            }

            sessionState.put(sessionId, "awaiting_type_selection");

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select accessory category:");
            response.setOptions(options);
            response.setConversationEnd(false);
            response.setConversationType("type_selection");

            saveChatMessage(sessionId, "Type selection for " + vehicleModel, message.toString(), "getAccessoryTypesForModelFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting types: {}", e.getMessage());
            return createErrorResponse(sessionId, "Error loading accessory categories");
        }
    }

    // Handle type selection and show subtypes
    private ChatResponse handleTypeSelection(String sessionId, String selectedType) {
        try {
            String modelName = sessionState.get(sessionId + "_model");
            String typeIdStr = sessionState.get(sessionId + "_type_" + selectedType);

            if (modelName == null || typeIdStr == null) {
                return createErrorResponse(sessionId, "Session expired. Please start over.");
            }

            Long typeId = Long.parseLong(typeIdStr);
            sessionState.put(sessionId + "_selected_type", selectedType);
            sessionState.put(sessionId + "_selected_typeId", typeIdStr);

            logger.info("API CALL: getAccessorySubTypesForTypeFunction (model: {}, typeId: {})", modelName, typeId);
            AccessorySubTypesForTypeResponse subTypesResponse = mobisApiService.getAccessorySubTypesForType(modelName, typeId);

            if (!subTypesResponse.success() || subTypesResponse.subTypes().isEmpty()) {
                // If no subtypes, show all accessories for this type
                return showAccessoriesForType(sessionId, modelName, typeId, selectedType);
            }

            StringBuilder message = new StringBuilder();
            message.append(String.format("📂 **%s - Subcategories**\n\n", selectedType));
            message.append("Select a subcategory:\n\n");

            List<String> options = new ArrayList<>();
            for (SubTypeInfo subType : subTypesResponse.subTypes()) {
                message.append(String.format("• **%s**\n", subType.subTypeName()));
                options.add(subType.subTypeName());

                // Store subTypeId mapping in session
                sessionState.put(sessionId + "_subtype_" + subType.subTypeName(), String.valueOf(subType.subTypeId()));
            }

            sessionState.put(sessionId, "awaiting_subtype_selection");

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select subcategory:");
            response.setOptions(options);
            response.setConversationEnd(false);
            response.setConversationType("subtype_selection");

            saveChatMessage(sessionId, "Subtype selection for " + selectedType, message.toString(), "getAccessorySubTypesForTypeFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error handling type selection: {}", e.getMessage());
            return createErrorResponse(sessionId, "Error loading subcategories");
        }
    }

    // Handle subtype selection and show accessories
    private ChatResponse handleSubTypeSelection(String sessionId, String selectedSubType) {
        try {
            String modelName = sessionState.get(sessionId + "_model");
            String typeIdStr = sessionState.get(sessionId + "_selected_typeId");
            String subTypeIdStr = sessionState.get(sessionId + "_subtype_" + selectedSubType);

            if (modelName == null || typeIdStr == null || subTypeIdStr == null) {
                return createErrorResponse(sessionId, "Session expired. Please start over.");
            }

            Long typeId = Long.parseLong(typeIdStr);
            Long subTypeId = Long.parseLong(subTypeIdStr);

            logger.info("API CALL: getAccessoriesFilteredFunction (model: {}, typeId: {}, subTypeId: {})",
                    modelName, typeId, subTypeId);
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesFiltered(modelName, typeId, subTypeId);

            return displayAccessories(sessionId, modelName, accessories, selectedSubType);
        } catch (Exception e) {
            logger.error("Error handling subtype selection: {}", e.getMessage());
            return createErrorResponse(sessionId, "Error loading accessories");
        }
    }

    // Show accessories for a type (when no subtypes)
    private ChatResponse showAccessoriesForType(String sessionId, String modelName, Long typeId, String typeName) {
        try {
            logger.info("API CALL: getAccessoriesFilteredFunction (model: {}, typeId: {}, no subType)", modelName, typeId);
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesFiltered(modelName, typeId, null);

            return displayAccessories(sessionId, modelName, accessories, typeName);
        } catch (Exception e) {
            logger.error("Error showing accessories: {}", e.getMessage());
            return createErrorResponse(sessionId, "Error loading accessories");
        }
    }

    // Common method to display accessories
    private ChatResponse displayAccessories(String sessionId, String modelName, MobisAccessoriesResponse accessories, String categoryName) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("🛍️ **%s Accessories for %s**\n\n", categoryName, modelName));

        if (accessories.accessories().isEmpty()) {
            message.append("No accessories found in this category.");
        } else {
            message.append(String.format("Found %d accessories:\n\n", accessories.accessories().size()));

            for (MobisAccessoriesResponse.Accessory accessory : accessories.accessories()) {
                String icon = getCategoryIcon(accessory.type());
                message.append(String.format("%s **%s**\n", icon, accessory.accessoryName()));
                message.append(String.format("   💰 **MRP: ₹%.0f**\n", accessory.mrp()));
                message.append(String.format("   📝 %s\n", cleanHtml(accessory.body())));
                message.append(String.format("   🔧 Part Code: %s\n", accessory.accessoryCode()));
                message.append(String.format("   🏷️ Category: %s - %s\n\n", accessory.type(), accessory.subType()));
            }
        }

        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(message.toString());
        response.setQuestion("What would you like to do next?");
        response.setOptions(Arrays.asList(
                "Browse Another Category",
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Start over"
        ));
        response.setConversationEnd(false);
        response.setConversationType("accessories_result");

        saveChatMessage(sessionId, "Accessories display", message.toString(), "getAccessoriesFilteredFunction", null);
        return response;
    }

    private ChatResponse createLocationSelectionResponse(String sessionId) {
        try {
            logger.info("API CALL: getStatesFunction");
            StatesResponse states = mobisApiService.getStates();
            logger.info("API RESPONSE: {}", states);

            StringBuilder message = new StringBuilder();
            message.append("📍 **Find Dealers & Distributors**\n\n");
            message.append("I can help you find authorized Hyundai dealers and distributors. Which state are you looking for?\n\n");

            // Show first 10 states as options
            List<String> stateOptions = states.states().stream().limit(10).toList();
            for (String state : stateOptions) {
                message.append(String.format("• %s\n", state));
            }

            message.append("\n💡 *You can also type your city name directly.*");

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select a state or enter your city:");
            response.setOptions(stateOptions);
            response.setConversationEnd(false);
            response.setConversationType("location_selection");

            saveChatMessage(sessionId, "Location selection prompt", message.toString(), "getStatesFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting states: {}", e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the location information. Please try again.");
        }
    }

    private ChatResponse createOffersResponse(String sessionId) {
        try {
            logger.info("API CALL: getOffersFunction");
            OffersResponse offers = mobisApiService.getCurrentOffers(new OffersRequest("accessories", "all", null));
            logger.info("API RESPONSE: {}", offers);

            StringBuilder message = new StringBuilder();
            message.append("🎉 **Current Offers & Promotions**\n\n");
            message.append("Here are the latest offers on genuine Hyundai accessories:\n\n");

            for (OffersResponse.Offer offer : offers.offers()) {
                message.append(String.format("**%s**\n", offer.title()));
                message.append(String.format("📝 %s\n", offer.description()));
                message.append(String.format("💰 Discount: %s\n", offer.discount()));
                message.append(String.format("⏰ Valid until: %s\n", offer.validUntil()));
                message.append(String.format("📋 Terms: %s\n\n", offer.terms()));
            }

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("What would you like to do next?");
            response.setOptions(Arrays.asList(
                    "Browse Accessories",
                    "Find Dealers & Distributors",
                    "Start over"
            ));
            response.setConversationEnd(false);
            response.setConversationType("offers");

            saveChatMessage(sessionId, "Offers request", message.toString(), "getOffersFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting offers: {}", e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the current offers. Please try again.");
        }
    }

    private ChatResponse createProductSupportResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🔧 **Product Support & Warranty**\n\n" +
                "All Hyundai Mobis genuine accessories come with:\n\n" +
                "✅ **12-month warranty** on all accessories\n" +
                "✅ **Perfect fit guarantee** for your vehicle\n" +
                "✅ **Easy installation** with detailed instructions\n" +
                "✅ **Quality assurance** from Hyundai Mobis\n\n" +
                "For warranty claims or technical support, please contact your nearest authorized dealer.");
        response.setQuestion("What would you like to do next?");
        response.setOptions(Arrays.asList(
                "Browse Accessories",
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Start over"
        ));
        response.setConversationEnd(false);
        response.setConversationType("product_support");

        saveChatMessage(sessionId, "Product support request", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createInvalidSelectionResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("I didn't understand that selection. Let me show you the available options:");
        response.setQuestion("What would you like to explore?");
        response.setOptions(Arrays.asList(
                "Browse Accessories",
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Get Product Support"
        ));
        response.setConversationEnd(false);
        response.setConversationType("main_menu");

        saveChatMessage(sessionId, "Invalid selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createDealersForLocationResponse(String sessionId, String location) {
        try {
            logger.info("API CALL: findDealersFunction (location: {})", location);
            DealerSearchRequest request = new DealerSearchRequest(location, null, null, null, 0.0, 0.0, 50);
            DealerSearchResponse dealers = mobisApiService.findDealers(request);
            logger.info("API RESPONSE: {} dealers found", dealers.dealers().size());

            StringBuilder message = new StringBuilder();
            message.append(String.format("🏪 **Authorized Dealers in %s**\n\n", capitalize(location)));
            message.append("Here are the authorized Hyundai dealers near you:\n\n");

            for (DealerSearchResponse.Dealer dealer : dealers.dealers()) {
                message.append(String.format("**%s**\n", dealer.name()));
                message.append(String.format("📍 %s, %s %s\n", dealer.address(), dealer.city(), dealer.zipCode()));
                message.append(String.format("📞 %s\n", dealer.phone()));
                message.append(String.format("🌐 %s\n", dealer.website()));
                message.append(String.format("🕒 %s\n", dealer.operatingHours()));
                message.append(String.format("📊 Distance: %.1f km\n\n", dealer.distanceKm()));
            }

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("What would you like to do next?");
            response.setOptions(Arrays.asList(
                    "Browse Accessories",
                    "Check Current Offers",
                    "Find Distributors",
                    "Start over"
            ));
            response.setConversationEnd(false);
            response.setConversationType("dealers_result");

            saveChatMessage(sessionId, "Dealers in " + location, message.toString(), "findDealersFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting dealers for {}: {}", location, e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the dealers in " + location + ". Please try again.");
        }
    }

    private ChatResponse handleDirectQuestion(ChatRequest request) {
        // Use existing logic for direct questions
        List<Message> messages = Arrays.asList(
                new SystemMessage(getSystemPrompt()),
                new UserMessage(request.getMessage())
        );

        var response = chatClient.call(new Prompt(messages));
        String result = response.getResult().getOutput().getContent();

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setSessionId(request.getSessionId());
        chatResponse.setSuccess(true);
        chatResponse.setTimestamp(LocalDateTime.now());
        chatResponse.setMessage(result);
        chatResponse.setConversationEnd(true);

        saveChatMessage(request.getSessionId(), request.getMessage(), result, null, null);
        return chatResponse;
    }

    private ChatResponse createErrorResponse(String sessionId, String errorMessage) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(false);
        response.setTimestamp(LocalDateTime.now());
        response.setErrorMessage(errorMessage);
        response.setConversationEnd(true);

        saveChatMessage(sessionId, "Error", errorMessage, null, null);
        return response;
    }

    private void saveChatMessage(String sessionId, String userMessage, String botResponse, String functionsCalled, Long responseTimeMs) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setUserMessage(userMessage);
            chatMessage.setBotResponse(botResponse);
            chatMessage.setFunctionsCalled(functionsCalled);
            chatMessage.setResponseTimeMs(responseTimeMs);
            chatMessage.setTimestamp(LocalDateTime.now());

            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            logger.error("Error saving chat message: {}", e.getMessage());
        }
    }

    public ChatResponse processStreamMessage(ChatRequest request) {
        // For streaming responses (future enhancement)
        return processMessage(request);
    }

    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().substring(0, 8) + "_" +
                System.currentTimeMillis();
    }

    // Helper methods
    private String getModelIdFromName(String vehicleName) {
        HyundaiMobisConfig.Model model = config.getModelByName(vehicleName);
        return model != null ? model.getModelId() : null;
    }

    private String getCategoryIcon(String category) {
        String cat = category.toLowerCase();
        if (cat.contains("interior")) return "🪑";
        if (cat.contains("exterior")) return "🚗";
        if (cat.contains("performance")) return "⚡";
        if (cat.contains("safety")) return "🛡️";
        if (cat.contains("comfort")) return "😌";
        if (cat.contains("technology") || cat.contains("electronic")) return "📱";
        if (cat.contains("common")) return "🛍️";
        return "🔧";
    }

    // Clean HTML tags from body content
    private String cleanHtml(String html) {
        if (html == null) return "";
        // Remove HTML tags
        String text = html.replaceAll("<[^>]*>", "");
        // Clean up extra whitespace
        text = text.replaceAll("\\s+", " ").trim();
        // Decode HTML entities
        text = text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#10;", " ")
                .replace("&#39;", "'");
        return text;
    }

    // Navigation handling for "Browse Another Category" option
    private boolean isNavigationOption(String message) {
        String lower = message.toLowerCase();
        return lower.contains("browse another category") ||
                lower.contains("start over") ||
                lower.contains("back to") ||
                lower.contains("main menu");
    }

    // Additional handler in handleOptionSelection for navigation
    private ChatResponse handleNavigationOption(String sessionId, String option) {
        if (option.equalsIgnoreCase("Browse Another Category")) {
            String modelName = sessionState.get(sessionId + "_model");
            if (modelName != null) {
                return createTypeSelectionResponse(sessionId, modelName);
            }
        } else if (option.equalsIgnoreCase("Start over")) {
            // Clear session state
            sessionState.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId));
            return createConversationStartResponse(sessionId);
        }
        return createConversationStartResponse(sessionId);
    }

    // Update handleOptionSelection to include navigation handling
    private ChatResponse handleOptionSelectionEnhanced(ChatRequest request) {
        String userMessage = request.getMessage().trim();
        String sessionId = request.getSessionId();

        logger.info("Handling option selection: [{}]", userMessage);

        // Check for navigation options first
        if (isNavigationOption(userMessage)) {
            return handleNavigationOption(sessionId, userMessage);
        }

        // Main menu options
        if (userMessage.equalsIgnoreCase("Browse Accessories")) {
            sessionState.put(sessionId, "awaiting_vehicle_selection");
            return createVehicleSelectionResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Find Dealers & Distributors")) {
            sessionState.put(sessionId, "awaiting_location_selection");
            return createLocationSelectionResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Check Current Offers")) {
            return createOffersResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Get Product Support")) {
            return createProductSupportResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Find Distributors")) {
            return createDistributorsResponse(sessionId);
        }

        // Handle numeric selection
        if (userMessage.matches("^\\d+$")) {
            int selection = Integer.parseInt(userMessage);
            return handleNumericSelection(selection, sessionId);
        }

        return createInvalidSelectionResponse(sessionId);
    }

    // Add distributor response method
    private ChatResponse createDistributorsResponse(String sessionId) {
        try {
            logger.info("API CALL: findDistributorsFunction");
            DealerSearchRequest request = new DealerSearchRequest("", null, null, null, 0.0, 0.0, 50);
            DealerSearchResponse distributors = mobisApiService.findDistributors(request);

            StringBuilder message = new StringBuilder();
            message.append("🏭 **Authorized Parts Distributors**\n\n");
            message.append("Here are the authorized Hyundai parts distributors:\n\n");

            for (DealerSearchResponse.Dealer distributor : distributors.dealers()) {
                message.append(String.format("**%s**\n", distributor.name()));
                message.append(String.format("📍 %s, %s %s\n", distributor.address(), distributor.city(), distributor.zipCode()));
                message.append(String.format("📞 %s\n", distributor.phone()));
                message.append(String.format("📧 %s\n", distributor.email()));
                message.append(String.format("🌐 %s\n", distributor.website()));
                message.append(String.format("🕒 %s\n", distributor.operatingHours()));
                message.append(String.format("📦 Services: %s\n\n", String.join(", ", distributor.services())));
            }

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("What would you like to do next?");
            response.setOptions(Arrays.asList(
                    "Browse Accessories",
                    "Find Dealers",
                    "Check Current Offers",
                    "Start over"
            ));
            response.setConversationEnd(false);
            response.setConversationType("distributors_result");

            saveChatMessage(sessionId, "Distributors request", message.toString(), "findDistributorsFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting distributors: {}", e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the distributor information. Please try again.");
        }
    }

    // Clear session data method
    public void clearSessionData(String sessionId) {
        sessionState.entrySet().removeIf(entry -> entry.getKey().startsWith(sessionId));
    }

    // Get session info method for debugging
    public Map<String, String> getSessionInfo(String sessionId) {
        return sessionState.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(sessionId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    private String getPartCategoryIcon(String category) {
    String cat = category.toLowerCase();
    if (cat.contains("engine")) return "🔧";
    if (cat.contains("brake")) return "🛑";
    if (cat.contains("electric")) return "⚡";
    if (cat.contains("body")) return "🚗";
    if (cat.contains("suspension")) return "🏎️";
    if (cat.contains("clutch")) return "⚙️";
    return "🔩";
    }

    private ChatResponse createPartsTypesResponse(String sessionId) {
    try {
        logger.info("API CALL: getAllPartTypesFunction");
        var partTypes = mobisApiService.getAllPartTypes();
        
        if (!partTypes.success() || partTypes.types().isEmpty()) {
            return createErrorResponse(sessionId, "Unable to fetch parts categories.");
        }
        
        StringBuilder message = new StringBuilder();
        message.append("🔧 **Genuine Hyundai Parts Categories**\n\n");
        message.append("Select a category to view available parts:\n\n");
        
        List<String> options = new ArrayList<>();
        for (var type : partTypes.types()) {
            String icon = getPartCategoryIcon(type.typeName());
            message.append(String.format("%s **%s**\n", icon, type.typeName()));
            options.add(type.typeName());
            sessionState.put(sessionId + "_parttype_" + type.typeName(), String.valueOf(type.typeId()));
        }
        
        sessionState.put(sessionId, "awaiting_part_type_selection");
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(message.toString());
        response.setQuestion("Select parts category:");
        response.setOptions(options);
        response.setConversationEnd(false);
        response.setConversationType("part_types");
        
        return response;
    } catch (Exception e) {
        logger.error("Error getting part types: {}", e.getMessage());
        return createErrorResponse(sessionId, "Error loading parts categories");
    }
}

    private ChatResponse createPartsEnquiryResponse(String sessionId) {
    ChatResponse response = new ChatResponse();
    response.setSessionId(sessionId);
    response.setSuccess(true);
    response.setTimestamp(LocalDateTime.now());
    response.setMessage("📞 **Parts Enquiry**\n\n" +
        "To enquire about parts pricing and availability, you can:\n\n" +
        "1. **Visit Nearest Dealer** - Our dealers will provide exact pricing and availability\n" +
        "2. **Call Dealer** - Get instant information over phone\n" +
        "3. **Online Enquiry** - Submit your requirements and dealer will contact you\n\n" +
        "Would you like to find the nearest dealer?");
    response.setQuestion("Choose an option:");
    response.setOptions(Arrays.asList(
        "Find Nearest Dealer",
        "Browse More Parts",
        "Start over"
    ));
    response.setConversationEnd(false);
    response.setConversationType("parts_enquiry");
    
    return response;
}
    
private ChatResponse handlePartTypeSelection(String sessionId, String selectedType) {
    try {
        String typeIdStr = sessionState.get(sessionId + "_parttype_" + selectedType);
        if (typeIdStr == null) {
            return createErrorResponse(sessionId, "Invalid selection. Please try again.");
        }
        
        Long typeId = Long.parseLong(typeIdStr);
        logger.info("API CALL: getPartsByTypeFunction (typeId: {})", typeId);
        var partsResponse = mobisApiService.getPartsByType(typeId);
        
        StringBuilder message = new StringBuilder();
        message.append(String.format("🔧 **%s Parts**\n\n", selectedType));
        
        if (partsResponse.parts().isEmpty()) {
            message.append("No parts available in this category.");
        } else {
            for (var part : partsResponse.parts()) {
                message.append(String.format("**%s**\n", part.partName()));
                message.append(String.format("📝 %s\n", part.description()));
                message.append(String.format("🔧 Part Code: %s\n\n", part.partCode()));
            }
            
            message.append("\n⋆**Disclaimer**: For price, availability and any other details, ");
            message.append("kindly visit your nearest Hyundai Dealership/Authorized Distributor ");
            message.append("by Mobis India Ltd. The images shown are for illustration purposes only ");
            message.append("and may not be an exact representation of the product.\n\n");
            message.append("💬 **Click below to enquire - Nearest dealer will support you to buy the product.**");
        }
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(message.toString());
        response.setQuestion("What would you like to do?");
        response.setOptions(Arrays.asList(
            "Enquire About Parts",
            "Browse Another Category", 
            "Find Nearest Dealer",
            "Start over"
        ));
        response.setConversationEnd(false);
        response.setConversationType("parts_list");
        
        return response;
    } catch (Exception e) {
        logger.error("Error handling part type selection: {}", e.getMessage());
        return createErrorResponse(sessionId, "Error loading parts");
    }
}
}