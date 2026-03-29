package com.fatih.pharmacyfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatih.pharmacyfinder.client.CollectApiClient;
import com.fatih.pharmacyfinder.client.IzmirOpenDataClient;
import com.fatih.pharmacyfinder.client.TurkishGovClient;
import com.fatih.pharmacyfinder.model.Pharmacy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OnDutyPharmacyServiceTest {

    @Mock
    private TurkishGovClient turkishGovClient;
    @Mock
    private OnDutyPharmacyScraperService scraperService;
    @Mock
    private ValidationService validationService;
    @Mock
    private IzmirOpenDataClient izmirClient;
    @Mock
    private CollectApiClient collectApiClient;
    @Mock
    private LocationService locationService;

    private OnDutyPharmacyService onDutyPharmacyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        onDutyPharmacyService = new OnDutyPharmacyService(
                turkishGovClient, scraperService, validationService, izmirClient, collectApiClient, locationService);
        
        when(validationService.isValidCityOrDistrict(anyString())).thenReturn(true);
        when(validationService.sanitizeInput(anyString())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testGetOnDutyPharmacies_FallsBackToCollectApiWhenIzmirIsEmpty() {
        // Izmir returns empty list
        when(izmirClient.fetchOnDutyPharmacies()).thenReturn(Mono.just(objectMapper.createArrayNode()));
        
        // CollectAPI returns pharmacies
        String collectApiResponse = "{\"success\":true,\"result\":[{\"name\":\"COLLECT_PHARMACY\",\"loc\":\"38.1,27.3\"}]}";
        try {
            JsonNode collectNode = objectMapper.readTree(collectApiResponse);
            when(collectApiClient.fetchOnDutyPharmacies(anyString(), anyString())).thenReturn(Mono.just(collectNode));
        } catch (Exception e) {}

        Mono<List<Pharmacy>> result = onDutyPharmacyService.getOnDutyPharmacies("Torbalı", "İzmir");

        StepVerifier.create(result)
                .assertNext(list -> {
                    assertEquals(1, list.size());
                    assertEquals("COLLECT_PHARMACY", list.get(0).getDisplayName());
                })
                .verifyComplete();
        
        verify(izmirClient).fetchOnDutyPharmacies();
        verify(collectApiClient).fetchOnDutyPharmacies(anyString(), anyString());
    }
}
