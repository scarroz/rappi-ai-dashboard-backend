package com.rappi.dashboard.dto.analytics;

import java.time.LocalDate;

/**
 * Resumen de disponibilidad de un día específico.
 */
public record DailySummaryDto(
    LocalDate date,
    String    dayName,
    long      minValue,
    long      maxValue,
    long      avgValue,
    int       dataPoints
) {}
