package com.rappi.dashboard.dto.analytics;

import java.util.List;

/**
 * Tendencia diaria completa del período.
 */
public record DailyTrendDto(
    List<DailySummaryDto> days
) {}
