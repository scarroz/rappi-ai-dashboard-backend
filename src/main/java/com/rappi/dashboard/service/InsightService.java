package com.rappi.dashboard.service;

import com.rappi.dashboard.dto.analytics.DailySummaryDto;
import com.rappi.dashboard.dto.analytics.HourlyPatternDto;
import com.rappi.dashboard.dto.mcp.McpInsightResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio de análisis estadístico puro.
 *
 * Principio: todos los cálculos ocurren aquí en Java.
 * El modelo (Gemma) recibe el McpInsightResult ya procesado
 * y solo interpreta — no calcula.
 *
 * Esto evita el problema clásico de los LLMs pequeños:
 * errores aritméticos y alucinaciones de números.
 */
@Service
@Transactional(readOnly = true)
public class InsightService {

    private static final Logger log = Logger.getLogger(InsightService.class.getName());

    // Umbral para considerar un día como anomalía (desviación > N% del promedio global)
    private static final double ANOMALY_THRESHOLD_PERCENT = 15.0;

    private final AnalyticsService analyticsService;

    public InsightService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public McpInsightResult analyzePatterns() {
        List<DailySummaryDto>  days  = analyticsService.getDailyTrend().days();
        List<HourlyPatternDto> hours = analyticsService.getHourlyPattern().hours();

        return new McpInsightResult(
                calcOverallTrend(days),
                days.isEmpty() ? 0 : days.get(0).avgValue(),
                days.isEmpty() ? 0 : days.get(days.size() - 1).avgValue(),
                calcTrendChangePercent(days),
                calcWeekdayAvg(days),
                calcWeekendAvg(days),
                calcWeekdayVsWeekendPercent(days),
                findPeakDay(days),
                findPeakDayAvg(days),
                findLowDay(days),
                findLowDayAvg(days),
                findMostVolatileDay(days),
                findMostVolatileRange(days),
                findLeastVolatileDay(days),
                findLeastVolatileRange(days),
                findPeakInRange(hours, 6, 11),
                findPeakAvgInRange(hours, 6, 11),
                findPeakInRange(hours, 12, 17),
                findPeakAvgInRange(hours, 12, 17),
                findPeakInRange(hours, 18, 23),
                findPeakAvgInRange(hours, 18, 23),
                findValleyHour(hours),
                findValleyAvg(hours),
                detectAnomalies(days)
        );
    }

    // ── Tendencia General ─────────────────────────────────────

    private String calcOverallTrend(List<DailySummaryDto> days) {
        if (days.size() < 2) return "ESTABLE";
        double change = calcTrendChangePercent(days);
        if (change > 5.0)  return "CRECIENTE";
        if (change < -5.0) return "DECRECIENTE";
        return "ESTABLE";
    }

    private double calcTrendChangePercent(List<DailySummaryDto> days) {
        if (days.size() < 2) return 0.0;
        long first = days.get(0).avgValue();
        long last  = days.get(days.size() - 1).avgValue();
        if (first == 0) return 0.0;
        return ((double)(last - first) / first) * 100.0;
    }

    // ── Weekday vs Weekend ────────────────────────────────────

    private long calcWeekdayAvg(List<DailySummaryDto> days) {
        return (long) days.stream()
                .filter(d -> {
                    DayOfWeek dow = d.date().getDayOfWeek();
                    return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
                })
                .mapToLong(DailySummaryDto::avgValue)
                .average().orElse(0);
    }

    private long calcWeekendAvg(List<DailySummaryDto> days) {
        return (long) days.stream()
                .filter(d -> {
                    DayOfWeek dow = d.date().getDayOfWeek();
                    return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
                })
                .mapToLong(DailySummaryDto::avgValue)
                .average().orElse(0);
    }

    private double calcWeekdayVsWeekendPercent(List<DailySummaryDto> days) {
        long weekday = calcWeekdayAvg(days);
        long weekend = calcWeekendAvg(days);
        if (weekend == 0) return 0.0;
        return ((double)(weekday - weekend) / weekend) * 100.0;
    }

    // ── Días extremos ─────────────────────────────────────────

    private String findPeakDay(List<DailySummaryDto> days) {
        return days.stream()
                .max(Comparator.comparingLong(DailySummaryDto::avgValue))
                .map(d -> d.date().toString())
                .orElse("N/A");
    }

