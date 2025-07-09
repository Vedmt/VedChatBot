package com.hyundai.mobis.service;

import com.hyundai.mobis.config.HyundaiMobisConfig;
import com.hyundai.mobis.dto.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    @Autowired
    private EnquiryFlowHandler enquiryFlowHandler;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MobisApiService mobisApiService;

    @Autowired
    private HyundaiMobisConfig config;

    // Changed from String to SessionData
    private static final ConcurrentHashMap<String, SessionData> sessions = new ConcurrentHashMap<>();

    // Get or create session
    private SessionData getOrCreateSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new SessionData(sessionId));
    }

    // Dynamic system prompt that uses config
    private String getSystemPrompt() {
        String modelsList = String.join(", ", config.getModelNames());

        return String.format("""
            You are an AI assistant for Hyundai Mobis Genuine Accessories. You MUST answer ONLY using the available functions below. If a question cannot be answered using these functions, reply: 'I can only help you with Hyundai Mobis genuine accessories, dealer locations, and related services.'
            
            Available functions:
            - getAccessoryTypesForModelFunction: Get accessory categories for a specific model (returns typeId and typeName)
            - getAccessorySubTypesForTypeFunction: Get subcategories for a specific type (needs modelName and typeId)
            - getAccessoriesFilteredFunction: Get accessories filtered by type and/or subtype
            - getAccessoriesByModelFunction: Get all accessories for a specific Hyundai model
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
            
            Available models: %s
            
            If the function returns no data, say so. Do NOT use your own knowledge or browse the web.
            """, modelsList);
    }

    public ChatResponse processMessage(ChatRequest request) {
        try {
            logger.info("USER [{}]: {}", request.getSessionId(), request.getMessage());
            String userMessage = request.getMessage().toLowerCase().trim();
            
            SessionData session = getOrCreateSession(request.getSessionId());
            
            // Check if in enquiry flow
            if (session.isInEnquiryFlow()) {
                return enquiryFlowHandler.handleEnquiryFlow(session, request.getMessage());
            }
            
// Check if user selected a part by number for enquiry
if (session.isShowingParts() && userMessage.matches("^\\d+$")) {
    int selection = Integer.parseInt(userMessage);
    if (selection > 0 && selection <= session.getPartsCount()) {
        SessionData.PartInfo selectedPart = session.getPartByIndex(selection - 1);
        if (selectedPart != null) {
            session.setSelectedPart(selectedPart);
            session.setShowingParts(false);
            return enquiryFlowHandler.handleEnquiryFlow(session, "enquire");
        }
    }
}

            // Check if user selected an accessory by number for enquiry
            if (session.isShowingAccessories() && userMessage.matches("^\\d+$")) {
                int selection = Integer.parseInt(userMessage);
                if (selection > 0 && selection <= session.getAccessoriesCount()) {
                    SessionData.AccessoryInfo selectedAccessory = session.getAccessoryByIndex(selection - 1);
                    if (selectedAccessory != null) {
                        session.setSelectedAccessory(selectedAccessory);
                        session.setShowingAccessories(false);
                        return enquiryFlowHandler.handleEnquiryFlow(session, "enquire");
                    }
                }
            }
            
            // Check for enquiry keywords
            if (shouldHandleAsEnquiry(session, request.getMessage())) {
                if (session.getSelectedAccessory() != null) {
                    return enquiryFlowHandler.handleEnquiryFlow(session, "enquire");
                } else {
                    return createErrorResponse(request.getSessionId(), 
                        "Please select an accessory or part first before making an enquiry.");
                }
            }

            // Log session state
            String lastState = session.getCurrentState();
            logger.info("Session [{}] last state: {}", request.getSessionId(), lastState);

            // Check for stateful follow-up
            if (lastState != null) {
                if (lastState.equals("awaiting_vehicle_selection")) {
                    session.setCurrentState(null);
                    return createTypeSelectionResponse(session, request.getMessage());
                } else if (lastState.equals("awaiting_type_selection")) {
                    session.setCurrentState(null);
                    return handleTypeSelection(session, request.getMessage());
                } else if (lastState.equals("awaiting_part_type_selection")) {
                    session.setCurrentState(null);
                    return handlePartTypeSelection(session, request.getMessage());
                } else if (lastState.equals("awaiting_subtype_selection")) {
                    session.setCurrentState(null);
                    return handleSubTypeSelection(session, request.getMessage());
                } else if (lastState.equals("awaiting_location_selection")) {
                    session.setCurrentState(null);
                    return createDealersForLocationResponse(session, request.getMessage());
                }
            }

            // Handle conversational flow
            if (isConversationStart(userMessage)) {
                return createConversationStartResponse(session);
            } else if (isOptionSelection(userMessage)) {
                return handleOptionSelection(request, session);
            } else {
                // Handle direct questions using existing logic
                return handleDirectQuestion(request);
            }
        } catch (Exception e) {
            logger.error("Error processing chat message for session {}: {}", request.getSessionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process chat message: " + e.getMessage(), e);
        }
    }

    private boolean shouldHandleAsEnquiry(SessionData session, String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("enquire") || 
               lowerMessage.contains("enquiry") || 
               lowerMessage.contains("inquiry") ||
               lowerMessage.contains("buy") ||
               lowerMessage.contains("purchase") ||
               lowerMessage.contains("contact dealer");
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

    private ChatResponse createConversationStartResponse(SessionData session) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Hello! I'm your Hyundai Mobis assistant. I can help you explore genuine accessories and parts for your Hyundai vehicle and find the best prices.");
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

        saveChatMessage(session.getSessionId(), "Conversation start", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse handleOptionSelection(ChatRequest request, SessionData session) {
        String userMessage = request.getMessage().trim();
        String sessionId = session.getSessionId();

        logger.info("Handling option selection: [{}]", userMessage);

        // Check for navigation options first
        if (isNavigationOption(userMessage)) {
            return handleNavigationOption(session, userMessage);
        }

        // Handle main menu options
        if (userMessage.equalsIgnoreCase("Browse Accessories")) {
            session.setCurrentState("awaiting_vehicle_selection");
            return createVehicleSelectionResponse(session);
        }  else if (userMessage.equalsIgnoreCase("Browse Parts")) {
            session.setCurrentState("awaiting_part_type_selection");
            session.setIsAccessoryFlow(false);  // Mark this as parts flow
            return createPartsTypesResponse(session);
    }else if (userMessage.equalsIgnoreCase("Find Dealers & Distributors")) {
            session.setCurrentState("awaiting_location_selection");
            return createLocationSelectionResponse(session);
        } else if (userMessage.equalsIgnoreCase("Check Current Offers")) {
            return createOffersResponse(session);
        } else if (userMessage.equalsIgnoreCase("Get Product Support")) {
            return createProductSupportResponse(session);
        } else if (userMessage.equalsIgnoreCase("Find Distributors")) {
            return createDistributorsResponse(session);
        }

        // Handle numeric selection
        if (userMessage.matches("^\\d+$")) {
            int selection = Integer.parseInt(userMessage);
            return handleNumericSelection(selection, session);
        }

        return createInvalidSelectionResponse(session);
    }

    private ChatResponse handleNumericSelection(int selection, SessionData session) {
        switch (selection) {
            case 1:
                session.setCurrentState("awaiting_vehicle_selection");
                return createVehicleSelectionResponse(session);
            case 2:
                session.setCurrentState("awaiting_part_type_selection");
                session.setIsAccessoryFlow(false);
                return createPartsTypesResponse(session);
            case 3:
                session.setCurrentState("awaiting_location_selection");
                return createLocationSelectionResponse(session);
            case 4:
                return createOffersResponse(session);
            case 5:
                return createProductSupportResponse(session);
            default:
                return createInvalidSelectionResponse(session);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private ChatResponse createVehicleSelectionResponse(SessionData session) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage("Great! Let's find the perfect accessories for your Hyundai. Which vehicle model do you own?");
        response.setQuestion("Select your vehicle model:");

        // Get model names from config
        response.setOptions(config.getModelNames());

        response.setConversationEnd(false);
        response.setConversationType("vehicle_selection");

        saveChatMessage(session.getSessionId(), "Vehicle selection prompt", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createTypeSelectionResponse(SessionData session, String vehicleModel) {
        try {
            // Store selected model in session
            SessionData.ModelInfo modelInfo = new SessionData.ModelInfo();
            modelInfo.setName(vehicleModel);
                        HyundaiMobisConfig.Model configModel = config.getModelByName(vehicleModel);
            if (configModel != null) {
                modelInfo.setId(Long.parseLong(configModel.getModelId()));
            }
            session.setSelectedModel(modelInfo);
            session.setIsAccessoryFlow(true);

            logger.info("API CALL: getAccessoryTypesForModelFunction (model: {})", vehicleModel);
            AccessoryTypesForModelResponse typesResponse = mobisApiService.getAccessoryTypesForModel(vehicleModel);

            if (!typesResponse.success() || typesResponse.types().isEmpty()) {
                return createErrorResponse(session.getSessionId(), "No accessories found for " + vehicleModel);
            }

            StringBuilder message = new StringBuilder();
            message.append(String.format("🚗 **Accessories for %s**\n\n", vehicleModel));
            message.append("Select a category to explore:\n\n");

            List<String> options = new ArrayList<>();
            for (TypeInfo type : typesResponse.types()) {
                String icon = getCategoryIcon(type.typeName());
                message.append(String.format("%s **%s**\n", icon, type.typeName()));
                options.add(type.typeName());

                // Store type info in session
                SessionData.AccessoryTypeInfo typeInfo = new SessionData.AccessoryTypeInfo();
                typeInfo.setId(type.typeId());
                typeInfo.setName(type.typeName());
                session.addTypeInfo(type.typeName(), typeInfo);
            }

            session.setCurrentState("awaiting_type_selection");

            ChatResponse response = new ChatResponse();
            response.setSessionId(session.getSessionId());
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select accessory category:");
            response.setOptions(options);
            response.setConversationEnd(false);
            response.setConversationType("type_selection");

            saveChatMessage(session.getSessionId(), "Type selection for " + vehicleModel, message.toString(), "getAccessoryTypesForModelFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting types: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Error loading accessory categories");
        }
    }

    private ChatResponse handleTypeSelection(SessionData session, String selectedType) {
        try {
            SessionData.ModelInfo model = session.getSelectedModel();
            SessionData.AccessoryTypeInfo typeInfo = session.getTypeInfo(selectedType);

            if (model == null || typeInfo == null) {
                return createErrorResponse(session.getSessionId(), "Session expired. Please start over.");
            }

            session.setSelectedType(typeInfo);

            logger.info("API CALL: getAccessorySubTypesForTypeFunction (model: {}, typeId: {})", model.getName(), typeInfo.getId());
            AccessorySubTypesForTypeResponse subTypesResponse = mobisApiService.getAccessorySubTypesForType(model.getName(), typeInfo.getId());

            if (!subTypesResponse.success() || subTypesResponse.subTypes().isEmpty()) {
                // If no subtypes, show all accessories for this type
                return showAccessoriesForType(session, model.getName(), typeInfo.getId(), selectedType);
            }

            StringBuilder message = new StringBuilder();
            message.append(String.format("📂 **%s - Subcategories**\n\n", selectedType));
            message.append("Select a subcategory:\n\n");

            List<String> options = new ArrayList<>();
            for (SubTypeInfo subType : subTypesResponse.subTypes()) {
                message.append(String.format("• **%s**\n", subType.subTypeName()));
                options.add(subType.subTypeName());

                // Store subtype info in session
                SessionData.AccessorySubTypeInfo subTypeInfo = new SessionData.AccessorySubTypeInfo();
                subTypeInfo.setId(subType.subTypeId());
                subTypeInfo.setName(subType.subTypeName());
                session.addSubTypeInfo(subType.subTypeName(), subTypeInfo);
            }

            session.setCurrentState("awaiting_subtype_selection");

            ChatResponse response = new ChatResponse();
            response.setSessionId(session.getSessionId());
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select subcategory:");
            response.setOptions(options);
            response.setConversationEnd(false);
            response.setConversationType("subtype_selection");

            saveChatMessage(session.getSessionId(), "Subtype selection for " + selectedType, message.toString(), "getAccessorySubTypesForTypeFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error handling type selection: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Error loading subcategories");
        }
    }

    private ChatResponse handleSubTypeSelection(SessionData session, String selectedSubType) {
        try {
            SessionData.ModelInfo model = session.getSelectedModel();
            SessionData.AccessoryTypeInfo typeInfo = session.getSelectedType();
            SessionData.AccessorySubTypeInfo subTypeInfo = session.getSubTypeInfo(selectedSubType);

            if (model == null || typeInfo == null || subTypeInfo == null) {
                return createErrorResponse(session.getSessionId(), "Session expired. Please start over.");
            }

            session.setSelectedSubType(subTypeInfo);

            logger.info("API CALL: getAccessoriesFilteredFunction (model: {}, typeId: {}, subTypeId: {})",
                    model.getName(), typeInfo.getId(), subTypeInfo.getId());
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesFiltered(model.getName(), typeInfo.getId(), subTypeInfo.getId());

            return displayAccessories(session, model.getName(), accessories, selectedSubType);
        } catch (Exception e) {
            logger.error("Error handling subtype selection: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Error loading accessories");
        }
    }

    private ChatResponse showAccessoriesForType(SessionData session, String modelName, Long typeId, String typeName) {
        try {
            logger.info("API CALL: getAccessoriesFilteredFunction (model: {}, typeId: {}, no subType)", modelName, typeId);
            MobisAccessoriesResponse accessories = mobisApiService.getAccessoriesFiltered(modelName, typeId, null);

            return displayAccessories(session, modelName, accessories, typeName);
        } catch (Exception e) {
            logger.error("Error showing accessories: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Error loading accessories");
        }
    }

    private ChatResponse displayAccessories(SessionData session, String modelName, MobisAccessoriesResponse accessories, String categoryName) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("🛍️ **%s Accessories for %s**\n\n", categoryName, modelName));

        if (accessories.accessories().isEmpty()) {
            message.append("No accessories found in this category.");
            session.setShowingAccessories(false);
            session.clearAccessoriesList();
        } else {
            message.append(String.format("Found %d accessories:\n\n", accessories.accessories().size()));

            int index = 1;
            session.clearAccessoriesList(); // Clear previous accessories
            
            for (MobisAccessoriesResponse.Accessory accessory : accessories.accessories()) {
                String icon = getCategoryIcon(accessory.type());
                message.append(String.format("%d. %s **%s**\n", index++, icon, accessory.accessoryName()));
                message.append(String.format("   💰 **MRP: ₹%.0f**\n", accessory.mrp()));
                message.append(String.format("   📝 %s\n", cleanHtml(accessory.body())));
                message.append(String.format("   🔧 Part Code: %s\n", accessory.accessoryCode()));
                message.append(String.format("   🏷️ Category: %s - %s\n\n", accessory.type(), accessory.subType()));
                
                // Store accessory info in session
                SessionData.AccessoryInfo info = new SessionData.AccessoryInfo();
                info.setId(accessory.id());
                info.setName(accessory.accessoryName());
                info.setDescription(cleanHtml(accessory.body()));
                info.setPrice(String.valueOf(accessory.mrp()));
                session.addAccessoryToList(info);
            }
            
            session.setShowingAccessories(true);
            session.setAccessoriesCount(accessories.accessories().size());
            
            message.append("\n📝 **To enquire about any accessory, type the number (e.g., '1' for the first item)**");
        }

        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
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

        saveChatMessage(session.getSessionId(), "Accessories display", message.toString(), "getAccessoriesFilteredFunction", null);
        return response;
    }

    private ChatResponse createLocationSelectionResponse(SessionData session) {
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
            response.setSessionId(session.getSessionId());
            response.setSuccess(true);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(message.toString());
            response.setQuestion("Select a state or enter your city:");
            response.setOptions(stateOptions);
            response.setConversationEnd(false);
            response.setConversationType("location_selection");

            saveChatMessage(session.getSessionId(), "Location selection prompt", message.toString(), "getStatesFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting states: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Sorry, I couldn't retrieve the location information. Please try again.");
        }
    }

    private ChatResponse createOffersResponse(SessionData session) {
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
            response.setSessionId(session.getSessionId());
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

            saveChatMessage(session.getSessionId(), "Offers request", message.toString(), "getOffersFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting offers: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Sorry, I couldn't retrieve the current offers. Please try again.");
        }
    }

    private ChatResponse createProductSupportResponse(SessionData session) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
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

        saveChatMessage(session.getSessionId(), "Product support request", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createInvalidSelectionResponse(SessionData session) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
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

        saveChatMessage(session.getSessionId(), "Invalid selection", response.getMessage(), null, null);
        return response;
    }

    private ChatResponse createDealersForLocationResponse(SessionData session, String location) {
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
            response.setSessionId(session.getSessionId());
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

            saveChatMessage(session.getSessionId(), "Dealers in " + location, message.toString(), "findDealersFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting dealers for {}: {}", location, e.getMessage());
            return createErrorResponse(session.getSessionId(), "Sorry, I couldn't retrieve the dealers in " + location + ". Please try again.");
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
    private ChatResponse handleNavigationOption(SessionData session, String option) {
        if (option.equalsIgnoreCase("Browse Another Category")) {
            SessionData.ModelInfo model = session.getSelectedModel();
            if (model != null && session.isAccessoryFlow()) {
                return createTypeSelectionResponse(session, model.getName());
            } else if (!session.isAccessoryFlow()) {
                return createPartsTypesResponse(session);
            }
        } else if (option.equalsIgnoreCase("Start over")) {
            // Clear session state
            session.resetFlow();
            return createConversationStartResponse(session);
        }
        return createConversationStartResponse(session);
    }

    // Add distributor response method
    private ChatResponse createDistributorsResponse(SessionData session) {
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
            response.setSessionId(session.getSessionId());
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

            saveChatMessage(session.getSessionId(), "Distributors request", message.toString(), "findDistributorsFunction", null);
            return response;
        } catch (Exception e) {
            logger.error("Error getting distributors: {}", e.getMessage());
            return createErrorResponse(session.getSessionId(), "Sorry, I couldn't retrieve the distributor information. Please try again.");
        }
    }

    // Clear session data method
    public void clearSessionData(String sessionId) {
        sessions.remove(sessionId);
    }

    // Get session info method for debugging
    public Map<String, Object> getSessionInfo(String sessionId) {
        SessionData session = sessions.get(sessionId);
        if (session != null) {
            Map<String, Object> info = new HashMap<>();
            info.put("sessionId", session.getSessionId());
            info.put("currentState", session.getCurrentState());
            info.put("isAccessoryFlow", session.isAccessoryFlow());
            info.put("flowStage", session.getFlowStage());
            info.put("isInEnquiryFlow", session.isInEnquiryFlow());
            info.put("hasSelectedModel", session.getSelectedModel() != null);
            info.put("hasSelectedAccessory", session.getSelectedAccessory() != null);
            info.put("enquiryStage", session.getEnquiryStage());
            return info;
        }
        return Collections.emptyMap();
    }

    // Parts flow methods
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

private ChatResponse createPartsTypesResponse(SessionData session) {
    try {
        logger.info("API CALL: getAllPartTypesFunction");
        var partTypes = mobisApiService.getAllPartTypes();
        
        if (!partTypes.success() || partTypes.types().isEmpty()) {
            return createErrorResponse(session.getSessionId(), "Unable to fetch parts categories.");
        }
        
        StringBuilder message = new StringBuilder();
        message.append("🔧 **Genuine Hyundai Parts Categories**\n\n");
        message.append("Select a category to view available parts:\n\n");
        
        List<String> options = new ArrayList<>();
        session.clearPartTypeInfoMap(); // Add this method to SessionData
        
        for (var type : partTypes.types()) {
            String icon = getPartCategoryIcon(type.typeName());
            message.append(String.format("%s **%s**\n", icon, type.typeName()));
            options.add(type.typeName());
            
            // Store part type info in session
            SessionData.PartTypeInfo typeInfo = new SessionData.PartTypeInfo();
            typeInfo.setId(type.typeId());
            typeInfo.setName(type.typeName());
            typeInfo.setDescription(type.code());
            session.addPartTypeInfo(type.typeName(), typeInfo);
        }
        
        session.setCurrentState("awaiting_part_type_selection");
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(message.toString());
        response.setQuestion("Select parts category:");
        response.setOptions(options);
        response.setConversationEnd(false);
        response.setConversationType("part_types");
        
        saveChatMessage(session.getSessionId(), "Parts type selection", message.toString(), "getAllPartTypesFunction", null);
        return response;
    } catch (Exception e) {
        logger.error("Error getting part types: {}", e.getMessage());
        return createErrorResponse(session.getSessionId(), "Error loading parts categories");
    }
}

private ChatResponse handlePartTypeSelection(SessionData session, String selectedType) {
    try {
        SessionData.PartTypeInfo typeInfo = session.getPartTypeInfo(selectedType);
        
        if (typeInfo == null) {
            return createErrorResponse(session.getSessionId(), "Invalid selection. Please try again.");
        }
        
        session.setSelectedPartType(typeInfo);
        
        logger.info("API CALL: getPartsByTypeFunction (typeId: {})", typeInfo.getId());
        var partsResponse = mobisApiService.getPartsByType(typeInfo.getId());
        
        StringBuilder message = new StringBuilder();
        message.append(String.format("🔧 **%s Parts**\n\n", selectedType));
        
        if (partsResponse.parts().isEmpty()) {
            message.append("No parts available in this category.");
            session.setShowingParts(false);
            session.clearPartsList();
        } else {
            message.append(String.format("Found %d parts:\n\n", partsResponse.parts().size()));
            
            int index = 1;
            session.clearPartsList(); // Clear previous parts
            
            for (var part : partsResponse.parts()) {
                message.append(String.format("%d. 🔩 **%s**\n", index++, part.partName()));
                message.append(String.format("   📝 %s\n", part.description()));
                message.append(String.format("   🔧 Part Code: %s\n\n", part.partCode()));
                
                // Store part info in session
                SessionData.PartInfo info = new SessionData.PartInfo();
                info.setId(part.partId());
                info.setName(part.partName());
                info.setPartNumber(part.partCode());
                info.setDescription(part.description());
                session.addPartToList(info);
            }
            
            session.setShowingParts(true);
            session.setPartsCount(partsResponse.parts().size());
            
            message.append("\n⚠️ **Note**: Parts prices vary by dealer. Contact your nearest dealer for pricing.\n");
            message.append("\n📝 **To enquire about any part, type the number (e.g., '1' for the first item)**");
        }
        
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getSessionId());
        response.setSuccess(true);
        response.setTimestamp(LocalDateTime.now());
        response.setMessage(message.toString());
        response.setQuestion("What would you like to do?");
        response.setOptions(Arrays.asList(
            "Browse Another Category",
            "Find Nearest Dealer",
            "Start over"
        ));
        response.setConversationEnd(false);
        response.setConversationType("parts_list");
        
        saveChatMessage(session.getSessionId(), "Parts display", message.toString(), "getPartsByTypeFunction", null);
        return response;
    } catch (Exception e) {
        logger.error("Error handling part type selection: {}", e.getMessage());
        return createErrorResponse(session.getSessionId(), "Error loading parts");
    }
}
}
