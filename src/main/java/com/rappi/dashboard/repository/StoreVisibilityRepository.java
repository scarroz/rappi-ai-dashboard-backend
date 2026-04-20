package com.rappi.dashboard.repository;

import com.rappi.dashboard.model.StoreVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoreVisibilityRepository extends JpaRepository<StoreVisibility, Long> {

    // ── Carga ─────────────────────────────────────────────────

    boolean existsByTimestamp(LocalDateTime timestamp);

    // ── KPIs globales ─────────────────────────────────────────

    @Query("SELECT COUNT(s) FROM StoreVisibility s")
    long countAll();

    @Query("SELECT MIN(s.visibleStores) FROM StoreVisibility s")
    long globalMin();

    @Query("SELECT MAX(s.visibleStores) FROM StoreVisibility s")
    long globalMax();

    @Query("SELECT CAST(AVG(s.visibleStores) AS long) FROM StoreVisibility s")
    long globalAvg();

    @Query("SELECT MIN(s.timestamp) FROM StoreVisibility s")
    LocalDateTime earliestTimestamp();

    @Query("SELECT MAX(s.timestamp) FROM StoreVisibility s")
    LocalDateTime latestTimestamp();

    @Query("SELECT COUNT(DISTINCT s.dateOnly) FROM StoreVisibility s")
    int countDistinctDays();

    // ── Resumen diario ────────────────────────────────────────

    @Query("""
        SELECT s.dateOnly,
               MIN(s.visibleStores),
               MAX(s.visibleStores),
               CAST(AVG(s.visibleStores) AS long),
               COUNT(s)
        FROM StoreVisibility s
        GROUP BY s.dateOnly
        ORDER BY s.dateOnly
        """)
    List<Object[]> dailySummary();

    // ── Serie de tiempo por día ───────────────────────────────

    @Query("""
        SELECT s.timestamp, s.visibleStores
        FROM StoreVisibility s
        WHERE s.dateOnly = :date
        ORDER BY s.timestamp
        """)
    List<Object[]> timeSeriesByDate(@Param("date") LocalDate date);

    // ── Patrón horario ────────────────────────────────────────

    @Query("""
        SELECT s.hourOfDay,
               CAST(AVG(s.visibleStores) AS long),
               MIN(s.visibleStores),
               MAX(s.visibleStores)
        FROM StoreVisibility s
        GROUP BY s.hourOfDay
        ORDER BY s.hourOfDay
        """)
    List<Object[]> hourlyPattern();

    // ── Para contexto del agente MCP ──────────────────────────
    // Sin LIMIT en JPQL — se toma el primero en Java

    @Query("""
        SELECT s.dateOnly,
               CAST(AVG(s.visibleStores) AS long),
               MAX(s.visibleStores)
        FROM StoreVisibility s
        GROUP BY s.dateOnly
        ORDER BY AVG(s.visibleStores) DESC
        """)
    List<Object[]> allDaysByAvgVisibilityDesc();

    @Query("""
        SELECT s.hourOfDay, CAST(AVG(s.visibleStores) AS long)
        FROM StoreVisibility s
        GROUP BY s.hourOfDay
        ORDER BY AVG(s.visibleStores) DESC
        """)
    List<Object[]> allHoursByAvgVisibilityDesc();

    // ── Para McpTimeSeriesResult (resumen por hora de un día) ─

    @Query("""
        SELECT s.hourOfDay,
               CAST(AVG(s.visibleStores) AS long)
        FROM StoreVisibility s
        WHERE s.dateOnly = :date
        GROUP BY s.hourOfDay
        ORDER BY AVG(s.visibleStores) DESC
        """)
    List<Object[]> hourlyAvgByDate(@Param("date") LocalDate date);
}
