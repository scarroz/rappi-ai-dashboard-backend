package com.rappi.dashboard.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un punto de medición de synthetic_monitoring_visible_stores.
 * Granularidad: 10 segundos. Período: 2026-02-01 al 2026-02-11.
 */
@Entity
@Table(
    name = "store_visibility",
    indexes = {
        @Index(name = "idx_sv_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sv_date",      columnList = "date_only")
    }
)
public class StoreVisibility {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sv_seq")
    @SequenceGenerator(name = "sv_seq", sequenceName = "store_visibility_seq", allocationSize = 500)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "visible_stores", nullable = false)
    private Long visibleStores;

    @Column(name = "date_only", nullable = false)
    private LocalDate dateOnly;

    @Column(name = "hour_of_day", nullable = false)
    private Integer hourOfDay;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    // ── Constructors ──────────────────────────────────────────

    public StoreVisibility() {}

    public StoreVisibility(LocalDateTime timestamp, Long visibleStores) {
        this.timestamp     = timestamp;
        this.visibleStores = visibleStores;
        this.dateOnly      = timestamp.toLocalDate();
        this.hourOfDay     = timestamp.getHour();
        this.dayOfWeek     = timestamp.getDayOfWeek().getValue();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.dateOnly  = timestamp.toLocalDate();
        this.hourOfDay = timestamp.getHour();
        this.dayOfWeek = timestamp.getDayOfWeek().getValue();
    }

    public Long getVisibleStores() { return visibleStores; }
    public void setVisibleStores(Long visibleStores) { this.visibleStores = visibleStores; }

    public LocalDate getDateOnly() { return dateOnly; }
    public void setDateOnly(LocalDate dateOnly) { this.dateOnly = dateOnly; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    @Override
    public String toString() {
        return "StoreVisibility{id=" + id + ", timestamp=" + timestamp
               + ", visibleStores=" + visibleStores + "}";
    }
}
