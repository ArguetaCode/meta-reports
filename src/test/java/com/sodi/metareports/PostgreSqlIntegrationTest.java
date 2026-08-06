package com.sodi.metareports;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
    }
}
