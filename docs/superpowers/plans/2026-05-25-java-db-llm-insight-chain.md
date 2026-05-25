# Java Database-Backed LLM Insight Chain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Java/PostgreSQL insight flow that seeds labelled demo facts into business tables, reads facts from those tables for LLM analysis, renders AI-generated card lights/tags, and supplies AI-backed bear/base/bull calculator scenarios.

**Architecture:** Add normalized business-fact tables and a startup seed importer, then introduce a repository-backed `FactPackService` as the sole standard-path provider of LLM facts. Retain `InsightService` as generation/persistence orchestration, expose front-end compatible `/api/insights/*` routes, and bind both HTML pages to returned AI output and explicit `DEMO_SEED` provenance.

**Tech Stack:** Java 8, Spring Boot 1.5.9, MyBatis, PostgreSQL/H2 tests, Jackson, JSON Schema validation, JUnit 4/Mockito, HTML/vanilla JavaScript.

---

## File Map

**Database facts and import**

- Create `src/main/java/com/lufax/dashboard/model/entity/MetricSnapshot.java`: snapshot provenance and active-period metadata.
- Create `src/main/java/com/lufax/dashboard/model/entity/BusinessMetric.java`: stored metric facts and trace-only imported status.
- Create `src/main/java/com/lufax/dashboard/model/entity/CardDefinition.java`: database-backed card metadata and bindings.
- Create `src/main/java/com/lufax/dashboard/model/entity/ScenarioBaseline.java`: persisted default calculator inputs.
- Create `src/main/java/com/lufax/dashboard/repository/BusinessFactMapper.java`: MyBatis queries/inserts for the new business tables.
- Create `src/main/resources/mapper/BusinessFactMapper.xml`: SQL mappings.
- Create `src/main/java/com/lufax/dashboard/service/DemoSeedImportService.java`: transactional idempotent import from classpath demo JSON.
- Modify `src/main/resources/sql/init_postgres.sql` and `src/test/resources/schema-test.sql`: business tables and extended result fields.
- Modify `src/main/resources/application.properties`: enable schema initialization and expose default source settings without hiding demo provenance.

**Fact packs and LLM results**

- Create `src/main/java/com/lufax/dashboard/service/FactPackService.java`: query-backed card/dashboard/scenario fact packs.
- Modify `src/main/java/com/lufax/dashboard/model/entity/InsightResult.java`: persisted AI contract fields.
- Modify `src/main/java/com/lufax/dashboard/repository/InsightResultMapper.java` and `src/main/resources/mapper/InsightResultMapper.xml`: cache/persistence for raw packs and structured AI outputs.
- Modify `src/main/java/com/lufax/dashboard/service/InsightService.java`: generate from DB packs, cache results, validate AI-owned lights/tags.
- Modify `src/main/java/com/lufax/dashboard/llm/MockLlmAdapter.java`: schema-shaped deterministic AI responses for local/demo operation.
- Modify `src/main/resources/prompts/*.txt` and `src/main/resources/schemas/*.json`: card/dashboard/scenario output contracts.

**API and UI**

- Create `src/main/java/com/lufax/dashboard/controller/InsightController.java`: `/api/insights/*` compatibility surface.
- Modify `src/main/java/com/lufax/dashboard/controller/DashboardController.java`: database-backed `/api/dashboard/data`.
- Modify `src/main/java/com/lufax/dashboard/service/InsightJobService.java`, `src/main/resources/mapper/InsightJobMapper.xml`, and `src/main/resources/mapper/InsightJobItemMapper.xml`: generate dashboard/cards/default scenarios and persist their lifecycle.
- Modify `lufax_dashboard3.html`: render card light/tag/narrative from AI API output and show demo source.
- Modify `ai_insights.html`: hydrate three calculator scenarios and dashboard insight from Java API output and show demo source.

**Tests**

- Create `src/test/java/com/lufax/dashboard/repository/BusinessFactMapperTest.java`.
- Create `src/test/java/com/lufax/dashboard/service/DemoSeedImportServiceTest.java`.
- Create `src/test/java/com/lufax/dashboard/service/FactPackServiceTest.java`.
- Create `src/test/java/com/lufax/dashboard/controller/InsightControllerTest.java`.
- Modify `src/test/java/com/lufax/dashboard/service/InsightServiceTest.java`, `InsightJobServiceTest.java`, and `DashboardControllerTest.java`.

### Task 1: Add Normalized Business Fact Persistence

