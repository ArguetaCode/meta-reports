package com.sodi.metareports.branding;

import com.sodi.metareports.audit.AuditService;
import com.sodi.metareports.filemanagement.LogoStorage;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BrandingService {
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private final JdbcTemplate jdbc;
    private final LogoStorage storage;
    private final AuditService audit;

    public BrandingService(JdbcTemplate jdbc, LogoStorage storage, AuditService audit) {
        this.jdbc = jdbc; this.storage = storage; this.audit = audit;
    }

    public Map<String, Object> find(UUID clientId) {
        var rows = jdbc.queryForList("select * from client_branding where client_id=?", clientId);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    @Transactional
    public void update(UUID clientId, String primary, String secondary, String accent,
                       String header, String footer, MultipartFile logo) {
        validateColor(primary); validateColor(secondary); validateColor(accent);
        String logoPath = logo == null || logo.isEmpty() ? null : storage.store(clientId, logo);
        jdbc.update("""
                insert into client_branding(client_id,primary_color,secondary_color,accent_color,logo_path,report_header_text,report_footer_text)
                values(?,?,?,?,?,?,?) on conflict(client_id) do update set
                primary_color=excluded.primary_color,secondary_color=excluded.secondary_color,
                accent_color=excluded.accent_color,logo_path=coalesce(excluded.logo_path,client_branding.logo_path),
                report_header_text=excluded.report_header_text,report_footer_text=excluded.report_footer_text,updated_at=current_timestamp
                """, clientId, blank(primary), blank(secondary), blank(accent), logoPath, header, footer);
        audit.record("BRANDING_UPDATE", "ClientBranding", clientId.toString(), "Client branding updated", null,
                Map.of("logoUpdated", logoPath != null));
    }

    private void validateColor(String value) {
        if (value != null && !value.isBlank() && !COLOR.matcher(value).matches()) {
            throw new IllegalArgumentException("Los colores deben usar el formato #RRGGBB.");
        }
    }
    private String blank(String value) { return value == null || value.isBlank() ? null : value.toUpperCase(); }
}
