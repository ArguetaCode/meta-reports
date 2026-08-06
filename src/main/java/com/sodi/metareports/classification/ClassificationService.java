package com.sodi.metareports.classification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ClassificationService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public ClassificationResult classify(UUID adId, UUID executionId, String pageId, String instagramId,
                                         String adAccountId, String campaignName) {
        UUID pageOwner = singleOwner("""
                select cfp.client_id from client_facebook_page cfp
                join facebook_page fp on fp.id=cfp.facebook_page_id
                where fp.meta_page_id=? and cfp.active_until is null and fp.active
                """, pageId);
        UUID instagramOwner = singleOwner("""
                select cia.client_id from client_instagram_account cia
                join instagram_account ia on ia.id=cia.instagram_account_id
                where ia.meta_instagram_account_id=? and cia.active_until is null and ia.active
                """, instagramId);
        UUID accountOwner = singleOwner("""
                select caa.client_id from client_ad_account caa
                join meta_ad_account maa on maa.id=caa.ad_account_id
                where maa.meta_ad_account_id=? and caa.active_until is null and maa.active
                  and (maa.exclusive_client_account or caa.exclusive)
                """, adAccountId);
        String prefix = extractPrefix(campaignName);
        UUID prefixOwner = singleOwner("""
                select client_id from campaign_prefix
                where normalized_prefix=? and active and valid_until is null
                """, prefix);
        UUID manualOwner = jdbc.query("select client_id from manual_ad_assignment where ad_id=?",
                (rs, row) -> rs.getObject(1, UUID.class), adId).stream().findFirst().orElse(null);

        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("facebookPageId", pageId);
        signals.put("facebookOwner", pageOwner);
        signals.put("instagramAccountId", instagramId);
        signals.put("instagramOwner", instagramOwner);
        signals.put("adAccountId", adAccountId);
        signals.put("exclusiveAccountOwner", accountOwner);
        signals.put("campaignPrefix", prefix);
        signals.put("prefixOwner", prefixOwner);
        signals.put("manualOwner", manualOwner);

        Set<UUID> strongOwners = new LinkedHashSet<>();
        if (pageOwner != null) strongOwners.add(pageOwner);
        if (instagramOwner != null) strongOwners.add(instagramOwner);
        if (accountOwner != null) strongOwners.add(accountOwner);

        UUID selected = manualOwner != null ? manualOwner : pageOwner != null ? pageOwner : instagramOwner != null ? instagramOwner : accountOwner;
        String method = manualOwner != null ? "MANUAL" : pageOwner != null ? "FACEBOOK_PAGE"
                : instagramOwner != null ? "INSTAGRAM_ACCOUNT"
                : accountOwner != null ? "EXCLUSIVE_AD_ACCOUNT" : "UNCLASSIFIED";
        String confidence = manualOwner != null || pageOwner != null || instagramOwner != null ? "HIGH"
                : accountOwner != null ? "MEDIUM" : "NONE";
        String incidentType = null;
        String description = null;

        if (manualOwner != null) {
            incidentType = null;
        } else if (strongOwners.size() > 1 || selected != null && prefixOwner != null && !selected.equals(prefixOwner)) {
            incidentType = "CONFLICTING_SIGNALS";
            description = "Las señales del anuncio apuntan a clientes diferentes.";
        } else if (selected == null && prefixOwner != null) {
            incidentType = "PREFIX_ONLY";
            description = "Solo el prefijo sugiere un cliente; requiere revisión manual.";
        } else if (selected == null && prefix != null) {
            incidentType = "UNKNOWN_PREFIX";
            description = "El anuncio no tiene un activo reconocido y su prefijo es desconocido.";
        } else if (selected == null) {
            incidentType = "UNCLASSIFIED";
            description = "El anuncio no contiene señales suficientes para clasificarlo.";
        }

        String signalsJson = json(signals);
        jdbc.update("""
                insert into ad_classification(ad_id,client_id,method,confidence,signals,sync_execution_id)
                values(?,?,?,?,?::jsonb,?) on conflict(ad_id) do update set
                client_id=excluded.client_id,method=excluded.method,confidence=excluded.confidence,
                signals=excluded.signals,classified_at=current_timestamp,sync_execution_id=excluded.sync_execution_id
                """, adId, selected, method, confidence, signalsJson, executionId);
        if (incidentType != null) {
            jdbc.update("""
                    insert into classification_incident(ad_id,sync_execution_id,incident_type,description,signals)
                    values(?,?,?,?,?::jsonb) on conflict(ad_id,sync_execution_id,incident_type) do nothing
                    """, adId, executionId, incidentType, description, signalsJson);
        }
        return new ClassificationResult(selected, method, confidence, incidentType);
    }

    public ClassificationResult reclassifyExisting(UUID adId, UUID executionId) {
        return jdbc.queryForObject("""
                select a.facebook_page_meta_id,a.instagram_account_meta_id,maa.meta_ad_account_id,c.campaign_name
                from meta_ad a join meta_ad_set ads on ads.id=a.ad_set_id
                join meta_campaign c on c.id=ads.campaign_id
                join meta_ad_account maa on maa.id=c.ad_account_id where a.id=?
                """, (rs, row) -> classify(adId, executionId, rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4)), adId);
    }

    private UUID singleOwner(String sql, String signal) {
        if (signal == null || signal.isBlank()) return null;
        List<UUID> owners = jdbc.query(sql, (rs, row) -> rs.getObject(1, UUID.class), signal);
        return owners.size() == 1 ? owners.getFirst() : null;
    }

    private String extractPrefix(String campaignName) {
        if (campaignName == null || campaignName.isBlank()) return null;
        String candidate = campaignName.trim().split("[\\s:|]+", 2)[0].toUpperCase();
        return candidate.matches("[A-Z0-9][A-Z0-9_-]{1,29}") ? candidate : null;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar las señales de clasificación.", exception);
        }
    }

    public record ClassificationResult(UUID clientId, String method, String confidence, String incidentType) {
    }
}
