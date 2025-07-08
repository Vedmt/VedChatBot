package com.hyundai.mobis.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsResponse {
    private long totalUsers;
    private long totalConversations;
    private long totalMessages;
    private List<PopularBrand> popularBrands;
    private List<ConversationStat> conversationStats;
    private Map<String, Long> functionUsageStats;
    private LocalDateTime generatedAt;

    public AnalyticsResponse() {
        this.generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public List<PopularBrand> getPopularBrands() {
        return popularBrands;
    }

    public void setPopularBrands(List<PopularBrand> popularBrands) {
        this.popularBrands = popularBrands;
    }

    public List<ConversationStat> getConversationStats() {
        return conversationStats;
    }

    public void setConversationStats(List<ConversationStat> conversationStats) {
        this.conversationStats = conversationStats;
    }

    public Map<String, Long> getFunctionUsageStats() {
        return functionUsageStats;
    }

    public void setFunctionUsageStats(Map<String, Long> functionUsageStats) {
        this.functionUsageStats = functionUsageStats;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    // Nested classes
    public static class PopularBrand {
        private String brandName;
        private long queryCount;
        private String queryType; // "models", "vehicle_types", "general"

        public PopularBrand(String brandName, long queryCount, String queryType) {
            this.brandName = brandName;
            this.queryCount = queryCount;
            this.queryType = queryType;
        }

        // Getters and Setters
        public String getBrandName() {
            return brandName;
        }

        public void setBrandName(String brandName) {
            this.brandName = brandName;
        }

        public long getQueryCount() {
            return queryCount;
        }

        public void setQueryCount(long queryCount) {
            this.queryCount = queryCount;
        }

        public String getQueryType() {
            return queryType;
        }

        public void setQueryType(String queryType) {
            this.queryType = queryType;
        }
    }

    public static class ConversationStat {
        private String date;
        private long conversationCount;
        private long messageCount;

        public ConversationStat(String date, long conversationCount, long messageCount) {
            this.date = date;
            this.conversationCount = conversationCount;
            this.messageCount = messageCount;
        }

        // Getters and Setters
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getConversationCount() {
            return conversationCount;
        }

        public void setConversationCount(long conversationCount) {
            this.conversationCount = conversationCount;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }
    }
} 