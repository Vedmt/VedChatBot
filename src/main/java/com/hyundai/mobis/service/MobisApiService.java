package com.hyundai.mobis.service;

import com.hyundai.mobis.dto.DealerSearchRequest;
import com.hyundai.mobis.dto.DealerSearchResponse;
import com.hyundai.mobis.dto.PartSearchRequest;
import com.hyundai.mobis.dto.PartSearchResponse;
import com.hyundai.mobis.dto.MobisAccessoriesRequest;
import com.hyundai.mobis.dto.MobisAccessoriesResponse;
import com.hyundai.mobis.dto.AccessoryTypesResponse;
import com.hyundai.mobis.dto.AccessorySubTypesResponse;
import com.hyundai.mobis.dto.StatesResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.OffersResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.WarrantyCheckRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.WarrantyCheckResponse;
import com.hyundai.mobis.functions.MobisApiFunctions.VehicleInfoRequest;
import com.hyundai.mobis.functions.MobisApiFunctions.VehicleInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MobisApiService {

    private final RestTemplate restTemplate;

    @Value("${mobis.api.base-url:https://hyundaimobisin.com}")
    private String baseUrl;

    @Value("${mobis.api.timeout}")
    private int timeout;

    public MobisApiService() {
        this.restTemplate = new RestTemplate();
        // Configure timeout and other settings
    }

    @Cacheable(value = "parts", key = "#request.toString()")
    public PartSearchResponse searchParts(PartSearchRequest request) {
        try {
            // For demo purposes, return mock data
            // In production, this would make actual API calls
            
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

            // For demo - return mock data
            return createMockPartSearchResponse(request);
            
            // Production code would be:
            // ResponseEntity<PartSearchResponse> response = restTemplate.getForEntity(url, PartSearchResponse.class);
            // return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to search parts", e);
        }
    }

    @Cacheable(value = "dealers", key = "#request.toString()")
    public DealerSearchResponse findDealers(DealerSearchRequest request) {
        try {
            // Note: Real dealer API endpoint is not available on Hyundai Mobis API
            // Using mock data that varies based on location input
            return createMockDealerSearchResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find dealers", e);
        }
    }

    @Cacheable(value = "distributors", key = "#request.toString()")
    public DealerSearchResponse findDistributors(DealerSearchRequest request) {
        try {
            // Mock implementation for demo
            return createMockDistributorSearchResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find distributors", e);
        }
    }

    @Cacheable(value = "warranty", key = "#request.toString()")
    public WarrantyCheckResponse checkWarranty(WarrantyCheckRequest request) {
        try {
            // Mock implementation for demo
            return createMockWarrantyResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check warranty", e);
        }
    }

    @Cacheable(value = "offers", key = "#request.toString()")
    public OffersResponse getCurrentOffers(OffersRequest request) {
        try {
            // Mock implementation for demo
            return createMockOffersResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get offers", e);
        }
    }

    @Cacheable(value = "vehicleInfo", key = "#request.toString()")
    public VehicleInfoResponse getVehicleInfo(VehicleInfoRequest request) {
        try {
            // Mock implementation for demo
            return createMockVehicleInfoResponse(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get vehicle info", e);
        }
    }

    @Cacheable(value = "accessoryTypes", key = "'all'")
    public AccessoryTypesResponse getAllAccessoryTypes() {
        try {
            String url = baseUrl + "/service/accessories/getAllTypes";
            
            // For demo - return mock data
            return createMockAccessoryTypesResponse();
            
            // Production code would be:
            // ResponseEntity<AccessoryTypesResponse> response = restTemplate.getForEntity(url, AccessoryTypesResponse.class);
            // return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get accessory types", e);
        }
    }

    @Cacheable(value = "accessorySubTypes", key = "'all'")
    public AccessorySubTypesResponse getAllAccessorySubTypes() {
        try {
            String url = baseUrl + "/service/accessories/getAllSubTypes";
            
            // For demo - return mock data
            return createMockAccessorySubTypesResponse();
            
            // Production code would be:
            // ResponseEntity<AccessorySubTypesResponse> response = restTemplate.getForEntity(url, AccessorySubTypesResponse.class);
            // return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get accessory subtypes", e);
        }
    }

    @Cacheable(value = "accessoriesByModel", key = "#request.toString()")
    public MobisAccessoriesResponse getAccessoriesByModel(MobisAccessoriesRequest request) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://api.hyundaimobisin.com/service/accessories/getByModelIdYear")
                    .queryParam("modelId", request.modelId())
                    .queryParam("year", request.year())
                    .build()
                    .toUriString();
            
            // Make actual API call to Hyundai Mobis
            ResponseEntity<MobisAccessoriesResponse.Accessory[]> response = restTemplate.getForEntity(url, MobisAccessoriesResponse.Accessory[].class);
            
            if (response.getBody() != null) {
                List<MobisAccessoriesResponse.Accessory> accessories = Arrays.asList(response.getBody());
                return new MobisAccessoriesResponse(accessories, accessories.size(), true, "Success");
            } else {
                return new MobisAccessoriesResponse(new ArrayList<>(), 0, false, "No accessories found");
            }
            
        } catch (Exception e) {
            // Fallback to mock data if API fails
            return createMockAccessoriesByModelResponse(request);
        }
    }

    @Cacheable(value = "states", key = "'all'")
    public StatesResponse getStates() {
        try {
            String url = baseUrl + "/service/accessories/getStates";
            
            // For demo - return mock data
            return createMockStatesResponse();
            
            // Production code would be:
            // ResponseEntity<StatesResponse> response = restTemplate.getForEntity(url, StatesResponse.class);
            // return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get states", e);
        }
    }

    @Cacheable(value = "distributorStates", key = "'all'")
    public StatesResponse getDistributorStates() {
        try {
            String url = baseUrl + "/service/accessories/getDistributorStates";
            
            // For demo - return mock data
            return createMockDistributorStatesResponse();
            
            // Production code would be:
            // ResponseEntity<StatesResponse> response = restTemplate.getForEntity(url, StatesResponse.class);
            // return response.getBody();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get distributor states", e);
        }
    }

    // Mock data creation methods (for demo purposes)
    
    private PartSearchResponse createMockPartSearchResponse(PartSearchRequest request) {
        // Mock implementation - replace with actual API call in production
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
        
        // Determine the primary location for dealer data
        String primaryLocation = "";
        if (!city.isEmpty()) {
            primaryLocation = city;
        } else if (!state.isEmpty()) {
            primaryLocation = state;
        } else if (!location.isEmpty()) {
            primaryLocation = location;
        } else {
            primaryLocation = "Mumbai"; // Default fallback
        }
        
        List<DealerSearchResponse.Dealer> mockDealers = new ArrayList<>();
        
        // Generate location-specific dealer data
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
        } else if (primaryLocation.toLowerCase().contains("bangalore") || primaryLocation.toLowerCase().contains("bengaluru")) {
            mockDealers.addAll(Arrays.asList(
                new DealerSearchResponse.Dealer(
                    "DEAL_KA001",
                    "Bangalore Hyundai Service Center",
                    "123 MG Road, Brigade Road",
                    "Bangalore",
                    "Karnataka",
                    "560001",
                    "+91-80-234-5678",
                    "service@bangalorehyundai.com",
                    "www.bangalorehyundai.com",
                    12.9716,
                    77.5946,
                    1.8,
                    Arrays.asList("Parts Sales", "Service", "Warranty"),
                    "Mon-Sat: 9AM-7PM, Sun: 10AM-5PM",
                    true
                ),
                new DealerSearchResponse.Dealer(
                    "DEAL_KA002",
                    "Electronic City Hyundai",
                    "456 Hosur Road",
                    "Bangalore",
                    "Karnataka",
                    "560100",
                    "+91-80-345-6789",
                    "info@electroniccityhyundai.com",
                    "www.electroniccityhyundai.com",
                    12.8458,
                    77.6658,
                    8.3,
                    Arrays.asList("Parts Sales", "Service", "Body Shop"),
                    "Mon-Sat: 8AM-8PM, Sun: Closed",
                    true
                )
            ));
        } else if (primaryLocation.toLowerCase().contains("chennai")) {
            mockDealers.addAll(Arrays.asList(
                new DealerSearchResponse.Dealer(
                    "DEAL_TN001",
                    "Chennai Hyundai Service Center",
                    "123 Anna Salai",
                    "Chennai",
                    "Tamil Nadu",
                    "600002",
                    "+91-44-234-5678",
                    "service@chennaihyundai.com",
                    "www.chennaihyundai.com",
                    13.0827,
                    80.2707,
                    2.5,
                    Arrays.asList("Parts Sales", "Service", "Warranty"),
                    "Mon-Sat: 9AM-7PM, Sun: 10AM-5PM",
                    true
                ),
                new DealerSearchResponse.Dealer(
                    "DEAL_TN002",
                    "T Nagar Hyundai Center",
                    "456 Usman Road",
                    "Chennai",
                    "Tamil Nadu",
                    "600017",
                    "+91-44-345-6789",
                    "info@tnagarhyundai.com",
                    "www.tnagarhyundai.com",
                    13.0604,
                    80.2495,
                    6.1,
                    Arrays.asList("Parts Sales", "Service", "Body Shop"),
                    "Mon-Sat: 8AM-8PM, Sun: Closed",
                    true
                )
            ));
        } else {
            // Default Mumbai dealers for other locations
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
        // Mock warranty check
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
        // Mock implementation
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

    // New mock data creation methods for Mobis APIs
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
        
        // Mock data based on model
        if ("29".equals(request.modelId()) || "new-i20".equals(request.modelId())) {
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
                    "Highlight the persona of your car Tail lamp with Superb elegance in every detail. This Tail Lamp Garnish in Chrome adds premiumness to your car's exterior.",
                    1347L,
                    "Exteriors",
                    1436L,
                    "Garnish",
                    new BigDecimal("1202.0"),
                    "tail-lamp-garnish-1163",
                    "tail-lamp-garnish",
                    "TAIL LAMP GARNISH",
                    "3373-TAIL-LAMP-GARNISH-BVF24IH000-.jpg"
                ),
                new MobisAccessoriesResponse.Accessory(
                    1174L,
                    "WINDOW BEADING - CHROME",
                    "BVS22AP001",
                    "Gives premium look to the exterior of your car with the high grade ABS chrome finished window beading.",
                    1347L,
                    "Exteriors",
                    1436L,
                    "Garnish",
                    new BigDecimal("2138.0"),
                    "window-beading-chrome-1174",
                    "window-beading-chrome",
                    "WINDOW BEADING - CHROME",
                    "3369-WINDOW-BEADING---CHROME--BVS22AP001.jpg"
                ),
                new MobisAccessoriesResponse.Accessory(
                    1175L,
                    "DOOR HANDLE CHROME",
                    "BVS48AP000",
                    "High grade , chrome finish, ABS molded door handle covers to protect and enhance the looks of your Creta's Door Handles.",
                    1347L,
                    "Exteriors",
                    1436L,
                    "Garnish",
                    new BigDecimal("1379.0"),
                    "door-handle-chrome-1175",
                    "door-handle-chrome",
                    "DOOR HANDLE CHROME",
                    "3370-DOOR-HANDLE-(CHROME)-BVS48AP000.jpg"
                )
            ));
        } else if ("30".equals(request.modelId()) || "hyundai-new-creta".equals(request.modelId())) {
            accessories.addAll(Arrays.asList(
                new MobisAccessoriesResponse.Accessory(
                    1156L,
                    "DOOR CLADDING",
                    "BVF42IH003",
                    "High grade ABS molded Side Cladding specially curated for the dynamic looks of your Creta. Available in Black colour.",
                    1347L,
                    "Exteriors",
                    1437L,
                    "Body Styling & Protection",
                    new BigDecimal("9859.0"),
                    "door-cladding-1156",
                    "door-cladding",
                    "DOOR CLADDING",
                    "3363-DOOR-CLADDING-BVF42IH003.jpg"
                ),
                new MobisAccessoriesResponse.Accessory(
                    1148L,
                    "SIDE STEP-BLACK",
                    "BVF37IH001",
                    "High quatlity ABS material with premium feel and comfort while stepping in and out from your car.",
                    1347L,
                    "Exteriors",
                    1437L,
                    "Body Styling & Protection",
                    new BigDecimal("18589.0"),
                    "side-step-black-1148",
                    "side-step-black",
                    "SIDE STEP-BLACK",
                    "3352-SIDE-STEP-BLACk--BVF37IH001.jpg"
                ),
                new MobisAccessoriesResponse.Accessory(
                    1149L,
                    "SKID EXTENDER FRONT",
                    "BVF27IH001",
                    "Announce your arrival with aesthetically appealing, unique design and classy red insert Skid Extender Front.",
                    1347L,
                    "Exteriors",
                    1437L,
                    "Body Styling & Protection",
                    new BigDecimal("1659.0"),
                    "skid-extender-front-1149",
                    "skid-extender-front",
                    "SKID EXTENDER FRONT",
                    "3354-SKID-EXTENDER-FRONT--BVF27IH001.jpg"
                ),
                new MobisAccessoriesResponse.Accessory(
                    1150L,
                    "SKID EXTENDER REAR",
                    "BVF27IH002",
                    "Make heads turn with this ABS moulded sporty design red insert Skid Extender Rear.",
                    1347L,
                    "Exteriors",
                    1437L,
                    "Body Styling & Protection",
                    new BigDecimal("8999.0"),
                    "skid-extender-rear-1150",
                    "skid-extender-rear",
                    "SKID EXTENDER REAR",
                    "3355-SKID-EXTENDER-REAR--BVF27IH002.jpg"
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