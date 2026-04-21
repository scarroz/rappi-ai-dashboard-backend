package com.rappi.dashboard.dto.mcp;

import java.util.List;

/**
 * Resultado del análisis de patrones calculado en Java.
 * El agente recibe este objeto ya procesado y solo necesita
 * interpretarlo y comunicarlo en lenguaje natural.
 *
 * Todos los cálculos estadísticos se hacen en InsightService,
 * no en el modelo — los LLMs pequeños son poco confiables con aritmética.
 */
public record McpInsightResult(

        // ── Tendencia general ─────────────────────────────────────
        String  overallTrend,           // "CRECIENTE", "DECRECIENTE", "ESTABLE"
        long    firstDayAvg,
        long    lastDayAvg,
        double  trendChangePercent,     // % de cambio del primer al último día

        // ── Patrón weekday vs weekend ─────────────────────────────
        long    weekdayAvg,
        long    weekendAvg,
        double  weekdayVsWeekendPercent,// % diferencia

        // ── Día más activo y menos activo ─────────────────────────
        String  peakDay,                // fecha del día con mayor avg
        long    peakDayAvg,
        String  lowDay,                 // fecha del día con menor avg
        long    lowDayAvg,

        // ── Volatilidad diaria ────────────────────────────────────
        String  mostVolatileDay,        // día con mayor diferencia max-min
        long    mostVolatileRange,      // max - min de ese día
        String  leastVolatileDay,
        long    leastVolatileRange,

        // ── Horas críticas ────────────────────────────────────────
        int     morningPeakHour,        // hora pico en franja 6-12
        long    morningPeakAvg,
        int     afternoonPeakHour,      // hora pico en franja 12-18
        long    afternoonPeakAvg,
        int     nightPeakHour,          // hora pico en franja 18-24
        long    nightPeakAvg,
        int     valleyHour,             // hora con menos actividad
        long    valleyAvg,

        // ── Anomalias detectadas ──────────────────────────────────
        List<String> anomalies          // caídas/picos detectados como texto
) {}
