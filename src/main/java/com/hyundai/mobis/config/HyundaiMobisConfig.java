package com.hyundai.mobis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "hyundai.mobis")
@EnableConfigurationProperties
public class HyundaiMobisConfig {

    private Api api = new Api();
    private List<Model> models;
    private Map<String, Model> modelMap;

    public static class Api {
        private String baseUrl;
        private int timeout;

        // Getters and setters
        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public static class Model {
        private String name;
        private String modelId;
        private String year;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }
    }

    // Getters and setters
    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public List<Model> getModels() {
        return models;
    }

    public void setModels(List<Model> models) {
        this.models = models;
        // Build model map for quick lookup
        this.modelMap = models.stream()
                .collect(Collectors.toMap(Model::getName, model -> model));
    }

    public Model getModelByName(String name) {
        return modelMap.get(name);
    }

    public List<String> getModelNames() {
        return models.stream()
                .map(Model::getName)
                .collect(Collectors.toList());
    }
}