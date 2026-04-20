package com.rappi.dashboard.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rappi.dashboard.dto.analytics.DailySummaryDto;
import com.rappi.dashboard.dto.analytics.HourlyPatternDto;
import com.rappi.dashboard.dto.analytics.HourlyPatternResponseDto;
import com.rappi.dashboard.dto.analytics.GlobalStatsDto;
import com.rappi.dashboard.dto.mcp.*;
import com.rappi.dashboard.service.AnalyticsService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Expone las herramientas de análisis al agente externo (Gemma via Ollama)
 * a través del protocolo MCP sobre SSE.
 *
 * Cada método anotado con @Tool es registrado automáticamente por
 * spring-ai-starter-mcp-server-webmvc como una tool invocable por el agente.
 *
 * Principio: solo lectura, resultados compactos.
 * El agente llama solo lo que necesita — no se le envía contexto masivo.
 */
@Service
public class StoreVisibilityMcpTools {

    private static final Logger log = Logger.getLogger(StoreVisibilityMcpTools.class.getName());

    private final AnalyticsService analyticsService;
    private final ObjectMapper     objectMapper;

    public StoreVisibilityMcpTools(AnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper     = objectMapper;
    }

    // ── Tool 1: KPIs Globales ─────────────────────────────────

    @Tool(
        name        = "get_global_stats",
        description = """
            Returns global KPIs of the Rappi store availability dataset.
            Includes: total data points, min/max/avg visible stores,
            period start/end dates, and total days covered.
            Use this tool when the user asks for general statistics,
            totals, averages, or overall dataset summary.
            """
    )
    public String getGlobalStats() {
        log.info("MCP Tool invoked: get_global_stats");
        try {
            GlobalStatsDto stats = analyticsService.getGlobalStats();
            McpGlobalStatsResult result = new McpGlobalStatsResult(
                stats.totalDataPoints(),
                stats.minVisibleStores(),
                stats.maxVisibleStores(),
                stats.avgVisibleStores(),
                stats.dataRangeStart(),
                stats.dataRangeEnd(),
                stats.totalDays()
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.severe("get_global_stats error: " + e.getMessage());
            return "{\"error\": \"Could not retrieve global stats\"}";
        }
    }

    // ── Tool 2: Tendencia Diaria ──────────────────────────────

    @Tool(
        name        = "get_daily_trend",
        description = """
            Returns the daily summary of store visibility for the full period
            (2026-02-01 to 2026-02-11). Each day includes: min, max, avg
            visible stores and total data points.
            Use this when the user asks which day had the most/least active stores,
            daily comparisons, or weekly patterns.
            """
    )
    public String getDailyTrend() {
        log.info("MCP Tool invoked: get_daily_trend");
        try {
            List<McpDailySummaryResult> results = analyticsService.getDailyTrend().days()
                .stream()
                .map(d -> new McpDailySummaryResult(
                    d.date().toString(),
                    d.dayName(),
                    d.minValue(),
                    d.maxValue(),
                    d.avgValue(),
                    d.dataPoints()
                ))
                .toList();
            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.severe("get_daily_trend error: " + e.getMessage());
            return "{\"error\": \"Could not retrieve daily trend\"}";
        }
    }

    // ── Tool 3: Patrón Horario ────────────────────────────────

    @Tool(
        name        = "get_hourly_pattern",
        description = """
            Returns the average number of visible stores per hour of day (0-23),
            aggregated across all 11 days of the dataset.
            Also includes the identified peak hour and its average value.
            Use this when the user asks about peak hours, rush hours,
            best time to order, or hourly activity patterns.
            """
    )
    public String getHourlyPattern() {
        log.info("MCP Tool invoked: get_hourly_pattern");
        try {
            HourlyPatternResponseDto pattern = analyticsService.getHourlyPattern();
            List<McpHourlyPatternResult> results = pattern.hours()
                .stream()
                .map(h -> new McpHourlyPatternResult(
                    h.hour(),
                    h.avgValue(),
                    h.minValue(),
                    h.maxValue()
                ))
                .toList();
            // Wrap with peak info
            record Response(List<McpHourlyPatternResult> hours, int peakHour, long peakAvgValue) {}
            return objectMapper.writeValueAsString(
                new Response(results, pattern.peakHour(), pattern.peakAvgValue())
            );
        } catch (Exception e) {
            log.severe("get_hourly_pattern error: " + e.getMessage());
            return "{\"error\": \"Could not retrieve hourly pattern\"}";
        }
    }

    // ── Tool 4: Serie de Tiempo por Fecha ─────────────────────

    @Tool(
        name        = "get_timeseries_by_date",
        description = """
            Returns statistical summary of store visibility for a specific date.
            Includes: total measurements, min/max/avg visible stores,
            and peak hour for that day.
            Valid dates: 2026-02-01 through 2026-02-11.
            Use this when the user asks about a specific date or day.
            """
    )
    public String getTimeseriesByDate(
            @ToolParam(
                description = "Date in ISO format YYYY-MM-DD. Valid range: 2026-02-01 to 2026-02-11."
            )
            String date) {

        log.info("MCP Tool invoked: get_timeseries_by_date, date=" + date);
        try {
            LocalDate localDate = LocalDate.parse(date);

            // Validar rango
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

            // Hora pico del día
            List<Object[]> hourlyRows = analyticsService.hourlyAvgByDate(localDate);
            int  peakHour    = hourlyRows.isEmpty() ? -1 : ((Number) hourlyRows.get(0)[0]).intValue();
            long peakHourAvg = hourlyRows.isEmpty() ? 0  : ((Number) hourlyRows.get(0)[1]).longValue();

            McpTimeSeriesResult result = new McpTimeSeriesResult(
                date, points.size(), minVal, maxVal, avgVal, peakHour, peakHourAvg
            );
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.severe("get_timeseries_by_date error: " + e.getMessage());
            return "{\"error\": \"Invalid date or internal error: " + e.getMessage() + "\"}";
        }
    }
}