**Files:**
- Create: `src/main/java/com/lufax/dashboard/model/entity/MetricSnapshot.java`
- Create: `src/main/java/com/lufax/dashboard/model/entity/BusinessMetric.java`
- Create: `src/main/java/com/lufax/dashboard/model/entity/CardDefinition.java`
- Create: `src/main/java/com/lufax/dashboard/model/entity/ScenarioBaseline.java`
- Create: `src/main/java/com/lufax/dashboard/repository/BusinessFactMapper.java`
- Create: `src/main/resources/mapper/BusinessFactMapper.xml`
- Modify: `src/main/resources/sql/init_postgres.sql`
- Modify: `src/test/resources/schema-test.sql`
- Test: `src/test/java/com/lufax/dashboard/repository/BusinessFactMapperTest.java`

- [ ] **Step 1: Write failing repository tests for snapshot, metrics, card bindings, and three baseline scenarios**

Create integration tests using the existing `@SpringBootTest`, `@ActiveProfiles("test")`, and `@Transactional` pattern:

```java
@Test
public void selectActiveSnapshotReturnsNewestImportedSnapshot() {
    mapper.insertSnapshot(snapshot("2026-03-YTD", "v1", "2026-05-20 10:00:00"));
    mapper.insertSnapshot(snapshot("2026-03-YTD", "v2", "2026-05-21 10:00:00"));

    MetricSnapshot active = mapper.selectActiveSnapshot("2026-03-YTD");

    assertEquals("v2", active.getMetricVersion());
    assertEquals("DEMO_SEED", active.getSourceType());
}

@Test
public void selectCardMetricsReturnsMappedFactsAndSourceStatus() {
    insertRiskCardAndMetric();
    List<BusinessMetric> facts =
        mapper.selectMetricsForCard("2026-03-YTD", "v1", "risk_analysis");
    assertEquals("credit_loss_rate", facts.get(0).getMetricCode());
    assertEquals("red", facts.get(0).getSourceStatus());
    assertEquals(Double.valueOf(8.0), facts.get(0).getRedline());
}

@Test
public void selectScenarioBaselinesReturnsBearBaseBull() {
    mapper.insertScenarioBaseline(scenario("bear"));
    mapper.insertScenarioBaseline(scenario("base"));
    mapper.insertScenarioBaseline(scenario("bull"));
    assertEquals(3, mapper.selectScenarioBaselines("2026-03-YTD", "v1").size());
}
```

- [ ] **Step 2: Run repository tests and observe failure because business mapper/tables do not exist**

Run:

```powershell
mvn -q -Dtest=BusinessFactMapperTest test
```

Expected: test compilation fails for missing `BusinessFactMapper`/entities, or SQL fails because the new tables are not yet created.

- [ ] **Step 3: Add entities, mapper interface, and SQL tables/mappings**

Add tables in both PostgreSQL and H2 schemas:

```sql
CREATE TABLE IF NOT EXISTS metric_snapshot (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_label VARCHAR(100) NOT NULL,
    source_file VARCHAR(200),
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (period, metric_version)
);

CREATE TABLE IF NOT EXISTS business_metric (
    id BIGSERIAL PRIMARY KEY,
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    metric_scope VARCHAR(20) NOT NULL,
    metric_code VARCHAR(80) NOT NULL,
    metric_name VARCHAR(120),
    module VARCHAR(40),
    dimension_key VARCHAR(120) NOT NULL DEFAULT '',
    segment VARCHAR(120),
    value DOUBLE PRECISION,
    display_value VARCHAR(80),
    unit VARCHAR(30),
    yoy DOUBLE PRECISION,
    display_yoy VARCHAR(80),
    mom DOUBLE PRECISION,
    budget DOUBLE PRECISION,
    budget_gap DOUBLE PRECISION,
    display_budget_gap VARCHAR(80),
    redline DOUBLE PRECISION,
    yellowline DOUBLE PRECISION,
    distance_to_redline DOUBLE PRECISION,
    display_distance_to_redline VARCHAR(80),
    direction VARCHAR(40),
    source_status VARCHAR(20),
    product_type VARCHAR(80),
    channel_type VARCHAR(80),
    customer_type VARCHAR(80),
    vintage VARCHAR(40),
    mob VARCHAR(40),
    UNIQUE (period, metric_version, metric_scope, metric_code, dimension_key)
);

CREATE TABLE IF NOT EXISTS card_definition (
    card_id VARCHAR(80) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    module VARCHAR(40),
    prompt_version VARCHAR(40) DEFAULT 'v2'
);

CREATE TABLE IF NOT EXISTS card_metric_binding (
    card_id VARCHAR(80) NOT NULL,
    metric_code VARCHAR(80) NOT NULL,
    PRIMARY KEY (card_id, metric_code)
);

CREATE TABLE IF NOT EXISTS scenario_baseline (
    period VARCHAR(40) NOT NULL,
    metric_version VARCHAR(80) NOT NULL,
    scenario VARCHAR(20) NOT NULL,
    scenario_label VARCHAR(80),
    inputs_json TEXT NOT NULL,
    PRIMARY KEY (period, metric_version, scenario)
);
```

