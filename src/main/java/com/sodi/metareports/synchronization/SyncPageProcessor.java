package com.sodi.metareports.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodi.metareports.classification.ClassificationService;
import com.sodi.metareports.metaintegration.MetaAdRecord;
import com.sodi.metareports.metaintegration.MetaFixturePage;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncPageProcessor {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ClassificationService classification;

    public SyncPageProcessor(JdbcTemplate jdbc, ObjectMapper objectMapper, ClassificationService classification) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.classification = classification;
    }

    @Transactional
    public PageResult process(UUID executionId, MetaFixturePage page) {
        int incidents = 0;
        for (MetaAdRecord raw : page.records()) {
            MetaAdRecord record = normalize(raw);
            UUID accountId = requiredId("select id from meta_ad_account where meta_ad_account_id=? and active", record.adAccountId(), "cuenta publicitaria");
            UUID campaignId = upsertCampaign(accountId, record);
            UUID adSetId = upsertAdSet(campaignId, record);
            UUID adId = upsertAd(executionId, adSetId, record);
            var result = classification.classify(adId, executionId, record.facebookPageId(),
                    record.instagramAccountId(), record.adAccountId(), record.campaignName());
            if (result.incidentType() != null) incidents++;
        }
        return new PageResult(page.records().size(), page.records().size(), incidents);
    }

    private UUID upsertCampaign(UUID accountId, MetaAdRecord record) {
        return jdbc.queryForObject("""
                insert into meta_campaign(meta_campaign_id,ad_account_id,campaign_name,effective_status)
                values(?,?,?,?) on conflict(meta_campaign_id) do update set
                ad_account_id=excluded.ad_account_id,campaign_name=excluded.campaign_name,
                effective_status=excluded.effective_status,updated_at=current_timestamp,version=meta_campaign.version+1
                returning id
                """, UUID.class, record.campaignId(), accountId, record.campaignName(), record.campaignStatus());
    }

    private UUID upsertAdSet(UUID campaignId, MetaAdRecord record) {
        return jdbc.queryForObject("""
                insert into meta_ad_set(meta_ad_set_id,campaign_id,ad_set_name,effective_status)
                values(?,?,?,?) on conflict(meta_ad_set_id) do update set
                campaign_id=excluded.campaign_id,ad_set_name=excluded.ad_set_name,
                effective_status=excluded.effective_status,updated_at=current_timestamp,version=meta_ad_set.version+1
                returning id
                """, UUID.class, record.adSetId(), campaignId, record.adSetName(), record.adSetStatus());
    }

    private UUID upsertAd(UUID executionId, UUID adSetId, MetaAdRecord record) {
        return jdbc.queryForObject("""
                insert into meta_ad(meta_ad_id,ad_set_id,ad_name,effective_status,facebook_page_meta_id,
                                    instagram_account_meta_id,source_payload,last_sync_execution_id)
                values(?,?,?,?,?,?,?::jsonb,?) on conflict(meta_ad_id) do update set
                ad_set_id=excluded.ad_set_id,ad_name=excluded.ad_name,effective_status=excluded.effective_status,
                facebook_page_meta_id=excluded.facebook_page_meta_id,
                instagram_account_meta_id=excluded.instagram_account_meta_id,
                source_payload=excluded.source_payload,last_sync_execution_id=excluded.last_sync_execution_id,
                updated_at=current_timestamp,version=meta_ad.version+1 returning id
                """, UUID.class, record.adId(), adSetId, record.adName(), record.adStatus(),
                record.facebookPageId(), record.instagramAccountId(), json(record), executionId);
    }

    private MetaAdRecord normalize(MetaAdRecord value) {
        return new MetaAdRecord(metaId(value.adAccountId(), true), metaId(value.campaignId(), false),
                required(value.campaignName(), "nombre de campaña"), value.campaignStatus(),
                metaId(value.adSetId(), false), required(value.adSetName(), "nombre de conjunto"), value.adSetStatus(),
                metaId(value.adId(), false), required(value.adName(), "nombre de anuncio"), value.adStatus(),
                optionalMetaId(value.facebookPageId()), optionalMetaId(value.instagramAccountId()));
    }

    private String metaId(String value, boolean removeAct) {
        String normalized = required(value, "ID Meta").trim();
        if (removeAct) normalized = normalized.replaceFirst("(?i)^act_", "");
        if (!normalized.matches("[0-9]{3,80}")) throw new IllegalArgumentException("El fixture contiene un ID Meta inválido.");
        return normalized;
    }

    private String optionalMetaId(String value) { return value == null || value.isBlank() ? null : metaId(value, false); }
    private String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + field + " en el fixture."); return value.trim(); }
    private UUID requiredId(String sql, String id, String type) { return jdbc.query(sql, (rs,row)->rs.getObject(1,UUID.class),id).stream().findFirst().orElseThrow(()->new IllegalArgumentException("No existe " + type + " activa para el ID " + id + ".")); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException("No fue posible serializar el fixture.",e); } }

    public record PageResult(int received, int upserted, int incidents) { }
}
