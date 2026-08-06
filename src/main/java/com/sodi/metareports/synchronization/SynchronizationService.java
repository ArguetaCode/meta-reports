package com.sodi.metareports.synchronization;

import com.sodi.metareports.audit.AuditService;
import com.sodi.metareports.metaintegration.MetaDataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SynchronizationService {
    private final JdbcTemplate jdbc;
    private final MetaDataSource source;
    private final SyncPageProcessor processor;
    private final AuditService audit;

    public SynchronizationService(JdbcTemplate jdbc, MetaDataSource source, SyncPageProcessor processor, AuditService audit) {
        this.jdbc = jdbc; this.source = source; this.processor = processor; this.audit = audit;
    }

    public UUID runFixture(String fixtureName) {
        UUID executionId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID actor = currentActor();
        jdbc.update("""
                insert into sync_execution(id,source,fixture_name,status,correlation_id,requested_by)
                values(?,'FIXTURE',?,'RUNNING',?,?)
                """, executionId, fixtureName, correlationId, actor);
        int pages = 0, received = 0, upserted = 0, incidents = 0;
        String cursor = null;
        try {
            do {
                var page = source.fetchPage(fixtureName, cursor);
                var result = processor.process(executionId, page);
                pages++; received += result.received(); upserted += result.upserted(); incidents += result.incidents();
                cursor = page.nextCursor();
                jdbc.update("""
                        update sync_execution set current_cursor=?,pages_processed=?,records_received=?,
                        records_upserted=?,incidents_created=? where id=?
                        """, cursor, pages, received, upserted, incidents, executionId);
            } while (cursor != null);
            jdbc.update("update sync_execution set status='COMPLETED',completed_at=current_timestamp where id=?", executionId);
            audit.record("SYNC_EXECUTE", "SyncExecution", executionId.toString(), "Fixture synchronization completed", null,
                    Map.of("fixture", fixtureName, "records", received, "incidents", incidents));
            return executionId;
        } catch (RuntimeException exception) {
            jdbc.update("update sync_execution set status='FAILED',completed_at=current_timestamp,error_message=? where id=?",
                    safeMessage(exception), executionId);
            throw exception;
        }
    }

    public List<Map<String, Object>> executions() {
        return jdbc.queryForList("select * from sync_execution order by started_at desc limit 100");
    }

    private UUID currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        return jdbc.query("select id from app_user where username=?", (rs,row)->rs.getObject(1,UUID.class), authentication.getName())
                .stream().findFirst().orElse(null);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? "Error de sincronización" : message.substring(0, Math.min(1000, message.length()));
    }
}
