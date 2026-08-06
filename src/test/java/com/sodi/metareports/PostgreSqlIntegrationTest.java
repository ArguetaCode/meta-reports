package com.sodi.metareports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.sodi.metareports.synchronization.SynchronizationService;
import com.sodi.metareports.incident.IncidentService;
import com.sodi.metareports.metric.MetricsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostgreSqlIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("meta_reports_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired SynchronizationService synchronizationService;
    @Autowired IncidentService incidentService;
    @Autowired MetricsService metricsService;

    @Test
    void flywayCreatesPhaseTwoSchemaOnPostgreSql() {
        String description = jdbcTemplate.queryForObject(
                "select description from app_schema_marker where id = 1", String.class);
        assertThat(description).contains("Connectivity marker");
        Integer roles = jdbcTemplate.queryForObject("select count(*) from role", Integer.class);
        Integer permissions = jdbcTemplate.queryForObject("select count(*) from permission", Integer.class);
        String jsonType = jdbcTemplate.queryForObject(
                "select data_type from information_schema.columns where table_name='audit_log' and column_name='new_data'",
                String.class);
        assertThat(roles).isEqualTo(4);
        assertThat(permissions).isEqualTo(20);
        assertThat(jsonType).isEqualTo("jsonb");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public' and table_name='sync_execution'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public' and table_name='incident_resolution_history'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void fixtureSynchronizationIsPagedClassifiedAndIdempotent() {
        UUID clientId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID sharedAccountId = UUID.randomUUID();
        UUID pageId = UUID.randomUUID();
        UUID instagramId = UUID.randomUUID();
        jdbcTemplate.update("insert into client(id,code,commercial_name,status,primary_currency,timezone) values(?,'REV','Cliente Revisión','PILOT','GTQ','America/Guatemala')", clientId);
        jdbcTemplate.update("insert into meta_ad_account(id,meta_ad_account_id,account_name,account_currency,timezone_name) values(?,'930000000000002','Cuenta Fixture','GTQ','America/Guatemala')", accountId);
        jdbcTemplate.update("insert into client_ad_account(client_id,ad_account_id,exclusive) values(?,?,false)", clientId, accountId);
        jdbcTemplate.update("insert into meta_ad_account(id,meta_ad_account_id,account_name,account_currency,timezone_name) values(?,'930000000000004','Cuenta Fixture Compartida','GTQ','America/Guatemala')", sharedAccountId);
        jdbcTemplate.update("insert into client_ad_account(client_id,ad_account_id,exclusive) values(?,?,false)", clientId, sharedAccountId);
        jdbcTemplate.update("insert into facebook_page(id,meta_page_id,page_name,normalized_page_name) values(?,'910000000000001','Página Fixture','pagina fixture')", pageId);
        jdbcTemplate.update("insert into client_facebook_page(client_id,facebook_page_id) values(?,?)", clientId, pageId);
        jdbcTemplate.update("insert into instagram_account(id,meta_instagram_account_id,username,display_name) values(?,'920000000000001','fixture','Fixture')", instagramId);
        jdbcTemplate.update("insert into client_instagram_account(client_id,instagram_account_id) values(?,?)", clientId, instagramId);
        jdbcTemplate.update("insert into campaign_prefix(client_id,prefix,normalized_prefix) values(?,'REV-TEST','REV-TEST')", clientId);

        synchronizationService.runFixture("phase3-demo");
        synchronizationService.runFixture("phase3-demo");

        assertThat(jdbcTemplate.queryForObject("select count(*) from sync_execution where status='COMPLETED'", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from meta_campaign", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from meta_ad_set", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from meta_ad", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ad_classification", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from classification_incident", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select method from ad_classification ac join meta_ad a on a.id=ac.ad_id where a.meta_ad_id='960000000000001'", String.class)).isEqualTo("FACEBOOK_PAGE");
        assertThat(jdbcTemplate.queryForObject("select client_id from ad_classification ac join meta_ad a on a.id=ac.ad_id where a.meta_ad_id='960000000000001'", UUID.class)).isEqualTo(clientId);
        assertThat(jdbcTemplate.queryForObject("select pages_processed from sync_execution order by started_at desc limit 1", Integer.class)).isEqualTo(2);

        UUID prefixIncident = jdbcTemplate.queryForObject("""
                select id from classification_incident where incident_type='PREFIX_ONLY'
                order by occurred_at desc limit 1
                """, UUID.class);
        incidentService.resolve(prefixIncident, clientId, "Asignación confirmada durante revisión manual");
        assertThat(jdbcTemplate.queryForObject("select status from classification_incident where id=?", String.class, prefixIncident)).isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject("""
                select method from ad_classification ac join classification_incident ci on ci.ad_id=ac.ad_id where ci.id=?
                """, String.class, prefixIncident)).isEqualTo("MANUAL");
        assertThat(jdbcTemplate.queryForObject("select count(*) from manual_ad_assignment", Integer.class)).isEqualTo(1);

        UUID unknownIncident = jdbcTemplate.queryForObject("""
                select id from classification_incident where incident_type='UNKNOWN_PREFIX'
                order by occurred_at desc limit 1
                """, UUID.class);
        incidentService.ignore(unknownIncident, "No corresponde al alcance del cliente piloto");
        assertThat(jdbcTemplate.queryForObject("select status from classification_incident where id=?", String.class, unknownIncident)).isEqualTo("IGNORED");

        UUID olderPrefixIncident = jdbcTemplate.queryForObject("""
                select id from classification_incident where incident_type='PREFIX_ONLY' and status='OPEN'
                order by occurred_at limit 1
                """, UUID.class);
        incidentService.reprocess(olderPrefixIncident, "Reprocesamiento con asignación manual vigente");
        assertThat(jdbcTemplate.queryForObject("select status from classification_incident where id=?", String.class, olderPrefixIncident)).isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from incident_resolution_history", Integer.class)).isEqualTo(3);

        assertThat(metricsService.importFixture()).isEqualTo(4);
        assertThat(metricsService.importFixture()).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from daily_ad_insight",Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select count(*) from insight_action",Integer.class)).isEqualTo(6);
        metricsService.saveRate(LocalDate.of(2026,7,18),"usd","gtq",new BigDecimal("7.80"),"Referencia de prueba");
        UUID periodId=metricsService.createPeriod(clientId,LocalDate.of(2026,7,1),LocalDate.of(2026,7,31));
        assertThat(metricsService.review(periodId)).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("select exchange_rate_snapshot from report_period where id=?",BigDecimal.class,periodId))
                .isEqualByComparingTo("7.80000000");
    }
}
