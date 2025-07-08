package com.hyundai.mobis.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.Map;

@Service
public class NhtsaApiService {
    private static final String NHTSA_MAKES_URL = "https://vpic.nhtsa.dot.gov/api/vehicles/GetMakesForVehicleType/car?format=json";
    private static final String NHTSA_MODELS_URL = "https://vpic.nhtsa.dot.gov/api/vehicles/GetModelsForMake/%s?format=json";
    private static final String NHTSA_TYPES_URL = "https://vpic.nhtsa.dot.gov/api/vehicles/GetVehicleTypesForMake/%s?format=json";
    private final RestTemplate restTemplate = new RestTemplate();

    public List<String> getCarMakes() {
        Map<String, Object> response = restTemplate.getForObject(NHTSA_MAKES_URL, Map.class);
        if (response == null || !response.containsKey("Results")) {
            throw new RuntimeException("No results from NHTSA API");
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("Results");
        return results.stream()
                .map(r -> (String) r.get("MakeName"))
                .toList();
    }

    public List<String> getModelsForMake(String make) {
        String url = String.format(NHTSA_MODELS_URL, make);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("Results")) {
            throw new RuntimeException("No results from NHTSA API");
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("Results");
        return results.stream()
                .map(r -> (String) r.get("Model_Name"))
                .toList();
    }

    public List<String> getVehicleTypesForMake(String make) {
        String url = String.format(NHTSA_TYPES_URL, make);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("Results")) {
            throw new RuntimeException("No results from NHTSA API");
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("Results");
        return results.stream()
                .map(r -> (String) r.get("VehicleTypeName"))
                .distinct()
                .toList();
    }
} 