Define mapper methods with explicit responsibilities:

```java
int insertSnapshot(MetricSnapshot snapshot);
MetricSnapshot selectActiveSnapshot(@Param("period") String period);
int insertMetric(BusinessMetric metric);
List<BusinessMetric> selectMetrics(@Param("period") String period, @Param("metricVersion") String metricVersion);
List<BusinessMetric> selectMetricsForCard(@Param("period") String period, @Param("metricVersion") String metricVersion, @Param("cardId") String cardId);
int insertCard(CardDefinition card);
int insertCardMetric(@Param("cardId") String cardId, @Param("metricCode") String metricCode);
List<CardDefinition> selectCards();
int insertScenarioBaseline(ScenarioBaseline baseline);
List<ScenarioBaseline> selectScenarioBaselines(@Param("period") String period, @Param("metricVersion") String metricVersion);
ScenarioBaseline selectScenarioBaseline(@Param("period") String period, @Param("metricVersion") String metricVersion, @Param("scenario") String scenario);
```

- [ ] **Step 4: Run repository tests to verify fact persistence/query passes**

Run:

```powershell
mvn -q -Dtest=BusinessFactMapperTest test
```

Expected: `Tests run: ... Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit the persistence increment**

```powershell
git add src/main/java/com/lufax/dashboard/model/entity src/main/java/com/lufax/dashboard/repository/BusinessFactMapper.java src/main/resources/mapper/BusinessFactMapper.xml src/main/resources/sql/init_postgres.sql src/test/resources/schema-test.sql src/test/java/com/lufax/dashboard/repository/BusinessFactMapperTest.java
git commit -m "feat: add business fact persistence model"
```

### Task 2: Import Labelled Demo Facts Into Tables

**Files:**
- Create: `src/main/java/com/lufax/dashboard/service/DemoSeedImportService.java`
- Modify: `src/main/java/com/lufax/dashboard/DashboardApplication.java`
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/com/lufax/dashboard/service/DemoSeedImportServiceTest.java`

- [ ] **Step 1: Write failing importer tests for idempotency and source provenance**

```java
@Test
public void importsJsonAsDemoSeedWithThreeScenarios() {
    service.importIfMissing();
    MetricSnapshot snapshot = mapper.selectActiveSnapshot("2026-03-YTD");
    assertEquals("DEMO_SEED", snapshot.getSourceType());
    assertEquals("演示数据，待替换", snapshot.getSourceLabel());
    assertFalse(mapper.selectMetrics(snapshot.getPeriod(), snapshot.getMetricVersion()).isEmpty());
    assertEquals(3, mapper.selectScenarioBaselines(snapshot.getPeriod(), snapshot.getMetricVersion()).size());
}

@Test
public void repeatedImportDoesNotDuplicateSnapshotOrMetrics() {
    service.importIfMissing();
    service.importIfMissing();
    assertEquals(1, mapper.countSnapshots("2026-03-YTD", "mock_v2"));
}
```

- [ ] **Step 2: Run importer tests and observe missing service failure**

Run:

```powershell
mvn -q -Dtest=DemoSeedImportServiceTest test
```

Expected: FAIL because `DemoSeedImportService` and count/query support are missing.

- [ ] **Step 3: Implement transactional startup seed import**

Implement a service that parses the two resource files and writes database rows once:

```java
@Service
public class DemoSeedImportService {
    static final String SOURCE_TYPE = "DEMO_SEED";
    static final String SOURCE_LABEL = "演示数据，待替换";

    @Transactional
    public void importIfMissing() {
        Map<String, Object> dashboard =
            jsonUtils.fromJson(resourceUtils.readResource("data/dashboard_metrics_mock.json"), Map.class);
        String period = String.valueOf(dashboard.get("period"));
        String version = String.valueOf(dashboard.get("metric_version"));
        if (businessFactMapper.selectSnapshot(period, version) != null) {
            return;
        }
        businessFactMapper.insertSnapshot(toSnapshot(period, version));
        importMetrics(period, version, "CORE", list(dashboard.get("core_metrics")));
        importMetrics(period, version, "SEGMENT", list(dashboard.get("segment_metrics")));
        importCards();
        importScenarios(period, version, list(dashboard.get("scenario_defaults")));
    }
}
```

