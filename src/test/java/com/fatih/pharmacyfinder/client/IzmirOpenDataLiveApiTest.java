package com.fatih.pharmacyfinder.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("live")
class IzmirOpenDataLiveApiTest {

    @Autowired
    private IzmirOpenDataClient izmirOpenDataClient;

    @Test
    @DisplayName("GERÇEK API: İzmir nöbetçi eczaneler listesi alınabilmeli")
    void testFetchOnDutyPharmacies_RealRequest() {
        izmirOpenDataClient.fetchOnDutyPharmacies()
                .as(StepVerifier::create)
                .assertNext(jsonNode -> {
                    assertNotNull(jsonNode, "Yanıt null olmamalı");

                    // Izmir API usually returns an array of nodes
                    assertTrue(jsonNode.isArray(), "Yanıt bir liste olmalı");

                    if (jsonNode.size() > 0) {
                        JsonNode firstPharmacy = jsonNode.get(0);
                        // Fields in Izmir API are like Adi, Ilce, Adres, Telefon
                        assertTrue(firstPharmacy.has("Adi"), "Eczane adı (Adi) bulunmalı");
                        assertTrue(firstPharmacy.has("Bolge"), "İlçe bilgisi (Bolge) bulunmalı");

                    }
                })
                .verifyComplete();
    }
}
