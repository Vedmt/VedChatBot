package com.hyundai.mobis.service;

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
import static java.util.stream.Collectors.groupingBy;

@Service
public class ChatbotService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    private static final String SYSTEM_PROMPT = """
        You are an AI assistant for Hyundai Mobis Genuine Accessories. You MUST answer ONLY using the available functions below. If a question cannot be answered using these functions, reply: 'I can only help you with Hyundai Mobis genuine accessories, dealer locations, and related services.'
        
        Available functions:
        - getAllAccessoryTypesFunction: Get all available accessory categories (Interior, Exterior, Performance, etc.).
        - getAllAccessorySubTypesFunction: Get all accessory subtypes (Floor Mats, Bumper Accessories, etc.).
        - getAccessoriesByModelFunction: Get accessories for a specific Hyundai model and year.
        - findDealersFunction: Find authorized Hyundai dealers by location.
        - findDistributorsFunction: Find parts distributors in specific regions.
        - getStatesFunction: Get all supported states for product distribution.
        - getDistributorStatesFunction: Get states with distributor coverage.
        - getOffersFunction: Get current offers and promotions.
        
        Examples:
        - "What accessory categories are available?" → use getAllAccessoryTypesFunction
        - "Show me accessories for New i20" → use getAccessoriesByModelFunction with modelId='new-i20'
        - "Find dealers in Mumbai" → use findDealersFunction with city='Mumbai'
        - "What states are supported?" → use getStatesFunction
        
        If the function returns no data, say so. Do NOT use your own knowledge or browse the web.
        """;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MobisApiService mobisApiService;

    private static final ConcurrentHashMap<String, String> sessionState = new ConcurrentHashMap<>();

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
                    return createAccessoriesForVehicleResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_location_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createDealersForLocationResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_interior_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createInteriorSubCategoriesResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_exterior_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createExteriorSubCategoriesResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_performance_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createPerformanceSubCategoriesResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_safety_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createSafetySubCategoriesResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.equals("awaiting_comfort_selection")) {
                    sessionState.remove(request.getSessionId());
                    return createComfortSubCategoriesResponse(request.getSessionId(), request.getMessage());
                } else if (lastState.startsWith("awaiting_vehicle_for_")) {
                    sessionState.remove(request.getSessionId());
                    return createSpecificAccessoryForVehicleResponse(request.getSessionId(), lastState, request.getMessage());
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
        // Check if message is a number (1, 2, 3, etc.) or contains specific keywords
        return message.matches("^\\d+$") || 
               message.contains("browse accessories") || message.contains("view accessory") || 
               message.contains("find dealers") || message.contains("check offers") ||
               message.contains("product support") || message.contains("interior") ||
               message.contains("exterior") || message.contains("performance") ||
               message.contains("safety") || message.contains("comfort") ||
               message.contains("convenience") || message.contains("navigation") ||
               message.contains("sound") || message.contains("vision") ||
               message.contains("ride") || message.contains("handling") ||
               message.contains("seat") || message.contains("floor") ||
               message.contains("dashboard") || message.contains("steering") ||
               message.contains("console") || message.contains("door") ||
               message.contains("glove") || message.contains("shift");
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
        } else if (userMessage.equalsIgnoreCase("Find Dealers & Distributors")) {
            sessionState.put(sessionId, "awaiting_location_selection");
            return createLocationSelectionResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Check Current Offers")) {
            return createOffersResponse(sessionId);
        } else if (userMessage.equalsIgnoreCase("Get Product Support")) {
            return createProductSupportResponse(sessionId);
        }

        // Handle specific accessory categories
        if (userMessage.toLowerCase().contains("interior")) {
            sessionState.put(sessionId, "awaiting_interior_selection");
            return createInteriorAccessoriesResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("exterior")) {
            sessionState.put(sessionId, "awaiting_exterior_selection");
            return createExteriorAccessoriesResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("performance")) {
            sessionState.put(sessionId, "awaiting_performance_selection");
            return createPerformanceAccessoriesResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("safety")) {
            sessionState.put(sessionId, "awaiting_safety_selection");
            return createSafetyAccessoriesResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("comfort")) {
            sessionState.put(sessionId, "awaiting_comfort_selection");
            return createComfortAccessoriesResponse(sessionId);
        }

        // Handle specific accessory types
        if (userMessage.toLowerCase().contains("seat")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_seat");
            return createSeatCoversResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("floor")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_floor");
            return createFloorMatsResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("dashboard")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_dashboard");
            return createDashboardTrimsResponse(sessionId);
        } else if (userMessage.toLowerCase().contains("steering")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_steering");
            return createSteeringWheelCoversResponse(sessionId);
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
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("vehicle_selection");
        
        saveChatMessage(sessionId, "Vehicle selection prompt", response.getMessage(), null, null);
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

    private ChatResponse createAccessoriesForVehicleResponse(String sessionId, String vehicleModel) {
        try {
            String modelId = getModelIdFromName(vehicleModel);
            if (modelId == null) {
                return createInvalidVehicleResponse(sessionId);
            }

            logger.info("API CALL: getAccessoriesByModelFunction (modelId: {}, year: 2018)", modelId);
            MobisAccessoriesRequest request = new MobisAccessoriesRequest(modelId, "2018", null, null);
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesByModel(request);
            logger.info("API RESPONSE: {} accessories found", accessories.accessories().size());
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("🚗 **Genuine Accessories for %s**\n\n", capitalize(vehicleModel)));
            message.append(String.format("Here are the available genuine accessories for your %s:\n\n", capitalize(vehicleModel)));
            
            // Group accessories by type for better organization
            Map<String, List<MobisAccessoriesResponse.Accessory>> groupedAccessories = 
                accessories.accessories().stream().collect(groupingBy(MobisAccessoriesResponse.Accessory::type));
            
            for (Map.Entry<String, List<MobisAccessoriesResponse.Accessory>> entry : groupedAccessories.entrySet()) {
                String type = entry.getKey();
                List<MobisAccessoriesResponse.Accessory> typeAccessories = entry.getValue();
                
                String icon = getCategoryIcon(type);
                message.append(String.format("%s **%s**\n", icon, type));
                
                for (MobisAccessoriesResponse.Accessory accessory : typeAccessories) {
                    message.append(String.format("   • **%s**\n", accessory.accessoryName()));
                    message.append(String.format("     💰 **MRP: ₹%.0f**\n", accessory.mrp()));
                    message.append(String.format("     📝 %s\n", accessory.body()));
                    message.append(String.format("     🔧 Part Code: %s\n", accessory.accessoryCode()));
                    message.append(String.format("     🏷️ Category: %s\n\n", accessory.subType()));
                }
            }
            
            if (accessories.accessories().isEmpty()) {
                message.append("No accessories found for this vehicle model. Please try another model or contact your nearest dealer.");
            }
            
            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("What would you like to do next?");
            response.setOptions(Arrays.asList(
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Browse Another Vehicle",
                "Start over"
            ));
            response.setConversationEnd(false);
            response.setConversationType("accessories_result");
            
            saveChatMessage(sessionId, "Accessories for " + vehicleModel, message.toString(), "getAccessoriesByModelFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting accessories for {}: {}", vehicleModel, e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the accessories for " + vehicleModel + ". Please try again.");
        }
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
            message.append("ℹ️ *Note: Dealer information is simulated for demonstration purposes*\n\n");
            
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
            new SystemMessage(SYSTEM_PROMPT),
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
        String name = vehicleName.toLowerCase();
        if (name.contains("i20")) return "27";
        if (name.contains("creta inline")) return "31";
        if (name.contains("alcazar")) return "34";
        if (name.contains("creta ev") || name.contains("creta")) return "35";
        if (name.contains("verna")) return "32";
        if (name.contains("venue")) return "33";
        if (name.contains("exter")) return "36";
        if (name.contains("grand i10") || name.contains("nios")) return "28";
        if (name.contains("aura")) return "29";
        return null;
    }

    private String getCategoryIcon(String category) {
        String cat = category.toLowerCase();
        if (cat.contains("interior")) return "🪑";
        if (cat.contains("exterior")) return "🚗";
        if (cat.contains("performance")) return "⚡";
        if (cat.contains("safety")) return "🛡️";
        if (cat.contains("comfort")) return "😌";
        if (cat.contains("technology")) return "📱";
        return "🛍️";
    }

    private ChatResponse createInvalidVehicleResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("I didn't recognize that vehicle model. Let me show you the available options:");
        response.setQuestion("Select your vehicle model:");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("vehicle_selection");
        
        saveChatMessage(sessionId, "Invalid vehicle selection", response.getMessage(), null, null);
        return response;
    }

    private boolean lastQuestionWasVehicleTypes(String sessionId) {
        // This method is no longer needed since we use session state directly
        return false;
    }

    // New response methods for accessory categories
    private ChatResponse createInteriorAccessoriesResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🪑 **Interior Accessories**\n\n" +
            "Enhance your vehicle's interior with premium accessories:");
        response.setQuestion("Select an interior accessory:");
        response.setOptions(Arrays.asList(
            "Seat Covers",
            "Floor Mats",
            "Dashboard Trims",
            "Steering Wheel Covers",
            "Center Console Organizers",
            "Door Trim Kits",
            "Glove Compartments",
            "Shift Knobs"
        ));
        response.setConversationEnd(false);
        response.setConversationType("interior_selection");
        
        saveChatMessage(sessionId, "Interior accessories selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createExteriorAccessoriesResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🚗 **Exterior Accessories**\n\n" +
            "Upgrade your vehicle's exterior appearance:");
        response.setQuestion("Select an exterior accessory:");
        response.setOptions(Arrays.asList(
            "Bumper Guards",
            "Side Mouldings",
            "Roof Rails",
            "Mud Flaps",
            "Window Tinting",
            "Body Kits",
            "Wheel Covers",
            "Hood Protectors"
        ));
        response.setConversationEnd(false);
        response.setConversationType("exterior_selection");
        
        saveChatMessage(sessionId, "Exterior accessories selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createPerformanceAccessoriesResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("⚡ **Performance Accessories**\n\n" +
            "Boost your vehicle's performance:");
        response.setQuestion("Select a performance accessory:");
        response.setOptions(Arrays.asList(
            "Air Filters",
            "Exhaust Systems",
            "Brake Pads",
            "Suspension Kits",
            "Engine Oil",
            "Spark Plugs",
            "Fuel Filters",
            "Performance Chips"
        ));
        response.setConversationEnd(false);
        response.setConversationType("performance_selection");
        
        saveChatMessage(sessionId, "Performance accessories selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createSafetyAccessoriesResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🛡️ **Safety Accessories**\n\n" +
            "Enhance your vehicle's safety features:");
        response.setQuestion("Select a safety accessory:");
        response.setOptions(Arrays.asList(
            "Child Safety Seats",
            "First Aid Kits",
            "Emergency Kits",
            "Safety Vests",
            "Warning Triangles",
            "Fire Extinguishers",
            "Seat Belt Extenders",
            "Blind Spot Mirrors"
        ));
        response.setConversationEnd(false);
        response.setConversationType("safety_selection");
        
        saveChatMessage(sessionId, "Safety accessories selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createComfortAccessoriesResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("😌 **Comfort Accessories**\n\n" +
            "Improve your driving comfort:");
        response.setQuestion("Select a comfort accessory:");
        response.setOptions(Arrays.asList(
            "Neck Pillows",
            "Lumbar Supports",
            "Armrest Cushions",
            "Seat Heaters",
            "Massage Cushions",
            "Sun Shades",
            "Window Visors",
            "Cup Holders"
        ));
        response.setConversationEnd(false);
        response.setConversationType("comfort_selection");
        
        saveChatMessage(sessionId, "Comfort accessories selection", response.getMessage(), null, null);
        return response;
    }

    // Specific accessory type responses
    private ChatResponse createSeatCoversResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🪑 **Seat Covers**\n\n" +
            "Protect and style your seats with genuine Hyundai seat covers. Available in various materials and colors.");
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("seat_covers_vehicle_selection");
        
        saveChatMessage(sessionId, "Seat covers vehicle selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createFloorMatsResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🛡️ **Floor Mats**\n\n" +
            "Keep your vehicle's interior clean with custom-fit floor mats designed for your Hyundai.");
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("floor_mats_vehicle_selection");
        
        saveChatMessage(sessionId, "Floor mats vehicle selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createDashboardTrimsResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🎛️ **Dashboard Trims**\n\n" +
            "Add a touch of elegance to your dashboard with premium trim accessories.");
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("dashboard_trims_vehicle_selection");
        
        saveChatMessage(sessionId, "Dashboard trims vehicle selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createSteeringWheelCoversResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("🎯 **Steering Wheel Covers**\n\n" +
            "Enhance your driving experience with comfortable and stylish steering wheel covers.");
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("steering_covers_vehicle_selection");
        
        saveChatMessage(sessionId, "Steering wheel covers vehicle selection", response.getMessage(), null, null);
        return response;
    }

    // Sub-category response methods
    private ChatResponse createInteriorSubCategoriesResponse(String sessionId, String selection) {
        // Handle interior sub-category selection
        if (selection.toLowerCase().contains("seat")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_seat");
            return createSeatCoversResponse(sessionId);
        } else if (selection.toLowerCase().contains("floor")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_floor");
            return createFloorMatsResponse(sessionId);
        } else if (selection.toLowerCase().contains("dashboard")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_dashboard");
            return createDashboardTrimsResponse(sessionId);
        } else if (selection.toLowerCase().contains("steering")) {
            sessionState.put(sessionId, "awaiting_vehicle_for_steering");
            return createSteeringWheelCoversResponse(sessionId);
        } else {
            return createGenericInteriorAccessoryResponse(sessionId, selection);
        }
    }

    private ChatResponse createExteriorSubCategoriesResponse(String sessionId, String selection) {
        return createGenericExteriorAccessoryResponse(sessionId, selection);
    }

    private ChatResponse createPerformanceSubCategoriesResponse(String sessionId, String selection) {
        return createGenericPerformanceAccessoryResponse(sessionId, selection);
    }

    private ChatResponse createSafetySubCategoriesResponse(String sessionId, String selection) {
        return createGenericSafetyAccessoryResponse(sessionId, selection);
    }

    private ChatResponse createComfortSubCategoriesResponse(String sessionId, String selection) {
        return createGenericComfortAccessoryResponse(sessionId, selection);
    }

    // Generic accessory response methods
    private ChatResponse createGenericInteriorAccessoryResponse(String sessionId, String accessoryType) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(String.format("🪑 **%s**\n\n" +
            "Premium interior accessory for your Hyundai. Available in various styles and materials.", 
            capitalize(accessoryType)));
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("generic_interior_selection");
        
        saveChatMessage(sessionId, accessoryType + " selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createGenericExteriorAccessoryResponse(String sessionId, String accessoryType) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(String.format("🚗 **%s**\n\n" +
            "Enhance your vehicle's exterior with this premium accessory.", 
            capitalize(accessoryType)));
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("generic_exterior_selection");
        
        saveChatMessage(sessionId, accessoryType + " selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createGenericPerformanceAccessoryResponse(String sessionId, String accessoryType) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(String.format("⚡ **%s**\n\n" +
            "Boost your vehicle's performance with this genuine accessory.", 
            capitalize(accessoryType)));
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("generic_performance_selection");
        
        saveChatMessage(sessionId, accessoryType + " selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createGenericSafetyAccessoryResponse(String sessionId, String accessoryType) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(String.format("🛡️ **%s**\n\n" +
            "Enhance your vehicle's safety with this essential accessory.", 
            capitalize(accessoryType)));
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("generic_safety_selection");
        
        saveChatMessage(sessionId, accessoryType + " selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createGenericComfortAccessoryResponse(String sessionId, String accessoryType) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(String.format("😌 **%s**\n\n" +
            "Improve your driving comfort with this premium accessory.", 
            capitalize(accessoryType)));
        response.setQuestion("Which vehicle do you own?");
        response.setOptions(Arrays.asList(
            "i20",
            "Creta EV",
            "Creta Inline",
            "Alcazar",
            "Verna",
            "Venue",
            "Exter",
            "Grand i10 NIOS",
            "Aura"
        ));
        response.setConversationEnd(false);
        response.setConversationType("generic_comfort_selection");
        
        saveChatMessage(sessionId, accessoryType + " selection", response.getMessage(), null, null);
        return response;
    }

    // Method to handle specific accessory for vehicle selection
    private ChatResponse createSpecificAccessoryForVehicleResponse(String sessionId, String lastState, String vehicleModel) {
        String modelId = getModelIdFromName(vehicleModel);
        if (modelId == null) {
            return createInvalidVehicleResponse(sessionId);
        }

        String accessoryType = lastState.replace("awaiting_vehicle_for_", "");
        String accessoryName = getAccessoryNameFromType(accessoryType);
        
        try {
            logger.info("API CALL: getAccessoriesByModelFunction (modelId: {}, accessoryType: {})", modelId, accessoryType);
            MobisAccessoriesRequest request = new MobisAccessoriesRequest(modelId, "2018", accessoryType, null);
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesByModel(request);
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("💰 **%s for %s**\n\n", accessoryName, capitalize(vehicleModel)));
            message.append(String.format("Here are the available %s for your %s:\n\n", accessoryName.toLowerCase(), capitalize(vehicleModel)));
            
            if (accessories.accessories().isEmpty()) {
                message.append("No specific accessories found for this category. Here are some general options:\n\n");
                message.append(String.format("• **Premium %s** - ₹%s\n", accessoryName, getEstimatedPrice(accessoryType)));
                message.append(String.format("• **Standard %s** - ₹%s\n", accessoryName, getEstimatedPrice(accessoryType) * 0.7));
                message.append(String.format("• **Deluxe %s** - ₹%s\n", accessoryName, getEstimatedPrice(accessoryType) * 1.3));
            } else {
                // Filter accessories by subType that matches the accessory type
                List<MobisAccessoriesResponse.Accessory> filteredAccessories = accessories.accessories().stream()
                    .filter(acc -> acc.subType().toLowerCase().contains(accessoryType.toLowerCase()) || 
                                  acc.accessoryName().toLowerCase().contains(accessoryType.toLowerCase()))
                    .toList();
                
                if (filteredAccessories.isEmpty()) {
                    // Show all accessories if no specific match found
                    for (MobisAccessoriesResponse.Accessory accessory : accessories.accessories()) {
                        String icon = getCategoryIcon(accessory.type());
                        message.append(String.format("%s **%s**\n", icon, accessory.accessoryName()));
                        message.append(String.format("   💰 **MRP: ₹%.0f**\n", accessory.mrp()));
                        message.append(String.format("   📝 %s\n", accessory.body()));
                        message.append(String.format("   🔧 Part Code: %s\n", accessory.accessoryCode()));
                        message.append(String.format("   🏷️ Category: %s\n\n", accessory.subType()));
                    }
                } else {
                    for (MobisAccessoriesResponse.Accessory accessory : filteredAccessories) {
                        String icon = getCategoryIcon(accessory.type());
                        message.append(String.format("%s **%s**\n", icon, accessory.accessoryName()));
                        message.append(String.format("   💰 **MRP: ₹%.0f**\n", accessory.mrp()));
                        message.append(String.format("   📝 %s\n", accessory.body()));
                        message.append(String.format("   🔧 Part Code: %s\n", accessory.accessoryCode()));
                        message.append(String.format("   🏷️ Category: %s\n\n", accessory.subType()));
                    }
                }
            }
            
            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("What would you like to do next?");
            response.setOptions(Arrays.asList(
                "Find Dealers & Distributors",
                "Check Current Offers",
                "Browse Another Accessory",
                "Start over"
            ));
            response.setConversationEnd(false);
            response.setConversationType("accessory_pricing_result");
            
            saveChatMessage(sessionId, accessoryName + " for " + vehicleModel, message.toString(), "getAccessoriesByModelFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting accessories for {}: {}", vehicleModel, e.getMessage());
            return createErrorResponse(sessionId, "Sorry, I couldn't retrieve the accessories for " + vehicleModel + ". Please try again.");
        }
    }

    // Helper methods for accessory handling
    private String getAccessoryNameFromType(String accessoryType) {
        switch (accessoryType) {
            case "seat": return "Seat Covers";
            case "floor": return "Floor Mats";
            case "dashboard": return "Dashboard Trims";
            case "steering": return "Steering Wheel Covers";
            default: return capitalize(accessoryType);
        }
    }

    private int getEstimatedPrice(String accessoryType) {
        switch (accessoryType) {
            case "seat": return 2500;
            case "floor": return 1200;
            case "dashboard": return 800;
            case "steering": return 600;
            default: return 1500;
        }
    }
} 