Invoke it after application startup through `CommandLineRunner`:

```java
@Bean
public CommandLineRunner seedDemoData(DemoSeedImportService seedService) {
    return args -> seedService.importIfMissing();
}
```

Configure schema initialization:

```properties
spring.datasource.initialize=true
spring.datasource.schema=classpath:sql/init_postgres.sql
app.data-source-type=DEMO_SEED
app.data-source-label=演示数据，待替换
```

- [ ] **Step 4: Run importer and repository tests**

Run:

```powershell
mvn -q -Dtest=BusinessFactMapperTest,DemoSeedImportServiceTest test
```

Expected: both classes pass; the repeated-import assertion confirms idempotency.

- [ ] **Step 5: Commit seed import**

```powershell
git add src/main/java/com/lufax/dashboard/service/DemoSeedImportService.java src/main/java/com/lufax/dashboard/DashboardApplication.java src/main/resources/application.properties src/test/java/com/lufax/dashboard/service/DemoSeedImportServiceTest.java src/main/java/com/lufax/dashboard/repository/BusinessFactMapper.java src/main/resources/mapper/BusinessFactMapper.xml
git commit -m "feat: import labelled demo metric snapshot"
```

### Task 3: Build Database-Backed Fact Packs Without Preassigned Card Lights

**Files:**
- Create: `src/main/java/com/lufax/dashboard/service/FactPackService.java`
- Modify: `src/main/java/com/lufax/dashboard/model/response/DashboardDataResponse.java`
- Modify: `src/main/java/com/lufax/dashboard/controller/DashboardController.java`
- Test: `src/test/java/com/lufax/dashboard/service/FactPackServiceTest.java`
- Test: `src/test/java/com/lufax/dashboard/controller/DashboardControllerTest.java`

- [ ] **Step 1: Write failing tests for database-origin facts and exclusion of source traffic light**

```java
@Test
public void buildCardFactPackUsesStoredThresholdFactsButOmitsImportedStatus() {
    Map<String, Object> pack = service.buildCardFactPack("2026-03-YTD", "risk_analysis");
    List<Map<String, Object>> metrics = (List<Map<String, Object>>) pack.get("metrics");
    assertEquals(Double.valueOf(8.0), metrics.get(0).get("redline"));
    assertEquals("higher_is_worse", metrics.get(0).get("direction"));
    assertFalse(metrics.get(0).containsKey("status"));
    assertFalse(metrics.get(0).containsKey("source_status"));
    assertEquals("DEMO_SEED", ((Map<?, ?>) pack.get("data_source")).get("type"));
}

@Test
public void dashboardDataReturnsDatabaseSnapshotWithDemoLabel() {
    DashboardDataResponse data = factPackService.getDashboardData("2026-03-YTD");
    assertEquals("DEMO_SEED", data.getDataSourceType());
    assertEquals("演示数据，待替换", data.getDataSourceLabel());
}
```

- [ ] **Step 2: Run tests and verify they fail against request/resource-based builder**

Run:

```powershell
mvn -q -Dtest=FactPackServiceTest,DashboardControllerTest test
```

Expected: FAIL because DB-backed pack/data methods and source fields are absent.

- [ ] **Step 3: Implement fact pack methods over `BusinessFactMapper`**

Create narrow methods:

```java
public MetricSnapshot requireSnapshot(String period) {
    MetricSnapshot snapshot = mapper.selectActiveSnapshot(period);
    if (snapshot == null) {
        throw new IllegalStateException("No metric snapshot for period: " + period);
    }
    return snapshot;
}

public Map<String, Object> buildCardFactPack(String period, String cardId) {
    MetricSnapshot snapshot = requireSnapshot(period);
    List<BusinessMetric> rows =
        mapper.selectMetricsForCard(snapshot.getPeriod(), snapshot.getMetricVersion(), cardId);
    Map<String, Object> pack = basePack("CARD_FACT_PACK", snapshot);
    pack.put("card", cardMap(mapper.selectCard(cardId)));
    pack.put("metrics", rows.stream().map(this::toLlmMetricFact).collect(Collectors.toList()));
    return pack;
}

private Map<String, Object> toLlmMetricFact(BusinessMetric metric) {
    Map<String, Object> fact = new LinkedHashMap<>();
    fact.put("metric_code", metric.getMetricCode());
    fact.put("metric_name", metric.getMetricName());
    fact.put("value", metric.getValue());
    fact.put("unit", metric.getUnit());
    fact.put("yoy", metric.getYoy());
    fact.put("budget_gap", metric.getBudgetGap());
    fact.put("redline", metric.getRedline());
    fact.put("yellowline", metric.getYellowline());
    fact.put("direction", metric.getDirection());
    return fact;
}
```

