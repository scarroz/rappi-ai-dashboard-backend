package com.rappi.dashboard.dto.analytics;

import java.time.LocalDateTime;

/**
 * Punto individual de la serie de tiempo (granularidad 10s).
 */
public record TimeSeriesPointDto(
    LocalDateTime timestamp,
    long          value
) {}
