package com.rappi.dashboard.dto.mcp;

/**
 * Entrada del resumen diario retornado al agente MCP.
 */
public record McpDailySummaryResult(
    String date,
    String dayName,
    long   minValue,
    long   maxValue,
    long   avgValue,
    int    dataPoints
) {}