Also implement `buildDashboardFactPack(period)`, `buildScenarioFactPack(period, scenario, overrides)`, and database-backed dashboard data retrieval. Do not call `ResourceUtils` in request-time dashboard/card/scenario methods.

- [ ] **Step 4: Run fact pack/controller tests to green**

Run:

```powershell
mvn -q -Dtest=FactPackServiceTest,DashboardControllerTest test
```

Expected: PASS, including assertion that imported status is not sent as AI conclusion.

- [ ] **Step 5: Commit DB fact-pack increment**

```powershell
git add src/main/java/com/lufax/dashboard/service/FactPackService.java src/main/java/com/lufax/dashboard/model/response/DashboardDataResponse.java src/main/java/com/lufax/dashboard/controller/DashboardController.java src/test/java/com/lufax/dashboard/service/FactPackServiceTest.java src/test/java/com/lufax/dashboard/controller/DashboardControllerTest.java
git commit -m "feat: build llm fact packs from database facts"
```

### Task 4: Make Card Lights And Labels LLM-Owned, Persisted Results

**Files:**
- Modify: `src/main/resources/prompts/card_insight_v2.txt`
- Modify: `src/main/resources/prompts/dashboard_insight_v2.txt`
- Modify: `src/main/resources/prompts/scenario_insight_v2.txt`
- Modify: `src/main/resources/schemas/card_insight.schema.json`
- Modify: `src/main/resources/schemas/dashboard_insight.schema.json`
- Modify: `src/main/resources/schemas/scenario_insight.schema.json`
- Modify: `src/main/java/com/lufax/dashboard/model/entity/InsightResult.java`
- Modify: `src/main/java/com/lufax/dashboard/repository/InsightResultMapper.java`
- Modify: `src/main/resources/mapper/InsightResultMapper.xml`
- Modify: `src/main/java/com/lufax/dashboard/service/InsightService.java`
- Modify: `src/main/java/com/lufax/dashboard/llm/MockLlmAdapter.java`
- Test: `src/test/java/com/lufax/dashboard/service/InsightServiceTest.java`
- Test: `src/test/java/com/lufax/dashboard/repository/InsightResultMapperTest.java`

- [ ] **Step 1: Write failing tests establishing the AI output contract**

```java
@Test
public void generateCardPersistsAiTrafficLightLabelsAndRawFactPack() {
    when(factPackService.buildCardFactPack("2026-03-YTD", "risk_analysis"))
        .thenReturn(cardFactPackWithoutStatus());
    when(llmAdapter.generate(anyString(), anyMap())).thenReturn(
        "{\"card_id\":\"risk_analysis\",\"traffic_light\":\"red\"," +
        "\"analysis_labels\":[\"损失率触红线\"],\"standard_insight\":\"风险承压\"," +
        "\"drivers\":[],\"watch_items\":[],\"evidence\":[\"credit_loss_rate=8.2\"]}");

    Map<String, Object> result = insightService.generateCardInsight("2026-03-YTD", "risk_analysis", false);

    assertEquals("red", result.get("traffic_light"));
    assertEquals("损失率触红线", ((List<?>) result.get("analysis_labels")).get(0));
    ArgumentCaptor<InsightResult> stored = ArgumentCaptor.forClass(InsightResult.class);
    verify(insightResultMapper).insertOrUpdate(stored.capture());
    assertTrue(stored.getValue().getRawFactPackJson().contains("credit_loss_rate"));
}

@Test
public void invalidCardAiOutputIsStoredFailedWithoutLocalLightFallback() {
    when(llmAdapter.generate(anyString(), anyMap())).thenReturn("{\"standard_insight\":\"missing light\"}");
    Map<String, Object> result = insightService.generateCardInsight("2026-03-YTD", "risk_analysis", true);
    assertEquals("failed", result.get("status"));
    assertFalse(result.containsKey("traffic_light"));
}
```

- [ ] **Step 2: Run service/result mapper tests and observe failures**

Run:

```powershell
mvn -q -Dtest=InsightServiceTest,InsightResultMapperTest test
```

Expected: FAIL for missing AI fields, fact-pack service injection, and persistence columns.

- [ ] **Step 3: Extend schemas and prompts with mandatory LLM-owned outputs**

