package com.rappi.dashboard.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.rappi.dashboard.dto.analytics.DailySummaryDto;
import com.rappi.dashboard.dto.analytics.DailyTrendDto;
import com.rappi.dashboard.dto.analytics.GlobalStatsDto;
import com.rappi.dashboard.dto.analytics.HourlyPatternDto;
import com.rappi.dashboard.dto.analytics.HourlyPatternResponseDto;
import com.rappi.dashboard.dto.mcp.McpInsightResult;
import com.rappi.dashboard.util.PdfExportUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PdfExportService {

    private static final DateTimeFormatter GENERATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalyticsService analyticsService;
    private final InsightService insightService;

    public PdfExportService(AnalyticsService analyticsService, InsightService insightService) {
        this.analyticsService = analyticsService;
        this.insightService = insightService;
    }

    public byte[] exportImportantInsightsPdf() {
        try {
            GlobalStatsDto globalStats = analyticsService.getGlobalStats();
            DailyTrendDto dailyTrend = analyticsService.getDailyTrend();
            HourlyPatternResponseDto hourlyPattern = analyticsService.getHourlyPattern();
            McpInsightResult insights = insightService.analyzePatterns();

            DailySummaryDto bestDay = findBestDay(dailyTrend.days());
            DailySummaryDto worstDay = findWorstDay(dailyTrend.days());
            DailySummaryDto mostVolatileDay = findMostVolatileDay(dailyTrend.days());
            HourlyPatternDto peakHourDetail = findPeakHourDetail(hourlyPattern.hours());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = PdfExportUtil.createDocument();
            PdfWriter.getInstance(document, baos);

            document.open();

            String generatedAt = LocalDateTime.now().format(GENERATED_AT_FORMAT);

            PdfExportUtil.addTitle(
                    document,
                    "Reporte Ejecutivo de Insights",
                    "Rappi Store Availability Dashboard · Generado el " + generatedAt
            );

            PdfExportUtil.addSection(document, "1. Resumen ejecutivo");
            PdfExportUtil.addText(
                    document,
                    "Este documento resume los insights más importantes del comportamiento de " +
                            "disponibilidad de tiendas visibles en el período analizado, utilizando los " +
                            "servicios analíticos y estadísticos del backend."
            );

            PdfExportUtil.addSection(document, "2. KPIs globales");
            var kpiTable = PdfExportUtil.createTable(1f, 1f, 1f);
            kpiTable.addCell(PdfExportUtil.keyMetricCell("Total de registros", formatLong(globalStats.totalDataPoints())));
            kpiTable.addCell(PdfExportUtil.keyMetricCell("Promedio global", formatLong(globalStats.avgVisibleStores())));
            kpiTable.addCell(PdfExportUtil.keyMetricCell("Máximo global", formatLong(globalStats.maxVisibleStores())));
            document.add(kpiTable);

            PdfExportUtil.addBullet(document, "Mínimo global observado: " + formatLong(globalStats.minVisibleStores()) + " tiendas visibles.");
            PdfExportUtil.addBullet(document, "Rango temporal: " + globalStats.dataRangeStart() + " a " + globalStats.dataRangeEnd() + ".");
            PdfExportUtil.addBullet(document, "Total de días cubiertos por el dataset: " + globalStats.totalDays() + ".");

            PdfExportUtil.addSection(document, "3. Insights más importantes");
            PdfExportUtil.addBullet(document, "Tendencia general del período: " + insights.overallTrend() + ".");
            PdfExportUtil.addBullet(document, "Cambio del primer al último día: " + formatDouble(insights.trendChangePercent()) + "%.");
            PdfExportUtil.addBullet(document, "Promedio weekday: " + formatLong(insights.weekdayAvg()) + ".");
            PdfExportUtil.addBullet(document, "Promedio weekend: " + formatLong(insights.weekendAvg()) + ".");
            PdfExportUtil.addBullet(
                    document,
                    "Los días de semana estuvieron " +
                            formatDouble(Math.abs(insights.weekdayVsWeekendPercent())) +
                            "% " +
                            (insights.weekdayVsWeekendPercent() >= 0 ? "por encima" : "por debajo") +
                            " del fin de semana."
            );

            if (bestDay != null) {
                PdfExportUtil.addBullet(
                        document,
                        "Día más fuerte: " + bestDay.date() +
                                " (" + bestDay.dayName() + ") con promedio de " +
                                formatLong(bestDay.avgValue()) + "."
                );
            }

            if (worstDay != null) {
                PdfExportUtil.addBullet(
                        document,
                        "Día más débil: " + worstDay.date() +
                                " (" + worstDay.dayName() + ") con promedio de " +
                                formatLong(worstDay.avgValue()) + "."
                );
            }

            if (mostVolatileDay != null) {
                PdfExportUtil.addBullet(
                        document,
                        "Día más volátil: " + mostVolatileDay.date() +
                                " con rango intradía de " +
                                formatLong(mostVolatileDay.maxValue() - mostVolatileDay.minValue()) + "."
                );
            }

            if (peakHourDetail != null) {
                PdfExportUtil.addBullet(
                        document,
                        "Hora pico global: " + peakHourDetail.hour() + ":00 con promedio de " +
                                formatLong(peakHourDetail.avgValue()) + "."
                );
            }

            PdfExportUtil.addBullet(
                    document,
                    "Hora valle global: " + insights.valleyHour() + ":00 con promedio de " +
                            formatLong(insights.valleyAvg()) + "."
            );

            PdfExportUtil.addSection(document, "4. Resumen diario");
            var dailyTable = PdfExportUtil.createTable(1.3f, 1.3f, 1f, 1f, 1f, 1f);
            dailyTable.addCell(PdfExportUtil.headerCell("Fecha"));
            dailyTable.addCell(PdfExportUtil.headerCell("Día"));
            dailyTable.addCell(PdfExportUtil.headerCell("Mínimo"));
            dailyTable.addCell(PdfExportUtil.headerCell("Máximo"));
            dailyTable.addCell(PdfExportUtil.headerCell("Promedio"));
            dailyTable.addCell(PdfExportUtil.headerCell("Puntos"));

            for (DailySummaryDto day : dailyTrend.days()) {
                dailyTable.addCell(PdfExportUtil.valueCell(day.date().toString()));
                dailyTable.addCell(PdfExportUtil.valueCell(day.dayName()));
                dailyTable.addCell(PdfExportUtil.valueCell(formatLong(day.minValue())));
                dailyTable.addCell(PdfExportUtil.valueCell(formatLong(day.maxValue())));
                dailyTable.addCell(PdfExportUtil.valueCell(formatLong(day.avgValue())));
                dailyTable.addCell(PdfExportUtil.valueCell(formatInt(day.dataPoints())));
            }

            document.add(dailyTable);

            PdfExportUtil.addSection(document, "5. Patrón horario");
            var hourlyTable = PdfExportUtil.createTable(0.8f, 1.1f, 1.1f, 1.1f);
            hourlyTable.addCell(PdfExportUtil.headerCell("Hora"));
            hourlyTable.addCell(PdfExportUtil.headerCell("Promedio"));
            hourlyTable.addCell(PdfExportUtil.headerCell("Mínimo"));
            hourlyTable.addCell(PdfExportUtil.headerCell("Máximo"));

            for (HourlyPatternDto hour : hourlyPattern.hours()) {
                hourlyTable.addCell(PdfExportUtil.valueCell(hour.hour() + ":00"));
                hourlyTable.addCell(PdfExportUtil.valueCell(formatLong(hour.avgValue())));
                hourlyTable.addCell(PdfExportUtil.valueCell(formatLong(hour.minValue())));
                hourlyTable.addCell(PdfExportUtil.valueCell(formatLong(hour.maxValue())));
            }

            document.add(hourlyTable);

            PdfExportUtil.addSection(document, "6. Franjas críticas del día");
            PdfExportUtil.addBullet(
                    document,
                    "Mañana: pico a las " + insights.morningPeakHour() + ":00 con " +
                            formatLong(insights.morningPeakAvg()) + "."
            );
            PdfExportUtil.addBullet(
                    document,
                    "Tarde: pico a las " + insights.afternoonPeakHour() + ":00 con " +
                            formatLong(insights.afternoonPeakAvg()) + "."
            );
            PdfExportUtil.addBullet(
                    document,
                    "Noche: pico a las " + insights.nightPeakHour() + ":00 con " +
                            formatLong(insights.nightPeakAvg()) + "."
            );

            PdfExportUtil.addSection(document, "7. Anomalías detectadas");
            if (insights.anomalies() == null || insights.anomalies().isEmpty()) {
                PdfExportUtil.addText(document, "No se detectaron anomalías significativas en el período.");
            } else {
                for (String anomaly : insights.anomalies()) {
                    PdfExportUtil.addBullet(document, anomaly);
                }
            }

            PdfExportUtil.addSection(document, "8. Recomendación de negocio");
            PdfExportUtil.addText(
                    document,
                    "Se recomienda reforzar cobertura operativa y monitoreo en las franjas pico " +
                            "identificadas, especialmente durante las ventanas con mayor promedio de visibilidad. " +
                            "También es conveniente generar alertas proactivas sobre caídas extremas en los días " +
                            "históricamente más fuertes para anticipar incidentes de disponibilidad."
            );

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de insights", e);
        }
    }

    private DailySummaryDto findBestDay(List<DailySummaryDto> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .max(Comparator.comparingLong(DailySummaryDto::avgValue))
                .orElse(null);
    }

    private DailySummaryDto findWorstDay(List<DailySummaryDto> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .min(Comparator.comparingLong(DailySummaryDto::avgValue))
                .orElse(null);
    }

    private DailySummaryDto findMostVolatileDay(List<DailySummaryDto> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .max(Comparator.comparingLong(d -> d.maxValue() - d.minValue()))
                .orElse(null);
    }

    private HourlyPatternDto findPeakHourDetail(List<HourlyPatternDto> hours) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        return hours.stream()
                .max(Comparator.comparingLong(HourlyPatternDto::avgValue))
                .orElse(null);
    }

    private String formatLong(long value) {
        return String.format("%,d", value);
    }

    private String formatInt(int value) {
        return String.format("%,d", value);
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }
}