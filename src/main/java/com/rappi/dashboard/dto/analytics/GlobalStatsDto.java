package com.rappi.dashboard.dto.analytics;

/**
 * KPIs globales del dataset completo.
 */
public record GlobalStatsDto(
    long   totalDataPoints,
    long   minVisibleStores,
    long   maxVisibleStores,
    long   avgVisibleStores,
    String dataRangeStart,
    String dataRangeEnd,
    int    totalDays
) {}
