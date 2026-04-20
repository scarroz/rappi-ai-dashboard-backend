package com.rappi.dashboard.dto.analytics;

/**
 * Promedio de tiendas visibles para una hora específica del día.
 */
public record HourlyPatternDto(
    int  hour,
    long avgValue,
    long minValue,
    long maxValue
) {}
