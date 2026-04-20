package com.rappi.dashboard.dto.mcp;

/**
 * Entrada del patrón horario retornado al agente MCP.
 */
public record McpHourlyPatternResult(
    int  hour,
    long avgValue,
    long minValue,
    long maxValue
) {}
