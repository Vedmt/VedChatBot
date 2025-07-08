package com.hyundai.mobis.service;

import com.hyundai.mobis.dto.AnalyticsResponse;
import com.hyundai.mobis.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    public AnalyticsResponse generateAnalytics() {
        logger.info("Generating analytics report...");
        
        AnalyticsResponse analytics = new AnalyticsResponse();
        
        try {
            // Get basic counts
            analytics.setTotalMessages(chatMessageRepository.count());
            analytics.setTotalUsers(chatMessageRepository.countDistinctSessions());
            analytics.setTotalConversations(chatMessageRepository.countDistinctSessions()); // Assuming one conversation per session
            
            // Get popular brands
            analytics.setPopularBrands(getPopularBrands());
            
            // Get function usage stats
            analytics.setFunctionUsageStats(getFunctionUsageStats());
            
            // Get conversation stats by date
            analytics.setConversationStats(getConversationStatsByDate());
            
            logger.info("Analytics generated successfully - Total users: {}, Total messages: {}", 
                       analytics.getTotalUsers(), analytics.getTotalMessages());
            
        } catch (Exception e) {
            logger.error("Error generating analytics: {}", e.getMessage(), e);
        }
        
        return analytics;
    }
    
    private List<AnalyticsResponse.PopularBrand> getPopularBrands() {
        try {
            // Get all chat messages and extract brand mentions
            var allMessages = chatMessageRepository.findAll();
            Map<String, Long> brandCounts = new HashMap<>();
            Map<String, String> brandTypes = new HashMap<>();
            
            List<String> popularBrands = Arrays.asList("toyota", "ford", "honda", "bmw", "mercedes-benz", 
                                                      "audi", "hyundai", "kia", "chevrolet", "nissan");
            
            for (var message : allMessages) {
                String userMessage = message.getUserMessage().toLowerCase();
                String botResponse = message.getBotResponse().toLowerCase();
                String functionsCalled = message.getFunctionsCalled();
                
                // Check for brand mentions in user messages and bot responses
                for (String brand : popularBrands) {
                    if (userMessage.contains(brand) || botResponse.contains(brand)) {
                        brandCounts.merge(brand, 1L, Long::sum);
                        
                        // Determine query type based on functions called
                        if (functionsCalled != null) {
                            if (functionsCalled.contains("getModelsForMakeFunction")) {
                                brandTypes.put(brand, "models");
                            } else if (functionsCalled.contains("getVehicleTypesForMakeFunction")) {
                                brandTypes.put(brand, "vehicle_types");
                            } else {
                                brandTypes.put(brand, "general");
                            }
                        }
                    }
                }
            }
            
            return brandCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> new AnalyticsResponse.PopularBrand(
                            capitalize(entry.getKey()), 
                            entry.getValue(),
                            brandTypes.getOrDefault(entry.getKey(), "general")
                    ))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error getting popular brands: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Map<String, Long> getFunctionUsageStats() {
        try {
            var allMessages = chatMessageRepository.findAll();
            Map<String, Long> functionStats = new HashMap<>();
            
            for (var message : allMessages) {
                String functionsCalled = message.getFunctionsCalled();
                if (functionsCalled != null && !functionsCalled.trim().isEmpty()) {
                    functionStats.merge(functionsCalled, 1L, Long::sum);
                }
            }
            
            return functionStats;
        } catch (Exception e) {
            logger.error("Error getting function usage stats: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    private List<AnalyticsResponse.ConversationStat> getConversationStatsByDate() {
        try {
            var allMessages = chatMessageRepository.findAll();
            Map<String, Set<String>> sessionsByDate = new HashMap<>();
            Map<String, Long> messagesByDate = new HashMap<>();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (var message : allMessages) {
                String date = message.getTimestamp().toLocalDate().format(formatter);
                
                // Track unique sessions per date
                sessionsByDate.computeIfAbsent(date, k -> new HashSet<>()).add(message.getSessionId());
                
                // Count messages per date
                messagesByDate.merge(date, 1L, Long::sum);
            }
            
            return sessionsByDate.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new AnalyticsResponse.ConversationStat(
                            entry.getKey(),
                            entry.getValue().size(),
                            messagesByDate.getOrDefault(entry.getKey(), 0L)
                    ))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error getting conversation stats by date: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
} 