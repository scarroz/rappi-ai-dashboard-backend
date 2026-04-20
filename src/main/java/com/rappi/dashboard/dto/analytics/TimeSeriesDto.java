package com.rappi.dashboard.dto.analytics;

import java.time.LocalDate;
import java.util.List;

/**
 * Serie de tiempo completa para una fecha dada.
 */
public record TimeSeriesDto(
    LocalDate                date,
    List<TimeSeriesPointDto> points
) {}
