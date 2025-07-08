package com.hyundai.mobis.functions;

import com.hyundai.mobis.dto.DealerSearchRequest;
import com.hyundai.mobis.dto.DealerSearchResponse;
import com.hyundai.mobis.dto.PartSearchRequest;
import com.hyundai.mobis.dto.PartSearchResponse;
import com.hyundai.mobis.dto.MobisAccessoriesRequest;
import com.hyundai.mobis.dto.MobisAccessoriesResponse;
import com.hyundai.mobis.dto.AccessoryTypesResponse;
import com.hyundai.mobis.dto.AccessorySubTypesResponse;
import com.hyundai.mobis.dto.StatesResponse;
import com.hyundai.mobis.service.MobisApiService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class MobisApiFunctions {

    private final MobisApiService mobisApiService;

    public MobisApiFunctions(MobisApiService mobisApiService) {
        this.mobisApiService = mobisApiService;
    }

    public PartSearchResponse searchPartsFunction(PartSearchRequest request) {
        return mobisApiService.searchParts(request);
    }

    public DealerSearchResponse findDealersFunction(DealerSearchRequest request) {
        return mobisApiService.findDealers(request);
    }

    public DealerSearchResponse findDistributorsFunction(DealerSearchRequest request) {
        return mobisApiService.findDistributors(request);
    }

    public WarrantyCheckResponse checkWarrantyFunction(WarrantyCheckRequest request) {
        return mobisApiService.checkWarranty(request);
    }

    public OffersResponse getOffersFunction(OffersRequest request) {
        return mobisApiService.getCurrentOffers(request);
    }

    public VehicleInfoResponse getVehicleInfoFunction(VehicleInfoRequest request) {
        return mobisApiService.getVehicleInfo(request);
    }

    public AccessoryTypesResponse getAllAccessoryTypesFunction() {
        return mobisApiService.getAllAccessoryTypes();
    }

    public AccessorySubTypesResponse getAllAccessorySubTypesFunction() {
        return mobisApiService.getAllAccessorySubTypes();
    }

    public MobisAccessoriesResponse getAccessoriesByModelFunction(MobisAccessoriesRequest request) {
        return mobisApiService.getAccessoriesByModel(request);
    }

    public StatesResponse getStatesFunction() {
        return mobisApiService.getStates();
    }

    public StatesResponse getDistributorStatesFunction() {
        return mobisApiService.getDistributorStates();
    }

    // Request/Response classes
    public record WarrantyCheckRequest(String partNumber, String purchaseDate) {}
    
    public record WarrantyCheckResponse(
        boolean isUnderWarranty,
        String status,
        String expiryDate,
        String warrantyType
    ) {}

    public record OffersRequest(
        String category,
        String region,
        String dealerCode
    ) {}

    public record OffersResponse(List<Offer> offers, int totalOffers) {
        public record Offer(
            String title,
            String description,
            String discount,
            String validUntil,
            List<String> applicableProducts,
            String terms
        ) {}
    }

    public record VehicleInfoRequest(
        String vin,
        String model,
        String year,
        String trim
    ) {}

    public record VehicleInfoResponse(
        String vin,
        String model,
        String year,
        String trim,
        String engine,
        String transmission,
        String fuelType,
        List<String> features,
        String warrantyStatus,
        String warrantyExpiry
    ) {}
} 