package com.rappi.dashboard.service;

import com.rappi.dashboard.dto.analytics.*;
import com.rappi.dashboard.repository.StoreVisibilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final DateTimeFormatter DT_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StoreVisibilityRepository repository;

    public AnalyticsService(StoreVisibilityRepository repository) {
        this.repository = repository;
    }

    // ── KPIs Globales ─────────────────────────────────────────

    public GlobalStatsDto getGlobalStats() {
        long count             = repository.countAll();
        long min               = repository.globalMin();
        long max               = repository.globalMax();
        long avg               = repository.globalAvg();
        LocalDateTime earliest = repository.earliestTimestamp();
        LocalDateTime latest   = repository.latestTimestamp();
        int  days              = repository.countDistinctDays();

        return new GlobalStatsDto(
            count, min, max, avg,
            earliest.format(DT_FORMATTER),
            latest.format(DT_FORMATTER),
            days
        );
    }

    // ── Tendencia Diaria ──────────────────────────────────────

    public DailyTrendDto getDailyTrend() {
        List<Object[]> rows = repository.dailySummary();

        List<DailySummaryDto> days = rows.stream()
            .map(r -> {
                LocalDate date = (LocalDate) r[0];
                long minVal    = ((Number) r[1]).longValue();
                long maxVal    = ((Number) r[2]).longValue();
                long avgVal    = ((Number) r[3]).longValue();
                int  points    = ((Number) r[4]).intValue();
                String dayName = date.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale("es", "CO"));
                return new DailySummaryDto(date, dayName, minVal, maxVal, avgVal, points);
            })
            .toList();

        return new DailyTrendDto(days);
    }

    // ── Serie de Tiempo por Día ───────────────────────────────

    public TimeSeriesDto getTimeSeriesByDate(LocalDate date) {
        List<Object[]> rows = repository.timeSeriesByDate(date);

        List<TimeSeriesPointDto> points = rows.stream()
            .map(r -> new TimeSeriesPointDto(
                (LocalDateTime) r[0],
                ((Number) r[1]).longValue()
            ))
            .toList();

        return new TimeSeriesDto(date, points);
    }

    // ── Patrón Horario Global ─────────────────────────────────

    public HourlyPatternResponseDto getHourlyPattern() {
        List<Object[]> rows = repository.hourlyPattern();

        List<HourlyPatternDto> hours = rows.stream()
            .map(r -> new HourlyPatternDto(
                ((Number) r[0]).intValue(),
                ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue(),
                ((Number) r[3]).longValue()
            ))
            .toList();

        HourlyPatternDto peak = hours.stream()
            .max(Comparator.comparingLong(HourlyPatternDto::avgValue))
            .orElseThrow();

        return new HourlyPatternResponseDto(hours, peak.hour(), peak.avgValue());
    }

    // ── Helpers para MCP Tools ────────────────────────────────

    public List<Object[]> allDaysByAvgDesc() {
        return repository.allDaysByAvgVisibilityDesc();
    }

    public List<Object[]> allHoursByAvgDesc() {
        return repository.allHoursByAvgVisibilityDesc();
    }

    public List<Object[]> hourlyAvgByDate(LocalDate date) {
        return repository.hourlyAvgByDate(date);
    }
}
