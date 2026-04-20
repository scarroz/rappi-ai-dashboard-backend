package com.rappi.dashboard.dto.mcp;

/**
 * Resumen estadístico de la serie de tiempo para una fecha.
 * No se devuelven los ~8000 puntos crudos — solo el resumen,
 * que es lo que el agente necesita para responder preguntas.
 */
public record McpTimeSeriesResult(
    String date,
    int    totalPoints,
    long   minValue,
    long   maxValue,
    long   avgValue,
    int    peakHour,
    long   peakHourAvg
) {}
