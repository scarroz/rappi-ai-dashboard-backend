package com.rappi.dashboard.dto.analytics;

import java.util.List;

/**
 * Respuesta completa del patrón horario con la hora pico identificada.
 */
public record HourlyPatternResponseDto(
    List<HourlyPatternDto> hours,
    int                    peakHour,
    long                   peakAvgValue
) {}
