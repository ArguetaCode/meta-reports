package com.sodi.metareports.incident;

import com.sodi.metareports.audit.AuditService;
import com.sodi.metareports.classification.ClassificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private final JdbcTemplate jdbc;
    private final ClassificationService classification;
    private final AuditService audit;

    public IncidentService(JdbcTemplate jdbc, ClassificationService classification, AuditService audit) {
        this.jdbc = jdbc; this.classification = classification; this.audit = audit;
    }

    public List<Map<String, Object>> all() {
        return jdbc.queryForList("""
                select ci.id,ci.incident_type,ci.status,ci.description,ci.occurred_at,ci.resolution_note,
                       ci.resolved_at,a.meta_ad_id,a.ad_name,c.campaign_name,cl.commercial_name resolved_client
                from classification_incident ci join meta_ad a on a.id=ci.ad_id
                join meta_ad_set ads on ads.id=a.ad_set_id join meta_campaign c on c.id=ads.campaign_id
                left join client cl on cl.id=ci.resolved_client_id
                order by case when ci.status='OPEN' then 0 else 1 end,ci.occurred_at desc limit 200
                """);
    }

    public List<Map<String, Object>> activeClients() {
        return jdbc.queryForList("select id,code,commercial_name from client where active order by commercial_name");
    }

    @Transactional
    public void resolve(UUID incidentId, UUID clientId, String note) {
        String cleanNote = requiredNote(note);
        requireActiveClient(clientId);
        Incident incident = openIncident(incidentId);
        UUID actor = currentActor();
        jdbc.update("""
                insert into manual_ad_assignment(ad_id,client_id,note,assigned_by) values(?,?,?,?)
                on conflict(ad_id) do update set client_id=excluded.client_id,note=excluded.note,
                assigned_by=excluded.assigned_by,updated_at=current_timestamp
                """, incident.adId(), clientId, cleanNote, actor);
        jdbc.update("""
                update ad_classification set client_id=?,method='MANUAL',confidence='HIGH',classified_at=current_timestamp
                where ad_id=?
                """, clientId, incident.adId());
        int changed = jdbc.update("""
                update classification_incident set status='RESOLVED',resolved_client_id=?,resolution_note=?,
                resolved_by=?,resolved_at=current_timestamp where id=? and status='OPEN'
                """, clientId, cleanNote, actor, incidentId);
        ensureChanged(changed);
        history(incidentId, "RESOLVE", "OPEN", "RESOLVED", clientId, cleanNote, actor);
        audit.record("INCIDENT_RESOLVE", "ClassificationIncident", incidentId.toString(),
                "Classification incident resolved manually", Map.of("status", "OPEN"),
                Map.of("status", "RESOLVED", "clientId", clientId, "note", cleanNote));
    }

    @Transactional
    public void ignore(UUID incidentId, String note) {
        String cleanNote = requiredNote(note);
        Incident incident = openIncident(incidentId);
        UUID actor = currentActor();
        int changed = jdbc.update("""
                update classification_incident set status='IGNORED',resolution_note=?,resolved_by=?,
                resolved_at=current_timestamp where id=? and status='OPEN'
                """, cleanNote, actor, incidentId);
        ensureChanged(changed);
        history(incidentId, "IGNORE", "OPEN", "IGNORED", null, cleanNote, actor);
        audit.record("INCIDENT_IGNORE", "ClassificationIncident", incidentId.toString(),
                "Classification incident ignored", Map.of("status", "OPEN"), Map.of("status", "IGNORED", "note", cleanNote));
    }

    @Transactional
    public void reprocess(UUID incidentId, String note) {
        String cleanNote = requiredNote(note);
        Incident incident = openIncident(incidentId);
        var result = classification.reclassifyExisting(incident.adId(), incident.executionId());
        String resultingStatus = result.incidentType() == null ? "RESOLVED" : "OPEN";
        UUID actor = currentActor();
        if ("RESOLVED".equals(resultingStatus)) {
            jdbc.update("""
                    update classification_incident set status='RESOLVED',resolved_client_id=?,resolution_note=?,
                    resolved_by=?,resolved_at=current_timestamp where id=? and status='OPEN'
                    """, result.clientId(), cleanNote, actor, incidentId);
        }
        history(incidentId, "REPROCESS", "OPEN", resultingStatus, result.clientId(), cleanNote, actor);
        audit.record("INCIDENT_REPROCESS", "ClassificationIncident", incidentId.toString(),
                "Classification incident reprocessed", Map.of("status", "OPEN"),
                Map.of("status", resultingStatus, "method", result.method(), "note", cleanNote));
    }

    private Incident openIncident(UUID id) {
        return jdbc.query("select ad_id,sync_execution_id,status from classification_incident where id=?",
                (rs, row) -> new Incident(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3)), id)
                .stream().findFirst().filter(value -> "OPEN".equals(value.status()))
                .orElseThrow(() -> new IllegalArgumentException("La incidencia no existe o ya fue atendida."));
    }

    private void requireActiveClient(UUID id) {
        Integer count = jdbc.queryForObject("select count(*) from client where id=? and active", Integer.class, id);
        if (count == null || count != 1) throw new IllegalArgumentException("El cliente seleccionado no está activo.");
    }

    private String requiredNote(String note) {
        String value = note == null ? "" : note.trim();
        if (value.length() < 5 || value.length() > 1000) throw new IllegalArgumentException("La nota debe tener entre 5 y 1000 caracteres.");
        return value;
    }

    private void history(UUID incident, String action, String before, String after, UUID client, String note, UUID actor) {
        jdbc.update("""
                insert into incident_resolution_history(incident_id,action,previous_status,resulting_status,
                selected_client_id,note,performed_by) values(?,?,?,?,?,?,?)
                """, incident, action, before, after, client, note, actor);
    }

    private UUID currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        return jdbc.query("select id from app_user where username=?", (rs,row)->rs.getObject(1,UUID.class), authentication.getName())
                .stream().findFirst().orElse(null);
    }

    private void ensureChanged(int changed) {
        if (changed != 1) throw new IllegalStateException("La incidencia cambió mientras se procesaba.");
    }

    private record Incident(UUID adId, UUID executionId, String status) {}
}
