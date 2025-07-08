package com.hyundai.mobis.config;

import com.hyundai.mobis.service.MobisApiService;
import com.hyundai.mobis.dto.MobisAccessoriesRequest;
import com.hyundai.mobis.dto.MobisAccessoriesResponse;
import com.hyundai.mobis.dto.AccessoryTypesResponse;
import com.hyundai.mobis.dto.AccessorySubTypesResponse;
import com.hyundai.mobis.dto.StatesResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class SpringAIConfig {

    @Bean
    public Supplier<AccessoryTypesResponse> getAllAccessoryTypesFunction(MobisApiService mobisApiService) {
        return mobisApiService::getAllAccessoryTypes;
    }

    @Bean
    public Supplier<AccessorySubTypesResponse> getAllAccessorySubTypesFunction(MobisApiService mobisApiService) {
        return mobisApiService::getAllAccessorySubTypes;
    }

    @Bean
    public Function<MobisAccessoriesRequest, MobisAccessoriesResponse> getAccessoriesByModelFunction(MobisApiService mobisApiService) {
        return mobisApiService::getAccessoriesByModel;
    }

    @Bean
    public Supplier<StatesResponse> getStatesFunction(MobisApiService mobisApiService) {
        return mobisApiService::getStates;
    }

    @Bean
    public Supplier<StatesResponse> getDistributorStatesFunction(MobisApiService mobisApiService) {
        return mobisApiService::getDistributorStates;
    }
} 