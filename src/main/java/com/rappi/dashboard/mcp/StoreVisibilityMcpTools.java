package com.rappi.dashboard.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.dashboard.dto.analytics.HourlyPatternResponseDto;
import com.rappi.dashboard.dto.analytics.GlobalStatsDto;
import com.rappi.dashboard.dto.mcp.*;
import com.rappi.dashboard.service.AnalyticsService;
import com.rappi.dashboard.service.InsightService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tools del agente — principios de diseño:
 *
 * 1. CÁLCULO EN JAVA, NO EN EL MODELO: todos los números y patrones
 *    los calcula InsightService/AnalyticsService. El modelo solo interpreta.
 *
 * 2. CACHÉ EN MEMORIA: los datos del dataset son estáticos (no cambian
 *    una vez cargados). Las tools cachean sus resultados en el primer
 *    llamado para eliminar latencia de BD en llamadas sucesivas.
 *
 * 3. RESPUESTAS COMPACTAS: los JSON retornados son lo mínimo necesario
 *    para que el modelo genere el insight — no los ~67K puntos crudos.
 */
@Service
public class StoreVisibilityMcpTools {

    private static final Logger log = Logger.getLogger(StoreVisibilityMcpTools.class.getName());

    // Caché simple en memoria — los datos son inmutables post-carga
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private final AnalyticsService analyticsService;
    private final InsightService   insightService;
    private final ObjectMapper     objectMapper;

    public StoreVisibilityMcpTools(
            AnalyticsService analyticsService,
            InsightService   insightService,
            ObjectMapper     objectMapper) {
        this.analyticsService = analyticsService;
        this.insightService   = insightService;
        this.objectMapper     = objectMapper;
    }

    // ── Tool 1: KPIs Globales ─────────────────────────────────

    @Tool(
            name        = "get_global_stats",
            description = """
            Returns global KPIs of the Rappi store availability dataset.
            Includes total data points, min/max/avg visible stores,
            period start/end dates, and total days covered.
            Use when the user asks for general statistics, totals,
            averages, or overall dataset summary.
            """
    )
    public String getGlobalStats() {
        return cache.computeIfAbsent("global_stats", k -> {
            log.info("Tool: get_global_stats (cache miss)");
            try {
                GlobalStatsDto stats = analyticsService.getGlobalStats();
                McpGlobalStatsResult r = new McpGlobalStatsResult(
                        stats.totalDataPoints(), stats.minVisibleStores(),
                        stats.maxVisibleStores(), stats.avgVisibleStores(),
                        stats.dataRangeStart(), stats.dataRangeEnd(), stats.totalDays()
                );
                return objectMapper.writeValueAsString(r);
            } catch (Exception e) {
                log.severe("get_global_stats error: " + e.getMessage());
                return "{\"error\": \"Could not retrieve global stats\"}";
            }
        });
    }

    // ── Tool 2: Tendencia Diaria ──────────────────────────────

    @Tool(
            name        = "get_daily_trend",
            description = """
            Returns daily summary of store visibility for the full period
            (2026-02-01 to 2026-02-11). Each day: min, max, avg visible stores.
            Use when the user asks which day had most/least stores, daily
            comparisons, or trends across the period.
            """
    )
    public String getDailyTrend() {
        return cache.computeIfAbsent("daily_trend", k -> {
            log.info("Tool: get_daily_trend (cache miss)");
            try {
                List<McpDailySummaryResult> results = analyticsService.getDailyTrend().days()
                        .stream()
                        .map(d -> new McpDailySummaryResult(
                                d.date().toString(), d.dayName(),
                                d.minValue(), d.maxValue(), d.avgValue(), d.dataPoints()
                        ))
                        .toList();
                return objectMapper.writeValueAsString(results);
            } catch (Exception e) {
                log.severe("get_daily_trend error: " + e.getMessage());
                return "{\"error\": \"Could not retrieve daily trend\"}";
            }
        });
    }

    // ── Tool 3: Patrón Horario ────────────────────────────────

