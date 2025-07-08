package com.hyundai.mobis.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class CatFactService {
    private static final String CAT_FACT_URL = "https://catfact.ninja/fact";
    private final RestTemplate restTemplate = new RestTemplate();

    public String getCatFact() {
        Map<String, Object> response = restTemplate.getForObject(CAT_FACT_URL, Map.class);
        if (response == null || !response.containsKey("fact")) {
            throw new RuntimeException("No cat fact found");
        }
        return (String) response.get("fact");
    }
} 