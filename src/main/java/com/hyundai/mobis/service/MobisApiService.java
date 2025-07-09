package com.hyundai.mobis.service;

import com.hyundai.mobis.config.HyundaiMobisConfig;
import com.hyundai.mobis.dto.DealerSearchRequest;
import com.hyundai.mobis.dto.DealerSearchResponse;
import com.hyundai.mobis.dto.PartSearchRequest;
import com.hyundai.mobis.dto.PartSearchResponse;
import com.hyundai.mobis.dto.MobisAccessoriesRequest;
import com.hyundai.mobis.dto.MobisAccessoriesResponse;
import com.hyundai.mobis.dto.AccessoryTypesResponse;
import com.hyundai.mobis.dto.AccessorySubTypesResponse;
import com.hyundai.mobis.dto.StatesResponse;
import com.hyundai.mobis.dto.DistributorInfo;
import org.springframework.core.ParameterizedTypeReference;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.WarrantyCheckRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.WarrantyCheckResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.VehicleInfoRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.VehicleInfoResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class MobisApiService {

    private static final Logger logger = LoggerFactory.getLogger(MobisApiService.class);

    private final RestTemplate restTemplate;
    private final HyundaiMobisConfig config;

    @Value("${mobis.api.base-url:https://hyundaimobisin.com}")
    private String baseUrl;

    @Autowired
    public MobisApiService(HyundaiMobisConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
        configureRestTemplate();
    }

    private void configureRestTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getApi().getTimeout());
        factory.setReadTimeout(config.getApi().getTimeout());
        restTemplate.setRequestFactory(factory);
    }

    // Get states for dealers
    public List<StateInfo> getDealerStates() {
        try {
            ResponseEntity<List<StateInfo>> response = restTemplate.exchange(
                baseUrl + "/service/parts/getStates",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<StateInfo>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching dealer states", e);
            return new ArrayList<>();
        }
    }
    
    // Get states for distributors
    public List<StateInfo> getDistributorStates() {
        try {
            ResponseEntity<List<StateInfo>> response = restTemplate.exchange(
                baseUrl + "/service/parts/getDistributorStates",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<StateInfo>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching distributor states", e);
            return new ArrayList<>();
        }
    }
    
    // Get cities for a state
    public List<CityInfo> getCities(String stateId, String dealerCategory) {
        try {
            String url = String.format("%s/service/parts/getCities?dealerCategory=%s&stateId=%s",
                baseUrl, dealerCategory, stateId);
                
            ResponseEntity<List<CityInfo>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CityInfo>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching cities for state: " + stateId, e);
            return new ArrayList<>();
        }
    }
    
    // Get dealers for a city
    public List<DealerInfo> getDealers(String cityId, String dealerCategoryId) {
        try {
            String url = String.format("%s/service/accessories/getDealers?cityId=%s&dealerCategoryId=%s",
                baseUrl, cityId, dealerCategoryId);
                
            ResponseEntity<List<DealerInfo>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DealerInfo>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching dealers for city: " + cityId, e);
            return new ArrayList<>();
        }
    }
    
    // Get distributors for a state
    public List<DistributorInfo> getDistributorsByState(String stateId) {
        try {
            String url = String.format("%s/service/accessories/getByState?stateId=%s", baseUrl, stateId);
            
            ResponseEntity<List<DistributorInfo>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DistributorInfo>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching distributors for state: " + stateId, e);
            return new ArrayList<>();
        }
    }
    
    // New methods for type/subtype categorization
    public AccessoryTypesForModelResponse getAccessoryTypesForModel(String modelName) {
        try {
            HyundaiMobisConfig.Model model = config.getModelByName(modelName);
            if (model == null) {
                return new AccessoryTypesForModelResponse(List.of(), false, "Model not found: " + modelName);
            }

            // Get all accessories for the model
            MobisAccessoriesResponse accessories = getAccessoriesByModel(
                    new MobisAccessoriesRequest(model.getModelId(), model.getYear(), null, null)
            );

            // Extract unique types
            Map<Long, String> uniqueTypes = new LinkedHashMap<>();
            for (var accessory : accessories.accessories()) {
                uniqueTypes.putIfAbsent(accessory.typeId(), accessory.type());
            }

            List<TypeInfo> types = uniqueTypes.entrySet().stream()
                    .map(e -> new TypeInfo(e.getKey(), e.getValue()))
                    .toList();

            return new AccessoryTypesForModelResponse(types, true, "Success");
        } catch (Exception e) {
            logger.error("Error getting types for model {}: {}", modelName, e.getMessage());
            return new AccessoryTypesForModelResponse(List.of(), false, "Error: " + e.getMessage());
        }
    }

    public AccessorySubTypesForTypeResponse getAccessorySubTypesForType(String modelName, Long typeId) {
        try {
            HyundaiMobisConfig.Model model = config.getModelByName(modelName);
            if (model == null) {
                return new AccessorySubTypesForTypeResponse(List.of(), false, "Model not found: " + modelName);
            }

            // Get all accessories for the model
            MobisAccessoriesResponse accessories = getAccessoriesByModel(
                    new MobisAccessoriesRequest(model.getModelId(), model.getYear(), null, null)
            );

            // Filter by typeId and extract unique subtypes
            Map<Long, String> uniqueSubTypes = new LinkedHashMap<>();
            for (var accessory : accessories.accessories()) {
                if (accessory.typeId().equals(typeId)) {
                    uniqueSubTypes.putIfAbsent(accessory.subTypeId(), accessory.subType());
                }
            }

            List<SubTypeInfo> subTypes = uniqueSubTypes.entrySet().stream()
                    .map(e -> new SubTypeInfo(e.getKey(), e.getValue()))
                    .toList();

            return new AccessorySubTypesForTypeResponse(subTypes, true, "Success");
        } catch (Exception e) {
            logger.error("Error getting subtypes: {}", e.getMessage());
            return new AccessorySubTypesForTypeResponse(List.of(), false, "Error: " + e.getMessage());
        }
    }

    public MobisAccessoriesResponse getAccessoriesFiltered(String modelName, Long typeId, Long subTypeId) {
        try {
            HyundaiMobisConfig.Model model = config.getModelByName(modelName);
            if (model == null) {
                return new MobisAccessoriesResponse(List.of(), 0, false, "Model not found: " + modelName);
            }

            // Get all accessories for the model
            MobisAccessoriesResponse allAccessories = getAccessoriesByModel(
                    new MobisAccessoriesRequest(model.getModelId(), model.getYear(), null, null)
            );

            // Filter by typeId and subTypeId
            List<MobisAccessoriesResponse.Accessory> filtered = allAccessories.accessories().stream()
                    .filter(acc -> (typeId == null || acc.typeId().equals(typeId)) &&
                            (subTypeId == null || acc.subTypeId().equals(subTypeId)))
                    .toList();

            return new MobisAccessoriesResponse(filtered, filtered.size(), true, "Success");
        } catch (Exception e) {
            logger.error("Error filtering accessories: {}", e.getMessage());
            return new MobisAccessoriesResponse(List.of(), 0, false, "Error: " + e.getMessage());
        }
    }

    // Response DTOs for new methods
    public record TypeInfo(Long typeId, String typeName) {}
    public record SubTypeInfo(Long subTypeId, String subTypeName) {}
    public record AccessoryTypesForModelResponse(List<TypeInfo> types, boolean success, String message) {}
    public record AccessorySubTypesForTypeResponse(List<SubTypeInfo> subTypes, boolean success, String message) {}

    // Existing methods
    @Cacheable(value = "parts", key = "#request.toString()")
    public PartSearchResponse searchParts(PartSearchRequest request) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v1/parts/search")
                    .queryParamIfPresent("partNumber", request.partNumber() != null ?
                            java.util.Optional.of(request.partNumber()) : java.util.Optional.empty())
                    .queryParamIfPresent("vehicleModel", request.vehicleModel() != null ?
                            java.util.Optional.of(request.vehicleModel()) : java.util.Optional.empty())
                    .queryParamIfPresent("vehicleYear", request.vehicleYear() != null ?
                            java.util.Optional.of(request.vehicleYear()) : java.util.Optional.empty())
                    .queryParamIfPresent("category", request.category() != null ?
                            java.util.Optional.of(request.category()) : java.util.Optional.empty())
                    .build()
                    .toUriString();

            return createMockPartSearchResponse(request);

        } catch (Exception e) {
            throw new RuntimeException("Failed to search parts", e);
        }
    }

    @Cacheable(value = "dealers", key = "#request.toString()")
    public DealerSearchResponse findDealers(DealerSearchRequest request) {
        try {
            return createMockDealerSearchResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find dealers", e);
        }
    }

    @Cacheable(value = "distributors", key = "#request.toString()")
    public DealerSearchResponse findDistributors(DealerSearchRequest request) {
        try {
            return createMockDistributorSearchResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find distributors", e);
        }
    }

    @Cacheable(value = "warranty", key = "#request.toString()")
    public WarrantyCheckResponse checkWarranty(WarrantyCheckRequest request) {
        try {
            return createMockWarrantyResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check warranty", e);
        }
    }

    @Cacheable(value = "offers", key = "#request.toString()")
    public OffersResponse getCurrentOffers(OffersRequest request) {
        try {
            return createMockOffersResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get offers", e);
        }
    }

    @Cacheable(value = "vehicleInfo", key = "#request.toString()")
    public VehicleInfoResponse getVehicleInfo(VehicleInfoRequest request) {
        try {
            return createMockVehicleInfoResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get vehicle info", e);
        }
    }

    @Cacheable(value = "accessoryTypes", key = "'all'")
    public AccessoryTypesResponse getAllAccessoryTypes() {
        try {
            String url = config.getApi().getBaseUrl() + "/service/accessories/getAllTypes";

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null) {
                List<String> types = Arrays.stream(response.getBody())
                        .map(map -> (String) map.get("description"))
                        .distinct()
                        .collect(Collectors.toList());

                return new AccessoryTypesResponse(types, true, "Success");
            }

            return createMockAccessoryTypesResponse();

        } catch (Exception e) {
            logger.error("Error fetching accessory types: {}", e.getMessage());
            return createMockAccessoryTypesResponse();
        }
    }

    @Cacheable(value = "accessorySubTypes", key = "'all'")
    public AccessorySubTypesResponse getAllAccessorySubTypes() {
        try {
            String url = config.getApi().getBaseUrl() + "/service/accessories/getAllSubTypes";

            ResponseEntity<Map[]> response = restTemplate.getForEntity(url, Map[].class);

            if (response.getBody() != null) {
                List<String> subTypes = Arrays.stream(response.getBody())
                        .map(map -> (String) map.get("description"))
                        .distinct()
                        .collect(Collectors.toList());

                return new AccessorySubTypesResponse(subTypes, true, "Success");
            }

            return createMockAccessorySubTypesResponse();

        } catch (Exception e) {
            logger.error("Error fetching accessory subtypes: {}", e.getMessage());
            return createMockAccessorySubTypesResponse();
        }
    }

    @Cacheable(value = "accessoriesByModel", key = "#request.toString()")
    public MobisAccessoriesResponse getAccessoriesByModel(MobisAccessoriesRequest request) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(config.getApi().getBaseUrl() + "/service/accessories/getByModelIdYear")
                    .queryParam("modelId", request.modelId())
                    .queryParam("year", request.year())
                    .build()
                    .toUriString();

            ResponseEntity<MobisAccessoriesResponse.Accessory[]> response = restTemplate.getForEntity(url, MobisAccessoriesResponse.Accessory[].class);

            if (response.getBody() != null) {
                List<MobisAccessoriesResponse.Accessory> accessories = Arrays.asList(response.getBody());
                return new MobisAccessoriesResponse(accessories, accessories.size(), true, "Success");
            } else {
                return new MobisAccessoriesResponse(new ArrayList<>(), 0, false, "No accessories found");
            }

        } catch (Exception e) {
            logger.error("Error fetching accessories: {}", e.getMessage());
            return createMockAccessoriesByModelResponse(request);
        }
    }

    @Cacheable(value = "states", key = "'all'")
    public StatesResponse getStates() {
        try {
            String url = baseUrl + "/service/accessories/getStates";
            return createMockStatesResponse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get states", e);
        }
    }

    @Cacheable(value = "distributorStates", key = "'all'")
    public StatesResponse getDistributorStates() {
        try {
            String url = baseUrl + "/service/accessories/getDistributorStates";
            return createMockDistributorStatesResponse();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get distributor states", e);
        }
    }

    // All mock data creation methods remain the same
    private PartSearchResponse createMockPartSearchResponse(PartSearchRequest request) {
        List<PartSearchResponse.Part> parts = Arrays.asList(
                new PartSearchResponse.Part(
                        "BRK-2023-SNTA",
                        "Brake Pad Set - Front",
                        "Genuine Hyundai brake pads for optimal performance",
                        "BRK-2023-SNTA",
                        new BigDecimal("12999.99"),
                        "Brakes",
                        true,
                        Arrays.asList("Front", "Brake System", "Safety"),
                        "2020-2023 Sonata",
                        "12 months"
                ),
                new PartSearchResponse.Part(
                        "BRK-2023-SNTA-R",
                        "Brake Pad Set - Rear",
                        "Genuine Hyundai brake pads for optimal performance",
                        "BRK-2023-SNTA-R",
                        new BigDecimal("10999.99"),
                        "Brakes",
                        true,
                        Arrays.asList("Rear", "Brake System", "Safety"),
                        "2020-2023 Sonata",
                        "12 months"
                )
        );

        return new PartSearchResponse(parts, parts.size(), true);
    }

    private DealerSearchResponse createMockDealerSearchResponse(DealerSearchRequest request) {
        String location = request.location() != null ? request.location().toLowerCase() : "";
        String city = request.city() != null ? request.city() : "";
        String state = request.state() != null ? request.state() : "";

        String primaryLocation = "";
        if (!city.isEmpty()) {
            primaryLocation = city;
        } else if (!state.isEmpty()) {
            primaryLocation = state;
        } else if (!location.isEmpty()) {
            primaryLocation = location;
        } else {
            primaryLocation = "Mumbai";
        }

        List<DealerSearchResponse.Dealer> mockDealers = new ArrayList<>();

        if (primaryLocation.toLowerCase().contains("jharkhand") || primaryLocation.toLowerCase().contains("ranchi") || primaryLocation.toLowerCase().contains("jamshedpur")) {
            mockDealers.addAll(Arrays.asList(
                    new DealerSearchResponse.Dealer(
                            "DEAL_JH001",
                            "Ranchi Hyundai Service Center",
                            "123 MG Road, Harmu",
                            "Ranchi",
                            "Jharkhand",
                            "834002",
                            "+91-651-234-5678",
                            "service@ranchihyundai.com",
                            "www.ranchihyundai.com",
                            23.3441,
                            85.3096,
                            3.2,
                            Arrays.asList("Parts Sales", "Service", "Warranty"),
                            "Mon-Sat: 9AM-7PM, Sun: 10AM-5PM",
                            true
                    ),
                    new DealerSearchResponse.Dealer(
                            "DEAL_JH002",
                            "Jamshedpur Hyundai Center",
                            "456 Sakchi Market Road",
                            "Jamshedpur",
                            "Jharkhand",
                            "831001",
                            "+91-657-345-6789",
                            "info@jamshedpurhyundai.com",
                            "www.jamshedpurhyundai.com",
                            22.8046,
                            86.2029,
                            7.8,
                            Arrays.asList("Parts Sales", "Service", "Body Shop"),
                            "Mon-Sat: 8AM-8PM, Sun: Closed",
                            true
                    ),
                    new DealerSearchResponse.Dealer(
                            "DEAL_JH003",
                            "Dhanbad Hyundai Service",
                            "789 Bank More Circle",
                            "Dhanbad",
                            "Jharkhand",
                            "826001",
                            "+91-326-456-7890",
                            "service@dhanbadhyundai.com",
                            "www.dhanbadhyundai.com",
                            23.7957,
                            86.4304,
                            12.5,
                            Arrays.asList("Parts Sales", "Service", "Warranty"),
                            "Mon-Sat: 9AM-6PM, Sun: 10AM-4PM",
                            true
                    )
            ));
        } else if (primaryLocation.toLowerCase().contains("delhi") || primaryLocation.toLowerCase().contains("ncr")) {
            mockDealers.addAll(Arrays.asList(
                    new DealerSearchResponse.Dealer(
                            "DEAL_DL001",
                            "Delhi Hyundai Service Center",
                            "123 Connaught Place",
                            "New Delhi",
                            "Delhi",
                            "110001",
                            "+91-11-234-5678",
                            "service@delhihyundai.com",
                            "www.delhihyundai.com",
                            28.6139,
                            77.2090,
                            2.1,
                            Arrays.asList("Parts Sales", "Service", "Warranty"),
                            "Mon-Sat: 9AM-7PM, Sun: 10AM-5PM",
                            true
                    ),
                    new DealerSearchResponse.Dealer(
                            "DEAL_DL002",
                            "Gurgaon Hyundai Center",
                            "456 Cyber City Road",
                            "Gurgaon",
                            "Haryana",
                            "122002",
                            "+91-124-345-6789",
                            "info@gurgaonhyundai.com",
                            "www.gurgaonhyundai.com",
                            28.4595,
                            77.0266,
                            5.7,
                            Arrays.asList("Parts Sales", "Service", "Body Shop"),
                            "Mon-Sat: 8AM-8PM, Sun: Closed",
                            true
                    )
            ));
        } else {
            mockDealers.addAll(Arrays.asList(
                    new DealerSearchResponse.Dealer(
                            "DEAL001",
                            "Metro Hyundai Service Center",
                            "123 Main Street",
                            "Mumbai",
                            "Maharashtra",
                            "400001",
                            "+91-22-1234-5678",
                            "service@metrohyundai.com",
                            "www.metrohyundai.com",
                            19.0760,
                            72.8777,
                            5.2,
                            Arrays.asList("Parts Sales", "Service", "Warranty"),
                            "Mon-Sat: 9AM-7PM, Sun: 10AM-5PM",
                            true
                    ),
                    new DealerSearchResponse.Dealer(
                            "DEAL002",
                            "Premium Hyundai Center",
                            "456 Commercial Road",
                            "Mumbai",
                            "Maharashtra",
                            "400002",
                            "+91-22-2345-6789",
                            "info@premiumhyundai.com",
                            "www.premiumhyundai.com",
                            19.0896,
                            72.8656,
                            8.7,
                            Arrays.asList("Parts Sales", "Service", "Body Shop"),
                            "Mon-Sat: 8AM-8PM, Sun: Closed",
                            true
                    )
            ));
        }

        return new DealerSearchResponse(mockDealers, mockDealers.size());
    }

    private DealerSearchResponse createMockDistributorSearchResponse(DealerSearchRequest request) {
        var mockDistributors = Arrays.asList(
                new DealerSearchResponse.Dealer(
                        "DIST001",
                        "Mobis Parts Distribution Hub",
                        "789 Industrial Avenue",
                        request.city() != null ? request.city() : "Chennai",
                        request.state() != null ? request.state() : "Tamil Nadu",
                        request.zipCode() != null ? request.zipCode() : "600001",
                        "+91-44-1234-5678",
                        "orders@mobisdist.com",
                        "www.mobisdistribution.com",
                        13.0827,
                        80.2707,
                        12.5,
                        Arrays.asList("Wholesale Parts", "Bulk Orders", "Dealer Supply"),
                        "Mon-Fri: 8AM-6PM, Sat: 9AM-2PM",
                        true
                )
        );

        return new DealerSearchResponse(mockDistributors, mockDistributors.size());
    }

    private WarrantyCheckResponse createMockWarrantyResponse(WarrantyCheckRequest request) {
        boolean isUnderWarranty = java.util.concurrent.ThreadLocalRandom.current().nextBoolean();
        return new WarrantyCheckResponse(
                isUnderWarranty,
                isUnderWarranty ? "Active" : "Expired",
                isUnderWarranty ? "2025-12-31" : "2023-06-30",
                "Standard Parts Warranty"
        );
    }

    private OffersResponse createMockOffersResponse(OffersRequest request) {
        var mockOffers = Arrays.asList(
                new OffersResponse.Offer(
                        "Winter Service Special",
                        "Get 20% off on all brake pads and filters",
                        "20%",
                        "2024-03-31",
                        Arrays.asList("Brake Pads", "Air Filters", "Oil Filters"),
                        "Valid at participating dealers only"
                ),
                new OffersResponse.Offer(
                        "Genuine Parts Loyalty Program",
                        "Buy 3 get 1 free on selected accessories",
                        "25%",
                        "2024-06-30",
                        Arrays.asList("Floor Mats", "Seat Covers", "Car Care Products"),
                        "Cannot be combined with other offers"
                )
        );

        return new OffersResponse(mockOffers, mockOffers.size());
    }

    private VehicleInfoResponse createMockVehicleInfoResponse(VehicleInfoRequest request) {
        return new VehicleInfoResponse(
                request.vin(),
                "Sonata",
                "2023",
                "Luxury",
                "2.0L Turbo",
                "Automatic",
                "Gasoline",
                Arrays.asList("Sunroof", "Navigation", "Premium Audio"),
                "Active",
                "2025-12-31"
        );
    }

    private AccessoryTypesResponse createMockAccessoryTypesResponse() {
        List<String> types = Arrays.asList(
                "Interior Accessories",
                "Exterior Accessories",
                "Performance Accessories",
                "Safety Accessories",
                "Comfort Accessories",
                "Technology Accessories"
        );

        return new AccessoryTypesResponse(types, true, "Success");
    }

    private AccessorySubTypesResponse createMockAccessorySubTypesResponse() {
        List<String> subTypes = Arrays.asList(
                "Floor Mats",
                "Seat Covers",
                "Dashboard Accessories",
                "Bumper Accessories",
                "Lighting Accessories",
                "Wheel Accessories",
                "Body Kits",
                "Performance Exhaust",
                "Air Filters",
                "Brake Accessories",
                "Child Safety Seats",
                "First Aid Kits",
                "Cushions",
                "Storage Solutions",
                "Entertainment Systems",
                "Navigation Accessories"
        );

        return new AccessorySubTypesResponse(subTypes, true, "Success");
    }

    private MobisAccessoriesResponse createMockAccessoriesByModelResponse(MobisAccessoriesRequest request) {
        List<MobisAccessoriesResponse.Accessory> accessories = new ArrayList<>();

        if ("27".equals(request.modelId()) || "new-i20".equals(request.modelId())) {
            accessories.addAll(Arrays.asList(
                    new MobisAccessoriesResponse.Accessory(
                            1162L,
                            "FRONT BUMPER BEZEL",
                            "BVF27IH003",
                            "Enhance the appearance of your car with these chrome front bezel garnish.",
                            1347L,
                            "Exteriors",
                            1436L,
                            "Garnish",
                            new BigDecimal("989.0"),
                            "front-bumper-bezel-1162",
                            "front-bumper-bezel",
                            "FRONT BUMPER BEZEL",
                            "3372-FRONT-BUMPER-BEZEL-BVF27IH003-.jpg"
                    ),
                    new MobisAccessoriesResponse.Accessory(
                            1163L,
                            "TAIL LAMP GARNISH",
                            "BVF24IH000",
                            "Highlight the persona of your car Tail lamp with Superb elegance in every detail.",
                            1347L,
                            "Exteriors",
                            1436L,
                            "Garnish",
                            new BigDecimal("1202.0"),
                            "tail-lamp-garnish-1163",
                            "tail-lamp-garnish",
                            "TAIL LAMP GARNISH",
                            "3373-TAIL-LAMP-GARNISH-BVF24IH000-.jpg"
                    )
            ));
        }

        return new MobisAccessoriesResponse(accessories, accessories.size(), true, "Success");
    }

    private StatesResponse createMockStatesResponse() {
        List<String> states = Arrays.asList(
                "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
                "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
                "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
                "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
                "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
                "Uttar Pradesh", "Uttarakhand", "West Bengal"
        );

        return new StatesResponse(states, true, "Success");
    }

    private StatesResponse createMockDistributorStatesResponse() {
        List<String> states = Arrays.asList(
                "Maharashtra", "Karnataka", "Tamil Nadu", "Telangana", "Andhra Pradesh",
                "Gujarat", "Rajasthan", "Madhya Pradesh", "Uttar Pradesh", "Delhi",
                "Haryana", "Punjab", "West Bengal", "Kerala", "Odisha"
        );

        return new StatesResponse(states, true, "Success");
    }
}