    private long findPeakDayAvg(List<DailySummaryDto> days) {
        return days.stream()
                .mapToLong(DailySummaryDto::avgValue)
                .max().orElse(0);
    }

    private String findLowDay(List<DailySummaryDto> days) {
        return days.stream()
                .min(Comparator.comparingLong(DailySummaryDto::avgValue))
                .map(d -> d.date().toString())
                .orElse("N/A");
    }

    private long findLowDayAvg(List<DailySummaryDto> days) {
        return days.stream()
                .mapToLong(DailySummaryDto::avgValue)
                .min().orElse(0);
    }

    // ── Volatilidad ───────────────────────────────────────────

    private long rangeOf(DailySummaryDto d) {
        return d.maxValue() - d.minValue();
    }

    private String findMostVolatileDay(List<DailySummaryDto> days) {
        return days.stream()
                .max(Comparator.comparingLong(this::rangeOf))
                .map(d -> d.date().toString())
                .orElse("N/A");
    }

    private long findMostVolatileRange(List<DailySummaryDto> days) {
        return days.stream()
                .mapToLong(this::rangeOf)
                .max().orElse(0);
    }

    private String findLeastVolatileDay(List<DailySummaryDto> days) {
        return days.stream()
                .min(Comparator.comparingLong(this::rangeOf))
                .map(d -> d.date().toString())
                .orElse("N/A");
    }

    private long findLeastVolatileRange(List<DailySummaryDto> days) {
        return days.stream()
                .mapToLong(this::rangeOf)
                .min().orElse(0);
    }

    // ── Horas críticas ────────────────────────────────────────

    private int findPeakInRange(List<HourlyPatternDto> hours, int fromHour, int toHour) {
        return hours.stream()
                .filter(h -> h.hour() >= fromHour && h.hour() <= toHour)
                .max(Comparator.comparingLong(HourlyPatternDto::avgValue))
                .map(HourlyPatternDto::hour)
                .orElse(-1);
    }

    private long findPeakAvgInRange(List<HourlyPatternDto> hours, int fromHour, int toHour) {
        return hours.stream()
                .filter(h -> h.hour() >= fromHour && h.hour() <= toHour)
                .mapToLong(HourlyPatternDto::avgValue)
                .max().orElse(0);
    }

    private int findValleyHour(List<HourlyPatternDto> hours) {
        return hours.stream()
                .min(Comparator.comparingLong(HourlyPatternDto::avgValue))
                .map(HourlyPatternDto::hour)
                .orElse(-1);
    }

    private long findValleyAvg(List<HourlyPatternDto> hours) {
        return hours.stream()
                .mapToLong(HourlyPatternDto::avgValue)
                .min().orElse(0);
    }

    // ── Detección de Anomalías ────────────────────────────────

    /**
     * Detecta días cuyo promedio se desvía significativamente de la media global.
     * También detecta días con caídas extremas (minValue cercano a 0).
     */
    private List<String> detectAnomalies(List<DailySummaryDto> days) {
        if (days.isEmpty()) return List.of();

        double globalAvg = days.stream()
                .mapToLong(DailySummaryDto::avgValue)
                .average().orElse(0);

        List<String> anomalies = new ArrayList<>();

        for (DailySummaryDto day : days) {
            double deviation = Math.abs(day.avgValue() - globalAvg) / globalAvg * 100.0;

            if (deviation > ANOMALY_THRESHOLD_PERCENT) {
                String direction = day.avgValue() > globalAvg ? "pico alto" : "caída";
                anomalies.add(String.format(
                        "%s: %s inusual (avg=%,d, desviación=%.1f%% del promedio global)",
                        day.date(), direction, day.avgValue(), deviation
                ));
            }

            // Caída extrema: mínimo menor al 1% del máximo global
            long globalMax = days.stream().mapToLong(DailySummaryDto::maxValue).max().orElse(1);
            if (day.minValue() < globalMax * 0.01) {
                anomalies.add(String.format(
                        "%s: caída extrema detectada (min=%,d tiendas)",
                        day.date(), day.minValue()
                ));
            }
        }

        return anomalies.isEmpty()
                ? List.of("No se detectaron anomalías significativas en el período.")
                : anomalies;
    }
}