Card schema core:

```json
{
  "type": "object",
  "required": ["card_id", "traffic_light", "analysis_labels", "standard_insight", "drivers", "watch_items", "evidence"],
  "properties": {
    "card_id": {"type": "string"},
    "traffic_light": {"type": "string", "enum": ["red", "yellow", "green"]},
    "analysis_labels": {"type": "array", "items": {"type": "string"}},
    "standard_insight": {"type": "string"},
    "drivers": {"type": "array"},
    "watch_items": {"type": "array"},
    "evidence": {"type": "array"}
  }
}
```

Add explicit prompt rule:

```text
You determine traffic_light and analysis_labels. FACT_PACK contains facts and
thresholds only. Do not assume a precomputed lamp status. Every label and
traffic-light decision must be supported by evidence from FACT_PACK.
```

- [ ] **Step 4: Implement DB-pack generation and extended result storage**

Change `InsightService` standard generation methods to accept `period/cardId/scenario` and call `FactPackService`, not request `input_data`:

```java
public Map<String, Object> generateCardInsight(String period, String cardId, boolean forceRefresh) {
    Map<String, Object> factPack = factPackService.buildCardFactPack(period, cardId);
    MetricSnapshot snapshot = factPackService.requireSnapshot(period);
    InsightResult cached = findReadyResult(snapshot, "card", cardId, forceRefresh);
    if (cached != null) return hydrateResult(cached);
    try {
        Map<String, Object> output =
            validateAndParse(llmAdapter.generate(loadPrompt("card_insight_v2.txt"), factPack),
                "card_insight.schema.json");
        saveReadyResult(snapshot, "card", cardId, factPack, output);
        return withLifecycle("ready", output, snapshot);
    } catch (Exception ex) {
        saveFailedResult(snapshot, "card", cardId, factPack, ex);
        return failedResponse(snapshot, ex);
    }
}
```

Persist new fields using `insertOrUpdate`; add columns in both production/test schemas. Make `MockLlmAdapter.generate(prompt, params)` inspect `pack_type` and return schema-valid card/dashboard/scenario JSON so default `llm.adapter=mock` provides a runnable demonstration.

- [ ] **Step 5: Run service, mapper, and validation tests**

Run:

```powershell
mvn -q -Dtest=InsightServiceTest,InsightResultMapperTest,ValidationUtilsTest,MockLlmAdapterTest test
```

Expected: PASS; invalid output does not generate a guessed light.

- [ ] **Step 6: Commit LLM contract and result persistence**

```powershell
git add src/main/resources/prompts src/main/resources/schemas src/main/resources/sql/init_postgres.sql src/test/resources/schema-test.sql src/main/java/com/lufax/dashboard/model/entity/InsightResult.java src/main/java/com/lufax/dashboard/repository/InsightResultMapper.java src/main/resources/mapper/InsightResultMapper.xml src/main/java/com/lufax/dashboard/service/InsightService.java src/main/java/com/lufax/dashboard/llm/MockLlmAdapter.java src/test/java/com/lufax/dashboard/service/InsightServiceTest.java src/test/java/com/lufax/dashboard/repository/InsightResultMapperTest.java src/test/java/com/lufax/dashboard/llm/MockLlmAdapterTest.java
git commit -m "feat: generate and persist ai card traffic lights"
```

### Task 5: Expose UI-Compatible Insight APIs And Generate Default Scenarios

**Files:**
- Create: `src/main/java/com/lufax/dashboard/controller/InsightController.java`
- Modify: `src/main/java/com/lufax/dashboard/service/InsightJobService.java`
- Modify: `src/main/java/com/lufax/dashboard/model/request/CreateJobRequest.java`
- Modify: `src/main/java/com/lufax/dashboard/model/request/ScenarioInsightRequest.java`
- Modify: `src/main/java/com/lufax/dashboard/model/response/PageStateResponse.java`
- Modify: `src/main/java/com/lufax/dashboard/model/dto/CardInsightDto.java`
- Modify: `src/main/java/com/lufax/dashboard/controller/JobController.java`
- Modify: `src/main/resources/mapper/InsightJobMapper.xml`
- Modify: `src/main/resources/mapper/InsightJobItemMapper.xml`
- Test: `src/test/java/com/lufax/dashboard/controller/InsightControllerTest.java`
- Test: `src/test/java/com/lufax/dashboard/service/InsightJobServiceTest.java`

- [ ] **Step 1: Write failing API tests matching current page fetches**

