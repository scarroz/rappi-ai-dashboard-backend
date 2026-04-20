package com.rappi.dashboard.controller;

import com.rappi.dashboard.dto.analytics.*;
import com.rappi.dashboard.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@Tag(
    name        = "Analytics",
    description = "REST endpoints de análisis histórico para el frontend. " +
                  "Período: 2026-02-01 al 2026-02-11, granularidad 10 segundos."
)
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(
        summary     = "KPIs globales del dataset",
        description = "Total de mediciones, min/max/avg tiendas visibles, rango de fechas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = GlobalStatsDto.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<GlobalStatsDto> getGlobalStats() {
        return ResponseEntity.ok(analyticsService.getGlobalStats());
    }

    @Operation(
        summary     = "Tendencia diaria de visibilidad",
        description = "Resumen por día: min, max, promedio de tiendas visibles y total de mediciones."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = DailyTrendDto.class)))
    })
    @GetMapping("/trend")
    public ResponseEntity<DailyTrendDto> getDailyTrend() {
        return ResponseEntity.ok(analyticsService.getDailyTrend());
    }

    @Operation(
        summary     = "Patrón horario promedio",
        description = "Promedio de tiendas visibles por hora del día (0-23) sobre todo el período. " +
                      "Incluye la hora pico global."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = HourlyPatternResponseDto.class)))
    })
    @GetMapping("/hourly-pattern")
    public ResponseEntity<HourlyPatternResponseDto> getHourlyPattern() {
        return ResponseEntity.ok(analyticsService.getHourlyPattern());
    }

    @Operation(
        summary     = "Serie de tiempo por fecha",
        description = "Medición cada 10 segundos para una fecha específica. " +
                      "Rango válido: 2026-02-01 al 2026-02-11."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = TimeSeriesDto.class))),
        @ApiResponse(responseCode = "400", description = "Fecha fuera del rango del dataset",
            content = @Content(schema = @Schema(type = "string")))
    })
    @GetMapping("/timeseries")
    public ResponseEntity<?> getTimeSeries(
            @Parameter(
                description = "Fecha ISO (YYYY-MM-DD). Rango: 2026-02-01 a 2026-02-11.",
                example     = "2026-02-06",
                required    = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate minDate = LocalDate.of(2026, 2, 1);
        LocalDate maxDate = LocalDate.of(2026, 2, 11);

        if (date.isBefore(minDate) || date.isAfter(maxDate)) {
            return ResponseEntity.badRequest()
                .body("Fecha fuera del rango del dataset (2026-02-01 al 2026-02-11).");
        }

        return ResponseEntity.ok(analyticsService.getTimeSeriesByDate(date));
    }
}
