package com.lufax.dashboard.controller;

import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.ManualOverrideRequest;
import com.lufax.dashboard.model.response.ManualOverrideResponse;
import com.lufax.dashboard.service.InsightService;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CardController}.
 * Covers all 4 endpoints: POST /insight, GET /config, GET /config/all, POST /override
 */
public class CardControllerTest {

    @Mock
    private InsightService insightService;

    @Mock
    private ResourceUtils resourceUtils;

    @Mock
    private JsonUtils jsonUtils;

    @InjectMocks
    private CardController cardController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // ========== POST /api/card/insight ==========

    @Test
    public void testCardInsight_Success() {
        // Arrange
        CardInsightRequest request = TestDataFactory.createCardRequest("profit_analysis");
        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("scenario", "base");
        expectedResult.put("score", 72.5);
        when(insightService.generateCardInsight(any(CardInsightRequest.class))).thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("base", response.getBody().get("scenario"));
        verify(insightService).generateCardInsight(request);
    }

    @Test
    public void testCardInsight_DifferentCardId() {
        // Arrange
        CardInsightRequest request = TestDataFactory.createCardRequest("revenue_trend");
        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("card_insight", "Revenue up 15%");
        when(insightService.generateCardInsight(any(CardInsightRequest.class))).thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Revenue up 15%", response.getBody().get("card_insight"));
        verify(insightService).generateCardInsight(argThat(req -> "revenue_trend".equals(req.getCardId())));
    }

    @Test
    public void testCardInsight_ReturnsServiceResultDirectly() {
        // Arrange - verify the controller is a thin passthrough layer
        CardInsightRequest request = TestDataFactory.createCardRequest();
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("card_id", "profit_analysis");
        serviceResult.put("summary", "Profit margin improving");
        serviceResult.put("details", "Gross margin increased by 3pp");
        serviceResult.put("recommendation", "Continue current strategy");
        when(insightService.generateCardInsight(any(CardInsightRequest.class))).thenReturn(serviceResult);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4, response.getBody().size());
        assertEquals("profit_analysis", response.getBody().get("card_id"));
        assertEquals("Profit margin improving", response.getBody().get("summary"));
    }

    // ========== GET /api/card/config ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testCardConfig_Found() {
        // Arrange
        String cardId = "profit_analysis";
        String jsonContent = "{\"profit_analysis\":{\"title\":\"Profit Analysis\",\"type\":\"chart\"},\"revenue_trend\":{\"title\":\"Revenue\"}}";
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> cardConfig = new LinkedHashMap<>();
        cardConfig.put("title", "Profit Analysis");
        cardConfig.put("type", "chart");
        configMap.put(cardId, cardConfig);

        when(resourceUtils.readResource("data/card_config.json")).thenReturn(jsonContent);
        when(jsonUtils.fromJson(eq(jsonContent), eq(Map.class))).thenReturn(configMap);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardConfig(cardId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(cardId, response.getBody().get("card_id"));
        assertNotNull(response.getBody().get("config"));
        Map<String, Object> returnedConfig = (Map<String, Object>) response.getBody().get("config");
        assertEquals("Profit Analysis", returnedConfig.get("title"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCardConfig_NotFound_CardIdNotInMap() {
        // Arrange
        String cardId = "nonexistent_card";
        String jsonContent = "{\"profit_analysis\":{}}";
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("profit_analysis", new LinkedHashMap<>());

        when(resourceUtils.readResource("data/card_config.json")).thenReturn(jsonContent);
        when(jsonUtils.fromJson(eq(jsonContent), eq(Map.class))).thenReturn(configMap);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardConfig(cardId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Card config not found", response.getBody().get("error"));
    }

    @Test
    public void testCardConfig_EmptyJsonContent() {
        // Arrange
        when(resourceUtils.readResource("data/card_config.json")).thenReturn("");

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardConfig("any_card");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Card config not found", response.getBody().get("error"));
    }

    @Test
    public void testCardConfig_NullContent() {
        // Arrange
        when(resourceUtils.readResource("data/card_config.json")).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardConfig("any_card");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Card config not found", response.getBody().get("error"));
    }

    @Test
    public void testCardConfig_JsonParseException() {
        // Arrange
        String badJson = "{invalid json}";
        when(resourceUtils.readResource("data/card_config.json")).thenReturn(badJson);
        when(jsonUtils.fromJson(eq(badJson), eq(Map.class))).thenThrow(new RuntimeException("JSON parse error"));

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.cardConfig("any_card");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Card config not found", response.getBody().get("error"));
    }

    // ========== GET /api/card/config/all ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testAllCardConfigs_Success() {
        // Arrange
        String jsonContent = "{\"card1\":{},\"card2\":{}}";
        Map<String, Object> fullMap = new LinkedHashMap<>();
        fullMap.put("card1", new LinkedHashMap<>());
        fullMap.put("card2", new LinkedHashMap<>());

        when(resourceUtils.readResource("data/card_config.json")).thenReturn(jsonContent);
        when(jsonUtils.fromJson(eq(jsonContent), eq(Map.class))).thenReturn(fullMap);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.allCardConfigs();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().containsKey("card1"));
        assertTrue(response.getBody().containsKey("card2"));
    }

    @Test
    public void testAllCardConfigs_NullContent_ReturnsEmptyMap() {
        // Arrange
        when(resourceUtils.readResource("data/card_config.json")).thenReturn(null);

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.allCardConfigs();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testAllCardConfigs_EmptyString_ReturnsEmptyMap() {
        // Arrange
        when(resourceUtils.readResource("data/card_config.json")).thenReturn("");

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.allCardConfigs();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testAllCardConfigs_ParseException_ReturnsEmptyMap() {
        // Arrange
        String badJson = "not json";
        when(resourceUtils.readResource("data/card_config.json")).thenReturn(badJson);
        when(jsonUtils.fromJson(eq(badJson), eq(Map.class))).thenThrow(new RuntimeException("bad"));

        // Act
        ResponseEntity<Map<String, Object>> response = cardController.allCardConfigs();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ========== POST /api/card/override ==========

    @Test
    public void testManualOverride_Success() {
        // Arrange
        ManualOverrideRequest request = new ManualOverrideRequest();
        request.setCardId("profit_analysis");
        request.setOverrideValue("custom_value");
        request.setReason("Manual adjustment needed");

        // Act
        ResponseEntity<ManualOverrideResponse> response = cardController.manualOverride(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ManualOverrideResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Override recorded", body.getMessage());
        assertEquals("profit_analysis", body.getCardId());
        assertNotNull(body.getOverrideId());
        assertTrue(body.getOverrideId() > 0);
    }

    @Test
    public void testManualOverride_GeneratesUniqueIds() {
        // Arrange
        ManualOverrideRequest request = new ManualOverrideRequest();
        request.setCardId("test_card");

        // Act - call twice rapidly to check uniqueness
        ResponseEntity<ManualOverrideResponse> resp1 = cardController.manualOverride(request);
        ResponseEntity<ManualOverrideResponse> resp2 = cardController.manualOverride(request);

        // Assert - IDs should be different (timestamp-based)
        assertNotEquals(resp1.getBody().getOverrideId(), resp2.getBody().getOverrideId());
    }

    @Test
    public void testManualOverride_PreservesCardIdFromRequest() {
        // Arrange
        ManualOverrideRequest request = new ManualOverrideRequest();
        request.setCardId("special_card_123");

        // Act
        ResponseEntity<ManualOverrideResponse> response = cardController.manualOverride(request);

        // Assert
        assertEquals("special_card_123", response.getBody().getCardId());
    }
}