```java
@Test
public void pageStateReturnsAiCardOutputAndDemoSource() {
    mockMvc.perform(get("/api/insights/page-state").param("period", "2026-03-YTD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data_source.type").value("DEMO_SEED"))
        .andExpect(jsonPath("$.cards[0].traffic_light").value("red"))
        .andExpect(jsonPath("$.cards[0].analysis_labels[0]").value("损失率触红线"));
}

@Test
public void allScopeJobGeneratesDashboardCardsAndDefaultScenarios() {
    jobService.processScope("2026-03-YTD", "all", Collections.emptyList(), true);
    verify(insightService).generateScenarioInsight("2026-03-YTD", "bear", null, true);
    verify(insightService).generateScenarioInsight("2026-03-YTD", "base", null, true);
    verify(insightService).generateScenarioInsight("2026-03-YTD", "bull", null, true);
}

@Test
public void scenarioEndpointReturnsCachedDefaultScenarioFromStoredFacts() throws Exception {
    mockMvc.perform(post("/api/insights/scenario")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"period\":\"2026-03-YTD\",\"scenario_id\":\"base\",\"cache_only\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scenario").value("base"))
        .andExpect(jsonPath("$.data_source.type").value("DEMO_SEED"));
}
```

- [ ] **Step 2: Run API/job tests and verify missing endpoints/behavior fail**

Run:

```powershell
mvn -q -Dtest=InsightControllerTest,InsightJobServiceTest test
```

Expected: FAIL due to absent `/api/insights/*` controller and scenario orchestration.

- [ ] **Step 3: Implement the compatibility controller**

Add routes aligned to the pages:

```java
@RestController
@RequestMapping("/api/insights")
public class InsightController {
    @GetMapping("/page-state")
    public PageStateResponse pageState(@RequestParam String period) { ... }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam String period) { ... }

    @PostMapping("/jobs")
    public Map<String, Object> jobs(@RequestBody CreateJobRequest request) { ... }

    @GetMapping("/jobs/{jobId}")
    public Map<String, Object> job(@PathVariable String jobId) { ... }

    @PostMapping("/scenario")
    public Map<String, Object> scenario(@RequestBody ScenarioInsightRequest request) { ... }
}
```

Responses include:

```json
{
  "data_source": {"type": "DEMO_SEED", "label": "演示数据，待替换"},
  "status": "ready"
}
```

- [ ] **Step 4: Implement scope-based generation and default scenario warming**

Update `InsightJobService` so `scope=all` schedules dashboard, each card, and each default scenario; `scope=card` generates requested card IDs; `scope=dashboard` generates dashboard plus default calculator scenarios. Preserve lifecycle state as `pending/running/ready/failed`, not business light values.

Fix Java 8 incompatibility in touched controllers by replacing `Map.of(...)` with `LinkedHashMap` builders:

```java
private Map<String, Object> statusResponse(boolean success, String message) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", success);
    result.put("message", message);
    return result;
}
```

- [ ] **Step 5: Run API/job tests to green**

Run:

```powershell
mvn -q -Dtest=InsightControllerTest,InsightJobServiceTest,DashboardControllerTest,CardControllerTest,ScenarioControllerTest,JobControllerTest test
```

Expected: PASS for the UI-facing contract and legacy delegation paths.

- [ ] **Step 6: Commit API and scenario orchestration**

```powershell
git add src/main/java/com/lufax/dashboard/controller src/main/java/com/lufax/dashboard/service/InsightJobService.java src/main/java/com/lufax/dashboard/model/request src/main/java/com/lufax/dashboard/model/response src/main/java/com/lufax/dashboard/model/dto/CardInsightDto.java src/main/resources/mapper/InsightJobMapper.xml src/main/resources/mapper/InsightJobItemMapper.xml src/test/java/com/lufax/dashboard/controller src/test/java/com/lufax/dashboard/service/InsightJobServiceTest.java
git commit -m "feat: expose insight APIs and default scenario jobs"
```

### Task 6: Bind Card Lights, Labels, Calculator Scenarios, And Demo Provenance In UI

**Files:**
- Modify: `lufax_dashboard3.html`
- Modify: `ai_insights.html`

- [ ] **Step 1: Capture failing browser assertions against the current static UI**

After starting the Java service, use Browser/Playwright assertions that initially fail:

