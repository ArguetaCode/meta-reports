package com.sodi.metareports.configuration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevDemoDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public DevDemoDataInitializer(JdbcTemplate jdbc, @Value("${app.demo-data.enabled:false}") boolean enabled) {
        this.jdbc = jdbc; this.enabled = enabled;
    }

    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || jdbc.queryForObject("select count(*) from client", Integer.class) > 0) return;
        UUID diagnostics = UUID.randomUUID(); UUID climate = UUID.randomUUID();
        jdbc.update("insert into client(id,code,commercial_name,status,primary_currency,timezone) values(?, 'DEM', 'Cliente Demo Diagnósticos', 'PILOT', 'GTQ', 'America/Guatemala')", diagnostics);
        jdbc.update("insert into client(id,code,commercial_name,status,primary_currency,timezone) values(?, 'CLI', 'Cliente Demo Climatización', 'PILOT', 'GTQ', 'America/Guatemala')", climate);
        UUID page = UUID.randomUUID(); UUID instagram = UUID.randomUUID(); UUID account = UUID.randomUUID(); UUID portfolio = UUID.randomUUID();
        jdbc.update("insert into facebook_page(id,meta_page_id,page_name,normalized_page_name) values(?, '100000000000001', 'Página Demo Diagnósticos', 'pagina demo diagnosticos')", page);
        jdbc.update("insert into client_facebook_page(client_id,facebook_page_id) values(?,?)", diagnostics, page);
        jdbc.update("insert into instagram_account(id,meta_instagram_account_id,username,display_name) values(?, '200000000000001', 'instagram_demo', 'Instagram Demo')", instagram);
        jdbc.update("insert into client_instagram_account(client_id,instagram_account_id) values(?,?)", diagnostics, instagram);
        jdbc.update("insert into meta_business_portfolio(id,meta_business_id,name) values(?, '300000000000001', 'Portafolio Demo SODI')", portfolio);
        jdbc.update("insert into meta_ad_account(id,meta_ad_account_id,account_name,account_currency,timezone_name,business_portfolio_id) values(?, '400000000000001', 'Cuenta publicitaria compartida Demo', 'GTQ', 'America/Guatemala', ?)", account, portfolio);
        jdbc.update("insert into client_ad_account(client_id,ad_account_id,exclusive) values(?,?,false),(?,?,false)", diagnostics, account, climate, account);
        jdbc.update("insert into campaign_prefix(client_id,prefix,normalized_prefix,primary_prefix) values(?, 'DEM', 'DEM', true),(?, 'CLI', 'CLI', true)", diagnostics, climate);
    }
}
