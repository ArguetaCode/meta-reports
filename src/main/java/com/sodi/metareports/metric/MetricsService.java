package com.sodi.metareports.metric;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodi.metareports.audit.AuditService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final AuditService audit;

    public MetricsService(JdbcTemplate jdbc, ObjectMapper json, AuditService audit) {
        this.jdbc=jdbc; this.json=json; this.audit=audit;
    }

    @Transactional
    public int importFixture() {
        JsonNode records;
        try (var input = new ClassPathResource("fixtures/phase5-metrics.json").getInputStream()) {
            records=json.readTree(input).path("records");
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible leer el fixture de métricas.", exception);
        }
        int count=0;
        for (JsonNode record : records) {
            UUID adId=jdbc.query("select id from meta_ad where meta_ad_id=?",(rs,row)->rs.getObject(1,UUID.class),required(record,"adId"))
                    .stream().findFirst().orElseThrow(()->new IllegalArgumentException("El fixture referencia un anuncio inexistente."));
            UUID insightId=UUID.randomUUID();
            String payload=record.toString();
            jdbc.update("""
                    insert into daily_ad_insight(id,ad_id,insight_date,source,attribution_window,currency,spend,impressions,reach,clicks,source_payload)
                    values(?,?,?,'META_FIXTURE',?,?,?,?,?,?,?::jsonb) on conflict(ad_id,insight_date,source,attribution_window)
                    do update set currency=excluded.currency,spend=excluded.spend,impressions=excluded.impressions,
                    reach=excluded.reach,clicks=excluded.clicks,source_payload=excluded.source_payload,imported_at=current_timestamp
                    """, insightId,adId,LocalDate.parse(required(record,"date")),required(record,"attributionWindow"),
                    required(record,"currency"),record.path("spend").decimalValue(),record.path("impressions").longValue(),
                    record.path("reach").longValue(),record.path("clicks").longValue(),payload);
            UUID storedId=jdbc.queryForObject("select id from daily_ad_insight where ad_id=? and insight_date=? and source='META_FIXTURE' and attribution_window=?",
                    UUID.class,adId,LocalDate.parse(required(record,"date")),required(record,"attributionWindow"));
            jdbc.update("delete from insight_action where insight_id=?",storedId);
            Iterator<Map.Entry<String,JsonNode>> actions=record.path("actions").fields();
            while(actions.hasNext()){var action=actions.next();jdbc.update("insert into insight_action(insight_id,action_type,value) values(?,?,?)",storedId,action.getKey(),action.getValue().decimalValue());}
            count++;
        }
        audit.record("METRIC_FIXTURE_IMPORT","DailyAdInsight",null,"Daily metric fixture imported",null,Map.of("records",count));
        return count;
    }

    @Transactional
    public void saveRate(LocalDate month,String source,String target,BigDecimal rate,String rateSource) {
        LocalDate canonical=month.withDayOfMonth(1); String from=currency(source),to=currency(target); String origin=rateSource==null?"":rateSource.trim();
        if(from.equals(to)||rate==null||rate.signum()<=0||origin.length()<3||origin.length()>120)throw new IllegalArgumentException("La tasa, monedas o fuente no son válidas.");
        jdbc.update("""
                insert into exchange_rate(rate_month,source_currency,target_currency,rate,rate_source,created_by)
                values(?,?,?,?,?,?) on conflict(rate_month,source_currency,target_currency,rate_source)
                do update set rate=excluded.rate,created_by=excluded.created_by,created_at=current_timestamp
                """,canonical,from,to,rate,origin,currentActor());
        audit.record("EXCHANGE_RATE_SAVE","ExchangeRate",null,"Monthly exchange rate saved",null,Map.of("month",canonical,"from",from,"to",to,"rate",rate));
    }

    @Transactional
    public UUID createPeriod(UUID clientId, LocalDate start, LocalDate end) {
        if(start==null||end==null||start.isAfter(end)||!YearMonth.from(start).equals(YearMonth.from(end)))throw new IllegalArgumentException("El período debe pertenecer a un único mes.");
        String target=jdbc.query("select primary_currency from client where id=? and active",(rs,row)->rs.getString(1),clientId).stream().findFirst().orElseThrow(()->new IllegalArgumentException("El cliente no está activo."));
        Map<String,Object> rate=target.equals("USD")?null:jdbc.queryForList("""
                select id,rate from exchange_rate where rate_month=? and source_currency='USD' and target_currency=?
                order by created_at desc limit 1
                """,start.withDayOfMonth(1),target).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Falta la tasa USD/"+target+" para el mes."));
        UUID id=UUID.randomUUID();
        jdbc.update("insert into report_period(id,client_id,period_start,period_end,report_currency,exchange_rate_id,exchange_rate_snapshot,created_by) values(?,?,?,?,?,?,?,?)",
                id,clientId,start,end,target,rate==null?null:rate.get("id"),rate==null?BigDecimal.ONE:rate.get("rate"),currentActor());
        audit.record("REPORT_PERIOD_CREATE","ReportPeriod",id.toString(),"Monthly review period created",null,Map.of("clientId",clientId,"start",start,"end",end));
        return id;
    }

    public List<Map<String,Object>> periods(){return jdbc.queryForList("""
            select rp.*,c.code,c.commercial_name from report_period rp join client c on c.id=rp.client_id order by period_start desc,c.commercial_name
            """);}
    public List<Map<String,Object>> rates(){return jdbc.queryForList("select * from exchange_rate order by rate_month desc,source_currency,target_currency");}
    public List<Map<String,Object>> clients(){return jdbc.queryForList("select id,code,commercial_name from client where active order by commercial_name");}
    public Map<String,Object> period(UUID id){return jdbc.queryForMap("select rp.*,c.code,c.commercial_name from report_period rp join client c on c.id=rp.client_id where rp.id=?",id);}
    public List<Map<String,Object>> review(UUID id){return jdbc.queryForList("""
            select a.meta_ad_id,a.ad_name,c.campaign_name,sum(i.spend) spend_source,
                   sum(i.spend)*rp.exchange_rate_snapshot spend_report,sum(i.impressions) impressions,
                   sum(i.reach) reach,sum(i.clicks) clicks,
                   case when sum(i.impressions)=0 then 0 else sum(i.clicks)::numeric*100/sum(i.impressions) end ctr
            from report_period rp join ad_classification ac on ac.client_id=rp.client_id
            join meta_ad a on a.id=ac.ad_id join meta_ad_set ads on ads.id=a.ad_set_id join meta_campaign c on c.id=ads.campaign_id
            join daily_ad_insight i on i.ad_id=a.id and i.insight_date between rp.period_start and rp.period_end
            where rp.id=? group by a.id,a.meta_ad_id,a.ad_name,c.campaign_name,rp.exchange_rate_snapshot order by c.campaign_name,a.ad_name
            """,id);}

    private String required(JsonNode node,String field){String value=node.path(field).asText("").trim();if(value.isEmpty())throw new IllegalArgumentException("Campo requerido ausente: "+field);return value;}
    private String currency(String value){String result=value==null?"":value.trim().toUpperCase();if(!result.matches("[A-Z]{3}"))throw new IllegalArgumentException("Moneda inválida.");return result;}
    private UUID currentActor(){var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null)return null;return jdbc.query("select id from app_user where username=?",(rs,row)->rs.getObject(1,UUID.class),auth.getName()).stream().findFirst().orElse(null);}
}