```javascript
await page.goto('http://127.0.0.1:8000/lufax_dashboard3.html');
await page.waitForResponse(r => r.url().includes('/api/insights/page-state'));
await expect(page.locator('[data-card-id="risk_analysis"]').locator('xpath=ancestor::*[contains(@class,"section")]')).toContainText('损失率触红线');
await expect(page.getByText('演示数据，待替换')).toBeVisible();

await page.goto('http://127.0.0.1:8000/ai_insights.html');
await expect(page.locator('#pane-predict')).toContainText('演示数据，待替换');
await expect(page.locator('#ai-scenario-source')).toContainText('AI');
```

Expected before edits: missing source label and/or hardcoded card/scenario state means assertions fail.

- [ ] **Step 2: Render LLM card light and tags in `lufax_dashboard3.html`**

Extend the existing `renderCardInsightFromCache` and `renderCardInsight` flow:

```javascript
function applyAiTrafficLight(cardRoot, light) {
  var color = ['red','yellow','green'].indexOf(light) >= 0 ? light : 'yellow';
  cardRoot.querySelectorAll('.lamp-dot').forEach(function(dot) {
    dot.classList.remove('red','yellow','green');
    dot.classList.add(color);
  });
}

function renderCardInsightFromCache(el, c) {
  var root = el.closest('.section');
  applyAiTrafficLight(root, c.traffic_light);
  renderAnalysisLabels(root, c.analysis_labels || []);
  // retain existing standard_insight/drivers/watch_items rendering
}
```

Add a compact data-source indicator populated from `data.data_source.label`; do not leave static card lamp/tags authoritative once API state has loaded.

- [ ] **Step 3: Hydrate calculator defaults and AI scenario outputs in `ai_insights.html`**

Replace static initial scenario authority after API load:

```javascript
async function loadDefaultScenarios() {
  var keys = ['bear', 'base', 'bull'];
  var results = await Promise.all(keys.map(function(key) {
    return fetch('/api/insights/scenario', {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({period: CURRENT_PERIOD, scenario_id: key, cache_only: true})
    }).then(function(r) { return r.json(); });
  }));
  results.forEach(function(result) {
    if (result.status === 'ready') {
      SCENARIOS[result.scenario] = scenarioToSliders(result.inputs, result.scenario_label);
    }
  });
  loadScenario(CURRENT_SCENARIO);
}
```

Display `data_source.label`; continue using `POST /api/insights/scenario` for slider-customized analysis.

- [ ] **Step 4: Run browser verification against locally served pages**

Run Java:

```powershell
mvn spring-boot:run
```

Open:

```text
http://127.0.0.1:8000/lufax_dashboard3.html
http://127.0.0.1:8000/ai_insights.html
```

Expected:

- Card lights and tags change according to API LLM output.
- No card is labelled from static hardcoded tags after API data is ready.
- Bear/base/bull scenario controls hydrate from backend scenario result.
- Both pages visibly identify `演示数据，待替换`.

- [ ] **Step 5: Commit UI binding**

```powershell
git add lufax_dashboard3.html ai_insights.html
git commit -m "feat: display ai lights labels and scenarios"
```

### Task 7: Full Verification And Documentation Closeout

**Files:**
- No planned source edits; correct only an implementation defect exposed by the verification commands, using the owning task's file and test.

- [ ] **Step 1: Run the full Java test suite**

Run:

```powershell
mvn test
```

Expected: build success with zero failing tests.

- [ ] **Step 2: Verify PostgreSQL/demo startup path and API payloads**

With PostgreSQL configured and the Java app running, query:

```powershell
Invoke-RestMethod 'http://127.0.0.1:8000/api/dashboard/data?period=2026-03-YTD'
Invoke-RestMethod 'http://127.0.0.1:8000/api/insights/page-state?period=2026-03-YTD'
Invoke-RestMethod 'http://127.0.0.1:8000/api/insights/dashboard?period=2026-03-YTD'
```

Expected:

- Payloads state `DEMO_SEED` and `演示数据，待替换`.
- Card results carry `traffic_light` and `analysis_labels` returned by LLM/mock-LLM.
- Result cache contains `raw_fact_pack_json` and `raw_llm_output_json`.

- [ ] **Step 3: Perform browser rendering verification**

Use the Browser plugin first for both local pages. Check:

- Network requests resolve without console errors.
- Source label is visible.
- Card lights/tags come from response data.
- Calculator initial three scenarios load; editing a slider can request a custom explanation.

Capture screenshots for desktop and mobile widths when available.

- [ ] **Step 4: Inspect changed files and commit any verification-only corrections**

Run:

```powershell
git status --short
git diff --stat
```

Do not stage unrelated pre-existing workspace changes. If an implementation correction was required after verification:

```powershell
git add <only-files-corrected-for-this-feature>
git commit -m "fix: complete insight flow verification"
```
