package com.rappi.dashboard.dto.mcp;

/**
 * Resultado compacto de get_global_stats para consumo del agente MCP.
 * Serializado a JSON y devuelto como texto al modelo.
 */
public record McpGlobalStatsResult(
    long   totalDataPoints,
    long   minVisibleStores,
    long   maxVisibleStores,
    long   avgVisibleStores,
    String periodStart,
    String periodEnd,
    int    totalDays
) {}