    @Tool(
            name        = "get_hourly_pattern",
            description = """
            Returns avg visible stores per hour of day (0-23) across all 11 days.
            Includes identified peak hour and its average.
            Use when the user asks about peak hours, rush hours,
            or hourly activity patterns.
            """
    )
    public String getHourlyPattern() {
        return cache.computeIfAbsent("hourly_pattern", k -> {
            log.info("Tool: get_hourly_pattern (cache miss)");
            try {
                HourlyPatternResponseDto pattern = analyticsService.getHourlyPattern();
                List<McpHourlyPatternResult> results = pattern.hours().stream()
                        .map(h -> new McpHourlyPatternResult(
                                h.hour(), h.avgValue(), h.minValue(), h.maxValue()
                        ))
                        .toList();
                record Response(List<McpHourlyPatternResult> hours, int peakHour, long peakAvgValue) {}
                return objectMapper.writeValueAsString(
                        new Response(results, pattern.peakHour(), pattern.peakAvgValue())
                );
            } catch (Exception e) {
                log.severe("get_hourly_pattern error: " + e.getMessage());
                return "{\"error\": \"Could not retrieve hourly pattern\"}";
            }
        });
    }

    // ── Tool 4: Serie de Tiempo por Fecha ─────────────────────

    @Tool(
            name        = "get_timeseries_by_date",
            description = """
            Returns statistical summary for a specific date.
            Includes total measurements, min/max/avg visible stores, and peak hour.
            Valid dates: 2026-02-01 through 2026-02-11.
            Use when the user asks about a specific date or day.
            """
    )
    public String getTimeseriesByDate(
            @ToolParam(description = "Date in ISO format YYYY-MM-DD. Range: 2026-02-01 to 2026-02-11.")
            String date) {

        String cacheKey = "timeseries_" + date;
        return cache.computeIfAbsent(cacheKey, k -> {
            log.info("Tool: get_timeseries_by_date date=" + date + " (cache miss)");
            try {
                LocalDate localDate = LocalDate.parse(date);
                LocalDate min = LocalDate.of(2026, 2, 1);
                LocalDate max = LocalDate.of(2026, 2, 11);
                if (localDate.isBefore(min) || localDate.isAfter(max)) {
                    return "{\"error\": \"Date out of range. Valid: 2026-02-01 to 2026-02-11\"}";
                }

                var points = analyticsService.getTimeSeriesByDate(localDate).points();
                if (points.isEmpty()) {
                    return "{\"error\": \"No data for date: " + date + "\"}";
                }

                long minVal  = points.stream().mapToLong(p -> p.value()).min().orElse(0);
                long maxVal  = points.stream().mapToLong(p -> p.value()).max().orElse(0);
                long avgVal  = (long) points.stream().mapToLong(p -> p.value()).average().orElse(0);

                List<Object[]> hourlyRows = analyticsService.hourlyAvgByDate(localDate);
                int  peakHour    = hourlyRows.isEmpty() ? -1 : ((Number) hourlyRows.get(0)[0]).intValue();
                long peakHourAvg = hourlyRows.isEmpty() ? 0  : ((Number) hourlyRows.get(0)[1]).longValue();

                McpTimeSeriesResult result = new McpTimeSeriesResult(
                        date, points.size(), minVal, maxVal, avgVal, peakHour, peakHourAvg
                );
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.severe("get_timeseries_by_date error: " + e.getMessage());
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        });
    }

    // ── Tool 5: Análisis de Patrones e Insights ───────────────

    @Tool(
            name        = "analyze_patterns",
            description = """
            Performs deep statistical pattern analysis on the full dataset.
            Returns: overall trend (growing/declining/stable), weekday vs weekend
            comparison, peak and low days, daily volatility ranking, hourly peaks
            by time slot (morning/afternoon/night), valley hour, and detected anomalies.
            
            Use when the user asks for:
            - Insights or patterns in the data
            - Anomalies or unusual behavior
            - Weekday vs weekend comparison
            - Business recommendations based on data
            - Summary analysis of the full period
            - "Analiza los datos", "dame insights", "qué patrones ves"
            """
    )
    public String analyzePatterns() {
        return cache.computeIfAbsent("patterns", k -> {
            log.info("Tool: analyze_patterns (cache miss) — calculando patrones...");
            try {
                McpInsightResult result = insightService.analyzePatterns();
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.severe("analyze_patterns error: " + e.getMessage());
                return "{\"error\": \"Could not compute pattern analysis: " + e.getMessage() + "\"}";
            }
        });
    }
}
