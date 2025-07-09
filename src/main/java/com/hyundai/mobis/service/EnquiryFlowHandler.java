package com.hyundai.mobis.service;

import com.hyundai.mobis.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EnquiryFlowHandler {
    
    private static final int STATES_PER_PAGE = 5;
    private static final int CITIES_PER_PAGE = 4;
    private static final int DEALERS_PER_PAGE = 3;
    private static final int DISTRIBUTORS_PER_PAGE = 3;
    
    @Autowired
    private MobisApiService mobisApiService;
    
    @Autowired
    private EnquirySubmissionService enquirySubmissionService;
    
    // Main entry point for enquiry flow
    public ChatResponse handleEnquiryFlow(SessionData session, String userInput) {
        EnquiryForm form = session.getEnquiryForm();
        
        // Initialize form if not exists
        if (form == null) {
            return initiateEnquiryForm(session);
        }
        
        // Check if we're in dealer/distributor selection flow
        DealerDistributorFlow dealerFlow = session.getDealerDistributorFlow();
        if (dealerFlow != null && dealerFlow.getCurrentStep() != null) {
            return handleDealerDistributorFlow(session, userInput);
        }
        
        // Handle main enquiry flow stages
        String stage = session.getEnquiryStage();
        switch (stage) {
            case "INIT":
                return handleEnquiryConfirmation(session, userInput);
            case "DEALER_DISTRIBUTOR":
                return handleDealerDistributorFlow(session, userInput);
            case "CONTACT_DETAILS":
                return handleContactDetails(session, userInput);
            case "QUERY":
                return handleQueryInput(session, userInput);
            case "REVIEW":
                return handleReview(session, userInput);
            case "SUBMITTED":
                return handlePostSubmission(session, userInput);
            default:
                return initiateEnquiryForm(session);
        }
    }
    
    private ChatResponse initiateEnquiryForm(SessionData session) {
        EnquiryForm form = new EnquiryForm();
        
        // Pre-fill based on flow type
        if (session.isAccessoryFlow()) {
            form.setItemType("accessory");
            form.setModel(session.getSelectedModel().getName());
            form.setModelId(String.valueOf(session.getSelectedModel().getId()));
            form.setAccessoryName(session.getSelectedAccessory().getName());
            form.setAccessoryId(String.valueOf(session.getSelectedAccessory().getId()));
        } else {
            form.setItemType("part");
            form.setPartName(session.getSelectedPart().getName());
            form.setPartId(String.valueOf(session.getSelectedPart().getId()));
        }
        
        session.setEnquiryForm(form);
        session.setEnquiryStage("INIT");
        
        StringBuilder message = new StringBuilder();
        message.append("Great! I'll help you submit an enquiry.\n\n");
        message.append("📋 **Enquiry Details:**\n");
        
        if (session.isAccessoryFlow()) {
            message.append("• **Model**: ").append(form.getModel()).append("\n");
            message.append("• **Accessory**: ").append(form.getAccessoryName()).append("\n");
        } else {
            message.append("• **Part**: ").append(form.getPartName()).append("\n");
        }
        
        message.append("\nI'll need a few details to complete your enquiry:\n");
        message.append("• Preferred dealer/distributor\n");
        message.append("• Your contact information\n\n");
        
        List<ButtonResponse.Button> buttons = Arrays.asList(
            ButtonResponse.Button.create("continue", "✅ Continue with Enquiry"),
            ButtonResponse.Button.create("back", "🔙 Go Back"),
            ButtonResponse.Button.create("cancel", "❌ Cancel")
        );
        
        return ChatResponse.builder()
            .message(message.toString())
            .buttons(buttons)
            .responseType("buttons")
            .build();
    }
    
    private ChatResponse handleEnquiryConfirmation(SessionData session, String userInput) {
        if ("continue".equalsIgnoreCase(userInput)) {
            session.setEnquiryStage("DEALER_DISTRIBUTOR");
            return initiateDealerDistributorSelection(session);
        } else if ("back".equalsIgnoreCase(userInput)) {
            session.clearEnquiryForm();
            return ChatResponse.text("Taking you back to the previous selection...");
        } else if ("cancel".equalsIgnoreCase(userInput)) {
            session.clearEnquiryForm();
            return ChatResponse.text("Enquiry cancelled. How else can I help you?");
        }
        
        return initiateEnquiryForm(session);
    }
    
    // Dealer/Distributor Selection Flow
    private ChatResponse initiateDealerDistributorSelection(SessionData session) {
        DealerDistributorFlow flow = new DealerDistributorFlow();
        flow.setCurrentStep("TYPE_SELECTION");
        session.setDealerDistributorFlow(flow);
        
        List<ButtonResponse.Button> buttons = Arrays.asList(
            ButtonResponse.Button.create("dealer", "🏪 Dealer", "Select a dealer near you"),
            ButtonResponse.Button.create("distributor", "🏢 Distributor", "Select a distributor")
        );
        
        return ChatResponse.builder()
            .message("How would you like to receive your " + 
                    (session.isAccessoryFlow() ? "accessory" : "part") + "?")
            .buttons(buttons)
            .responseType("buttons")
            .build();
    }
    
    private ChatResponse handleDealerDistributorFlow(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        if (flow == null) {
            return initiateDealerDistributorSelection(session);
        }
        
        // Handle navigation commands
        if ("back".equalsIgnoreCase(userInput)) {
            flow.goBack();
            if (flow.getCurrentStep() == null || "TYPE_SELECTION".equals(flow.getCurrentStep())) {
                session.setEnquiryStage("INIT");
                return initiateEnquiryForm(session);
            }
        } else if ("start_over".equalsIgnoreCase(userInput)) {
            flow.reset();
            return initiateDealerDistributorSelection(session);
        }
        
        switch (flow.getCurrentStep()) {
            case "TYPE_SELECTION":
                return handleTypeSelection(session, userInput);
            case "STATE_SELECTION":
                return handleStateSelection(session, userInput);
            case "CITY_SELECTION":
                return handleCitySelection(session, userInput);
            case "DEALER_SELECTION":
                return handleDealerSelection(session, userInput);
            case "DISTRIBUTOR_SELECTION":
                return handleDistributorSelection(session, userInput);
            case "SEARCH_MODE":
                return handleDealerSearch(session, userInput);
            default:
                return continueToContactDetails(session);
        }
    }
    
    private ChatResponse handleTypeSelection(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        if ("dealer".equalsIgnoreCase(userInput)) {
            flow.setType("dealer");
            flow.setCurrentStep("STATE_SELECTION");
            return displayStates(session);
        } else if ("distributor".equalsIgnoreCase(userInput)) {
            flow.setType("distributor");
            flow.setCurrentStep("STATE_SELECTION");
            return displayStates(session);
        }
        
        return initiateDealerDistributorSelection(session);
    }
    
    private ChatResponse displayStates(SessionData session) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        try {
            List<StateInfo> states = "dealer".equals(flow.getType()) 
                ? mobisApiService.getDealerStates()
                : mobisApiService.getDistributorStates();
            
            if (states == null || states.isEmpty()) {
                return ChatResponse.builder()
                    .message("Sorry, unable to fetch states. Please try again.")
                    .buttons(Arrays.asList(
                        ButtonResponse.Button.create("retry", "🔄 Retry"),
                        ButtonResponse.Button.create("back", "↩ Go Back")
                    ))
                    .responseType("buttons")
                    .build();
            }
            
            String headerMessage = String.format(
                "🗺️ **Select Your State**\n" +
                "Contact Type: %s\n\n",
                "dealer".equals(flow.getType()) ? "🏪 Dealer" : "🏢 Distributor"
            );
            
            return createPaginatedResponse(
                states,
                flow.getCurrentPage(),
                headerMessage,
                state -> ButtonResponse.Button.create(
                    String.valueOf(state.getId()), 
                    state.getDescription()
                ),
                Arrays.asList(
                    ButtonResponse.Button.create("back", "↩ Change Contact Type"),
                    ButtonResponse.Button.create("start_over", "🔄 Start Over")
                )
            );
        } catch (Exception e) {
            log.error("Error fetching states", e);
            return ChatResponse.builder()
                .message("Sorry, there was an error fetching states. Please try again.")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("retry", "🔄 Retry"),
                    ButtonResponse.Button.create("back", "↩ Go Back")
                ))
                .responseType("buttons")
                .build();
        }
    }
    
    private ChatResponse handleStateSelection(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        // Handle pagination
        if ("next".equalsIgnoreCase(userInput)) {
            flow.incrementPage();
            return displayStates(session);
        } else if ("previous".equalsIgnoreCase(userInput)) {
            flow.decrementPage();
            return displayStates(session);
        }
        
        // Try to find selected state
        try {
            List<StateInfo> states = "dealer".equals(flow.getType()) 
                ? mobisApiService.getDealerStates()
                : mobisApiService.getDistributorStates();
            
            Optional<StateInfo> selectedState = states.stream()
                .filter(state -> String.valueOf(state.getId()).equals(userInput))
                .findFirst();
            
                        if (selectedState.isPresent()) {
                flow.setSelectedState(selectedState.get().getDescription());
                flow.setSelectedStateId(String.valueOf(selectedState.get().getId()));
                
                // Update enquiry form with state info
                session.getEnquiryForm().setStateName(selectedState.get().getDescription());
                session.getEnquiryForm().setStateId(String.valueOf(selectedState.get().getId()));
                
                if ("dealer".equals(flow.getType())) {
                    flow.setCurrentStep("CITY_SELECTION");
                    flow.setCurrentPage(0); // Reset page for cities
                    return displayCities(session);
                } else {
                    flow.setCurrentStep("DISTRIBUTOR_SELECTION");
                    flow.setCurrentPage(0);
                    return displayDistributors(session);
                }
            }
        } catch (Exception e) {
            log.error("Error handling state selection", e);
        }
        
        return displayStates(session);
    }
    
    private ChatResponse displayCities(SessionData session) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        try {
            List<CityInfo> cities = mobisApiService.getCities(flow.getSelectedStateId(), "2");
            
            if (cities == null || cities.isEmpty()) {
                return ChatResponse.builder()
                    .message("No cities found in " + flow.getSelectedState() + 
                            ". Please select a different state.")
                    .buttons(Arrays.asList(
                        ButtonResponse.Button.create("back", "↩ Back to State Selection")
                    ))
                    .responseType("buttons")
                    .build();
            }
            
            String headerMessage = String.format(
                "🏙️ **Select Your City**\n" +
                "State: %s\n" +
                "Cities found: %d\n\n",
                flow.getSelectedState(),
                cities.size()
            );
            
            return createPaginatedResponse(
                cities,
                flow.getCurrentPage(),
                headerMessage,
                city -> ButtonResponse.Button.create(
                    String.valueOf(city.getId()), 
                    city.getDescription()
                ),
                Arrays.asList(
                    ButtonResponse.Button.create("back", "↩ Back to State"),
                    ButtonResponse.Button.create("start_over", "🔄 Start Over")
                )
            );
        } catch (Exception e) {
            log.error("Error fetching cities", e);
            return ChatResponse.builder()
                .message("Sorry, there was an error fetching cities. Please try again.")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("retry", "🔄 Retry"),
                    ButtonResponse.Button.create("back", "↩ Back to State")
                ))
                .responseType("buttons")
                .build();
        }
    }
    
    private ChatResponse handleCitySelection(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        // Handle pagination
        if ("next".equalsIgnoreCase(userInput)) {
            flow.incrementPage();
            return displayCities(session);
        } else if ("previous".equalsIgnoreCase(userInput)) {
            flow.decrementPage();
            return displayCities(session);
        }
        
        try {
            List<CityInfo> cities = mobisApiService.getCities(flow.getSelectedStateId(), "2");
            
            Optional<CityInfo> selectedCity = cities.stream()
                .filter(city -> String.valueOf(city.getId()).equals(userInput))
                .findFirst();
            
            if (selectedCity.isPresent()) {
                flow.setSelectedCity(selectedCity.get().getDescription());
                flow.setSelectedCityId(String.valueOf(selectedCity.get().getId()));
                
                // Update enquiry form
                session.getEnquiryForm().setCityName(selectedCity.get().getDescription());
                session.getEnquiryForm().setCityId(String.valueOf(selectedCity.get().getId()));
                
                flow.setCurrentStep("DEALER_SELECTION");
                flow.setCurrentPage(0);
                return displayDealers(session);
            }
        } catch (Exception e) {
            log.error("Error handling city selection", e);
        }
        
        return displayCities(session);
    }
    
    private ChatResponse displayDealers(SessionData session) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        try {
            List<DealerInfo> dealers = mobisApiService.getDealers(flow.getSelectedCityId(), "2");
            
            if (dealers == null || dealers.isEmpty()) {
                return ChatResponse.builder()
                    .message("No dealers found in " + flow.getSelectedCity() + 
                            ". Please select a different city.")
                    .buttons(Arrays.asList(
                        ButtonResponse.Button.create("back", "↩ Back to City Selection")
                    ))
                    .responseType("buttons")
                    .build();
            }
            
            String headerMessage = String.format(
                "🏪 **Select a Dealer**\n" +
                "Location: %s, %s\n" +
                "Dealers found: %d\n\n",
                flow.getSelectedCity(),
                flow.getSelectedState(),
                dealers.size()
            );
            
            // For dealers, show more information
            Function<DealerInfo, ButtonResponse.Button> dealerButtonMapper = dealer -> {
                String label = dealer.getDealerName();
                String description = String.format("%s, %s", 
                    dealer.getLocation() != null ? dealer.getLocation() : dealer.getCity(),
                    dealer.getDealerTypeDesc() != null ? dealer.getDealerTypeDesc() : "Dealer"
                );
                return ButtonResponse.Button.create(
                    String.valueOf(dealer.getId()),
                    label,
                    description
                );
            };
            
            ChatResponse response = createPaginatedResponse(
                dealers,
                flow.getCurrentPage(),
                headerMessage,
                dealerButtonMapper,
                Arrays.asList(
                    ButtonResponse.Button.create("search", "🔍 Search by Name"),
                    ButtonResponse.Button.create("back", "↩ Back to City"),
                    ButtonResponse.Button.create("start_over", "🔄 Start Over")
                )
            );
            
            // Add dealer details preview
            StringBuilder dealerDetails = new StringBuilder("\n📋 **Dealer Details:**\n");
            int start = flow.getCurrentPage() * DEALERS_PER_PAGE;
            int end = Math.min(start + DEALERS_PER_PAGE, dealers.size());
            
            for (int i = start; i < end; i++) {
                DealerInfo dealer = dealers.get(i);
                dealerDetails.append(String.format(
                    "\n**%d. %s**\n📍 %s\n📞 %s\n\n",
                    i + 1,
                    dealer.getDealerName(),
                    dealer.getAddress2() != null ? dealer.getAddress2() : dealer.getAddress(),
                    dealer.getPhone() != null ? dealer.getPhone() : dealer.getMobile1()
                ));
            }
            
            response.setMessage(response.getMessage() + dealerDetails.toString());
            return response;
        } catch (Exception e) {
            log.error("Error fetching dealers", e);
            return ChatResponse.builder()
                .message("Sorry, there was an error fetching dealers. Please try again.")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("retry", "🔄 Retry"),
                    ButtonResponse.Button.create("back", "↩ Back to City")
                ))
                .responseType("buttons")
                .build();
        }
    }
    
    private ChatResponse handleDealerSelection(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        // Handle special actions
        if ("search".equalsIgnoreCase(userInput)) {
            flow.setInSearchMode(true);
            flow.setCurrentStep("SEARCH_MODE");
            return ChatResponse.builder()
                .message("🔍 **Search for a Dealer**\n\n" +
                        "Type the dealer name or part of it to search.\n" +
                        "Example: 'Arise' or 'Hyundai'\n\n" +
                        "Type 'cancel' to go back to the list.")
                .allowTextInput(true)
                .responseType("text")
                .build();
        }
        
        // Handle pagination
        if ("next".equalsIgnoreCase(userInput)) {
            flow.incrementPage();
            return displayDealers(session);
        } else if ("previous".equalsIgnoreCase(userInput)) {
            flow.decrementPage();
            return displayDealers(session);
        }
        
        // Handle dealer selection
        try {
            List<DealerInfo> dealers = mobisApiService.getDealers(flow.getSelectedCityId(), "2");
            
            Optional<DealerInfo> selectedDealer = dealers.stream()
                .filter(dealer -> String.valueOf(dealer.getId()).equals(userInput))
                .findFirst();
            
            if (selectedDealer.isPresent()) {
                DealerInfo dealer = selectedDealer.get();
                flow.setSelectedEntity(dealer.getDealerName());
                flow.setSelectedEntityId(String.valueOf(dealer.getId()));
                flow.setSelectedEntityDetails(dealer.getContactInfo());
                
                // Update enquiry form
                EnquiryForm form = session.getEnquiryForm();
                form.setDealerOrDistributor("dealer");
                form.setDealerDistributorId(String.valueOf(dealer.getId()));
                form.setDealerDistributorName(dealer.getDealerName());
                form.setDealerDistributorDetails(
                    String.format("%s\n%s", dealer.getAddress2(), dealer.getContactInfo())
                );
                
                return continueToContactDetails(session);
            }
        } catch (Exception e) {
            log.error("Error handling dealer selection", e);
        }
        
        return displayDealers(session);
    }
    
    private ChatResponse handleDealerSearch(SessionData session, String searchInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        if ("cancel".equalsIgnoreCase(searchInput)) {
            flow.setInSearchMode(false);
            flow.setCurrentStep("DEALER_SELECTION");
            return displayDealers(session);
        }
        
        try {
            List<DealerInfo> allDealers = mobisApiService.getDealers(flow.getSelectedCityId(), "2");
            
            List<DealerInfo> filtered = allDealers.stream()
                .filter(dealer -> dealer.getDealerName().toLowerCase()
                    .contains(searchInput.toLowerCase()))
                .collect(Collectors.toList());
            
            if (filtered.isEmpty()) {
                return ChatResponse.builder()
                    .message("No dealers found matching '" + searchInput + "'.\n" +
                            "Try a different search term or type 'cancel' to go back.")
                    .allowTextInput(true)
                    .responseType("text")
                    .build();
            }
            
            // Show search results as buttons
            flow.setInSearchMode(false);
            flow.setCurrentStep("DEALER_SELECTION");
            
            String headerMessage = "🔍 **Search Results for '" + searchInput + "':**\n\n";
            
            List<ButtonResponse.Button> dealerButtons = filtered.stream()
                .limit(5)
                .map(dealer -> ButtonResponse.Button.create(
                    String.valueOf(dealer.getId()),
                    dealer.getDealerName(),
                    dealer.getLocation()
                ))
                .collect(Collectors.toList());
            
            List<ButtonResponse.Button> actionButtons = Arrays.asList(
                ButtonResponse.Button.create("search", "🔍 New Search"),
                ButtonResponse.Button.create("view_all", "📋 View All Dealers"),
                ButtonResponse.Button.create("back", "↩ Back")
            );
            
            return ChatResponse.builder()
                .message(headerMessage)
                .buttons(dealerButtons)
                .actionButtons(actionButtons)
                .responseType("buttons")
                .build();
        } catch (Exception e) {
            log.error("Error searching dealers", e);
            return ChatResponse.builder()
                .message("Error searching dealers. Please try again.")
                .allowTextInput(true)
                .responseType("text")
                .build();
        }
    }
    
    private ChatResponse displayDistributors(SessionData session) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        try {
            List<DistributorInfo> distributors = mobisApiService.getDistributorsByState(flow.getSelectedStateId());
            
            if (distributors == null || distributors.isEmpty()) {
                return ChatResponse.builder()
                    .message("No distributors found in " + flow.getSelectedState() + 
                            ". Please select a different state.")
                    .buttons(Arrays.asList(
                        ButtonResponse.Button.create("back", "↩ Back to State Selection")
                    ))
                    .responseType("buttons")
                    .build();
            }
            
            String headerMessage = String.format(
                "🏢 **Select a Distributor**\n" +
                "State: %s\n" +
                "Distributors found: %d\n\n",
                flow.getSelectedState(),
                distributors.size()
            );
            
            Function<DistributorInfo, ButtonResponse.Button> distributorButtonMapper = dist -> {
                String label = dist.getName();
                String description = dist.getLocation() != null 
                    ? dist.getLocation() 
                    : dist.getCity();
                return ButtonResponse.Button.create(
                    String.valueOf(dist.getId()),
                    label,
                    description
                );
            };
            
            ChatResponse response = createPaginatedResponse(
                distributors,
                flow.getCurrentPage(),
                headerMessage,
                distributorButtonMapper,
                Arrays.asList(
                    ButtonResponse.Button.create("back", "↩ Back to State"),
                    ButtonResponse.Button.create("start_over", "🔄 Start Over")
                )
            );
            
            // Add distributor details
            StringBuilder distDetails = new StringBuilder("\n📋 **Distributor Details:**\n");
            int start = flow.getCurrentPage() * DISTRIBUTORS_PER_PAGE;
            int end = Math.min(start + DISTRIBUTORS_PER_PAGE, distributors.size());
            
            for (int i = start; i < end; i++) {
                DistributorInfo dist = distributors.get(i);
                distDetails.append(String.format(
                    "\n**%d. %s**\n📍 %s\n%s\n\n",
                    i + 1,
                    dist.getName(),
                    dist.getAddress2() != null ? dist.getAddress2() : dist.getAddress(),
                    dist.getContactInfo()
                ));
            }
            
            response.setMessage(response.getMessage() + distDetails.toString());
            return response;
        } catch (Exception e) {
            log.error("Error fetching distributors", e);
            return ChatResponse.builder()
                .message("Sorry, there was an error fetching distributors. Please try again.")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("retry", "🔄 Retry"),
                    ButtonResponse.Button.create("back", "↩ Back to State")
                ))
                .responseType("buttons")
                .build();
        }
    }
    
    private ChatResponse handleDistributorSelection(SessionData session, String userInput) {
        DealerDistributorFlow flow = session.getDealerDistributorFlow();
        
        // Handle pagination
        if ("next".equalsIgnoreCase(userInput)) {
            flow.incrementPage();
            return displayDistributors(session);
        } else if ("previous".equalsIgnoreCase(userInput)) {
            flow.decrementPage();
            return displayDistributors(session);
        }
        
        try {
            List<DistributorInfo> distributors = mobisApiService.getDistributorsByState(flow.getSelectedStateId());
            
            Optional<DistributorInfo> selectedDist = distributors.stream()
                .filter(dist -> String.valueOf(dist.getId()).equals(userInput))
                .findFirst();
            
            if (selectedDist.isPresent()) {
                DistributorInfo dist = selectedDist.get();
                flow.setSelectedEntity(dist.getName());
                flow.setSelectedEntityId(String.valueOf(dist.getId()));
                flow.setSelectedEntityDetails(dist.getContactInfo());
                
                // Update enquiry form
                EnquiryForm form = session.getEnquiryForm();
                form.setDealerOrDistributor("distributor");
                form.setDealerDistributorId(String.valueOf(dist.getId()));
                form.setDealerDistributorName(dist.getName());
                form.setDealerDistributorDetails(
                    String.format("%s\n%s", dist.getAddress2(), dist.getContactInfo())
                );
                
                return continueToContactDetails(session);
            }
        } catch (Exception e) {
            log.error("Error handling distributor selection", e);
        }
        
        return displayDistributors(session);
    }
    
        private ChatResponse continueToContactDetails(SessionData session) {
        session.setEnquiryStage("CONTACT_DETAILS");
        session.setDealerDistributorFlow(null); // Clear dealer flow
        
        EnquiryForm form = session.getEnquiryForm();
        
        StringBuilder message = new StringBuilder();
        message.append("✅ **Selection Complete!**\n\n");
        message.append("**Selected ").append(form.getDealerOrDistributor()).append(":**\n");
        message.append(form.getDealerDistributorName()).append("\n");
        message.append(form.getDealerDistributorDetails()).append("\n\n");
        
        message.append("Now I need your contact details. Please provide:\n");
        message.append("• Your Name\n");
        message.append("• Email Address\n");
        message.append("• Mobile Number\n\n");
        
        message.append("Please type all details in one message, separated by commas.\n");
        message.append("Example: John Doe, john@email.com, 9876543210");
        
        return ChatResponse.builder()
            .message(message.toString())
            .allowTextInput(true)
            .responseType("text")
            .buttons(Arrays.asList(
                ButtonResponse.Button.create("back", "↩ Change Selection")
            ))
            .build();
    }
    
    private ChatResponse handleContactDetails(SessionData session, String userInput) {
        if ("back".equalsIgnoreCase(userInput)) {
            session.setEnquiryStage("DEALER_DISTRIBUTOR");
            return initiateDealerDistributorSelection(session);
        }
        
        // Parse contact details
        String[] parts = userInput.split(",");
        if (parts.length < 3) {
            return ChatResponse.builder()
                .message("Please provide all three details separated by commas:\n" +
                        "Format: Name, Email, Mobile Number\n" +
                        "Example: John Doe, john@email.com, 9876543210")
                .allowTextInput(true)
                .responseType("text")
                .build();
        }
        
        String name = parts[0].trim();
        String email = parts[1].trim();
        String mobile = parts[2].trim();
        
        // Basic validation
        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            return ChatResponse.builder()
                .message("All fields are required. Please try again.\n" +
                        "Format: Name, Email, Mobile Number")
                .allowTextInput(true)
                .responseType("text")
                .build();
        }
        
        // Update form
        EnquiryForm form = session.getEnquiryForm();
        form.setCustomerName(name);
        form.setEmail(email);
        form.setMobileNo(mobile);
        
        // Check for duplicate enquiry
        String itemId = form.getItemType().equals("accessory") ? form.getAccessoryId() : form.getPartId();
        if (enquirySubmissionService.checkDuplicateEnquiry(email, mobile, itemId, form.getItemType())) {
            return ChatResponse.builder()
                .message("⚠️ You have already submitted an enquiry for this item in the last 24 hours.\n\n" +
                        "Would you like to continue anyway?")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("continue", "Yes, Continue"),
                    ButtonResponse.Button.create("cancel", "No, Cancel")
                ))
                .responseType("buttons")
                .build();
        }
        
        // Move to query stage
        session.setEnquiryStage("QUERY");
        return promptForQuery(session);
    }
    
    private ChatResponse promptForQuery(SessionData session) {
        return ChatResponse.builder()
            .message("Do you have any specific requirements or questions? (Optional)\n\n" +
                    "Type your query or click 'Skip' to proceed without adding a query.")
            .allowTextInput(true)
            .buttons(Arrays.asList(
                ButtonResponse.Button.create("skip", "Skip ➡️"),
                ButtonResponse.Button.create("back", "↩ Edit Contact Details")
            ))
            .responseType("mixed")
            .build();
    }
    
    private ChatResponse handleQueryInput(SessionData session, String userInput) {
        if ("back".equalsIgnoreCase(userInput)) {
            session.setEnquiryStage("CONTACT_DETAILS");
            return continueToContactDetails(session);
        }
        
        if (!"skip".equalsIgnoreCase(userInput)) {
            session.getEnquiryForm().setQuery(userInput);
        }
        
        session.setEnquiryStage("REVIEW");
        return showReviewScreen(session);
    }
    
    private ChatResponse showReviewScreen(SessionData session) {
        EnquiryForm form = session.getEnquiryForm();
        
        StringBuilder review = new StringBuilder();
        review.append("📝 **Review Your Enquiry**\n\n");
        
        review.append("**Product Details:**\n");
        if ("accessory".equals(form.getItemType())) {
            review.append("• Model: ").append(form.getModel()).append("\n");
            review.append("• Accessory: ").append(form.getAccessoryName()).append("\n");
        } else {
            review.append("• Part: ").append(form.getPartName()).append("\n");
        }
        
        review.append("\n**Contact Through:**\n");
        review.append("• Type: ").append(form.getDealerOrDistributor()).append("\n");
        review.append("• Name: ").append(form.getDealerDistributorName()).append("\n");
        review.append("• Location: ").append(form.getStateName());
        if (form.getCityName() != null) {
            review.append(", ").append(form.getCityName());
        }
        review.append("\n");
        
        review.append("\n**Your Details:**\n");
        review.append("• Name: ").append(form.getCustomerName()).append("\n");
        review.append("• Email: ").append(form.getEmail()).append("\n");
        review.append("• Mobile: ").append(form.getMobileNo()).append("\n");
        
        if (form.getQuery() != null && !form.getQuery().isEmpty()) {
            review.append("\n**Additional Query:**\n");
            review.append(form.getQuery()).append("\n");
        }
        
        List<ButtonResponse.Button> buttons = Arrays.asList(
            ButtonResponse.Button.create("submit", "✅ Submit Enquiry"),
            ButtonResponse.Button.create("edit_contact", "✏️ Edit Contact Details"),
            ButtonResponse.Button.create("edit_query", "✏️ Edit Query"),
            ButtonResponse.Button.create("cancel", "❌ Cancel Enquiry")
        );
        
        return ChatResponse.builder()
            .message(review.toString())
            .buttons(buttons)
            .responseType("buttons")
            .build();
    }
    
    private ChatResponse handleReview(SessionData session, String userInput) {
        switch (userInput.toLowerCase()) {
            case "submit":
                return submitEnquiry(session);
            case "edit_contact":
                session.setEnquiryStage("CONTACT_DETAILS");
                return continueToContactDetails(session);
            case "edit_query":
                session.setEnquiryStage("QUERY");
                return promptForQuery(session);
            case "cancel":
                session.clearEnquiryForm();
                return ChatResponse.text("Enquiry cancelled. How else can I help you?");
            default:
                return showReviewScreen(session);
        }
    }
    
    private ChatResponse submitEnquiry(SessionData session) {
        EnquiryForm form = session.getEnquiryForm();
        
        // Submit to database
        EnquirySubmissionService.EnquirySubmissionResult result = 
            enquirySubmissionService.submitEnquiry(form, session.getSessionId());
        
        if (result.isSuccess()) {
            session.setEnquiryStage("SUBMITTED");
            
            StringBuilder confirmation = new StringBuilder();
            confirmation.append("✅ **Enquiry Submitted Successfully!**\n\n");
            confirmation.append("Reference Number: **").append(result.getReferenceNumber()).append("**\n\n");
            confirmation.append("Thank you for your enquiry. ");
            confirmation.append("A ").append(form.getDealerOrDistributor());
            confirmation.append(" representative will contact you within 24-48 hours.\n\n");
            confirmation.append("You'll receive a confirmation email at ").append(form.getEmail()).append("\n\n");
            
            List<ButtonResponse.Button> buttons = Arrays.asList(
                ButtonResponse.Button.create("browse_more", "🛍️ Browse More " + 
                    (form.getItemType().equals("accessory") ? "Accessories" : "Parts")),
                ButtonResponse.Button.create("new_search", "🔍 Start New Search"),
                ButtonResponse.Button.create("track_enquiry", "📋 Track Enquiry"),
                ButtonResponse.Button.create("end", "👋 End Conversation")
            );
            
            return ChatResponse.builder()
                .message(confirmation.toString())
                .buttons(buttons)
                .responseType("buttons")
                .build();
        } else {
            return ChatResponse.builder()
                .message(result.getMessage() + "\n\nWould you like to try again?")
                .buttons(Arrays.asList(
                    ButtonResponse.Button.create("retry", "🔄 Try Again"),
                    ButtonResponse.Button.create("cancel", "❌ Cancel")
                ))
                .responseType("buttons")
                .build();
        }
    }
    
    private ChatResponse handlePostSubmission(SessionData session, String userInput) {
        session.clearEnquiryForm(); // Clear the form data
        
        switch (userInput.toLowerCase()) {
            case "browse_more":
                return ChatResponse.text("Let me show you more options...");
            case "new_search":
                return ChatResponse.text("What would you like to search for?");
            case "track_enquiry":
                return ChatResponse.text("Please enter your reference number to track your enquiry:");
            case "end":
                return ChatResponse.text("Thank you for using Hyundai Mobis! Have a great day! 👋");
            default:
                return ChatResponse.text("How can I help you further?");
        }
    }
    
    // Helper method for paginated responses
    private <T> ChatResponse createPaginatedResponse(
            List<T> items,
            int currentPage,
            String headerMessage,
            Function<T, ButtonResponse.Button> buttonMapper,
            List<ButtonResponse.Button> additionalButtons) {
        
        int itemsPerPage = getItemsPerPage(items);
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        // Ensure current page is valid
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
            startIndex = currentPage * itemsPerPage;
            endIndex = Math.min(startIndex + itemsPerPage, items.size());
        }
        
        List<ButtonResponse.Button> itemButtons = items.subList(startIndex, endIndex).stream()
            .map(buttonMapper)
            .collect(Collectors.toList());
        
        // Create navigation buttons
        List<ButtonResponse.Button> navigationButtons = new ArrayList<>();
        if (currentPage > 0) {
            navigationButtons.add(ButtonResponse.Button.create("previous", "◀ Previous"));
        }
        
        if (totalPages > 1) {
            navigationButtons.add(ButtonResponse.Button.create("page_info", 
                String.format("Page %d of %d", currentPage + 1, totalPages), 
                "Current page").builder().disabled(true).build());
        }
        
        if (currentPage < totalPages - 1) {
            navigationButtons.add(ButtonResponse.Button.create("next", "Next ▶"));
        }
        
        return ChatResponse.builder()
            .message(headerMessage)
            .buttons(itemButtons)
            .navigationButtons(navigationButtons)
            .actionButtons(additionalButtons)
            .responseType("buttons")
            .build();
    }
    
    private int getItemsPerPage(List<?> items) {
        if (items.isEmpty()) return 5;
        
        Object first = items.get(0);
        if (first instanceof StateInfo) return STATES_PER_PAGE;
        if (first instanceof CityInfo) return CITIES_PER_PAGE;
        if (first instanceof DealerInfo) return DEALERS_PER_PAGE;
        if (first instanceof DistributorInfo) return DISTRIBUTORS_PER_PAGE;
        
        return 5; // default
    }
}
