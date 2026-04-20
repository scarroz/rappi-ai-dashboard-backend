package com.rappi.dashboard.service;

import com.rappi.dashboard.model.StoreVisibility;
import com.rappi.dashboard.repository.StoreVisibilityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Carga los CSVs del ZIP al arrancar la aplicación.
 * Estrategia: si la tabla ya tiene datos, omite la carga.
 * Deduplicación: timestamps repetidos entre archivos se promedian.
 */
@Service
public class DataLoaderService implements ApplicationRunner {

    private static final Logger log = Logger.getLogger(DataLoaderService.class.getName());
    private static final int    BATCH_SIZE = 500;

    private static final DateTimeFormatter TS_FORMATTER =
        DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss", Locale.ENGLISH);

    @Value("${data.zip.path}")
    private String zipPath;

    private final StoreVisibilityRepository repository;

    public DataLoaderService(StoreVisibilityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long existing = repository.countAll();
        if (existing > 0) {
            log.info("DataLoader: BD ya contiene " + existing + " registros. Omitiendo carga.");
            return;
        }

        Path zip = resolveZipPath();
        if (!Files.exists(zip)) {
            log.warning("DataLoader: ZIP no encontrado en: " + zip.toAbsolutePath());
            return;
        }

        log.info("DataLoader: Cargando desde " + zip.toAbsolutePath());
        long start = System.currentTimeMillis();

        Map<LocalDateTime, List<Long>> aggregated = parseZip(zip);
        int inserted = persistData(aggregated);

        log.info(String.format("DataLoader: %d registros insertados en %.1fs",
            inserted, (System.currentTimeMillis() - start) / 1000.0));
    }

    // ── Parsing ───────────────────────────────────────────────

    private Map<LocalDateTime, List<Long>> parseZip(Path zip) throws IOException {
        Map<LocalDateTime, List<Long>> aggregated = new TreeMap<>();
        int filesProcessed = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.endsWith(".csv") || name.startsWith("__MACOSX")) {
                    zis.closeEntry();
                    continue;
                }
                byte[] bytes = zis.readAllBytes();
                parseCsv(new String(bytes, "UTF-8"), aggregated);
                filesProcessed++;
                zis.closeEntry();
            }
        }

        log.info("DataLoader: " + filesProcessed + " CSVs procesados, "
                 + aggregated.size() + " timestamps únicos.");
        return aggregated;
    }

    private void parseCsv(String content, Map<LocalDateTime, List<Long>> aggregated) {
        try (BufferedReader br = new BufferedReader(new StringReader(content))) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            String[] headers = headerLine.split(",");
            List<LocalDateTime> timestamps = new ArrayList<>();
            for (int i = 4; i < headers.length; i++) {
                timestamps.add(parseTimestamp(headers[i].trim()));
            }

            String dataLine;
            while ((dataLine = br.readLine()) != null) {
                if (dataLine.isBlank()) continue;
                String[] cols = dataLine.split(",", -1);
                for (int i = 4; i < cols.length && (i - 4) < timestamps.size(); i++) {
                    LocalDateTime ts = timestamps.get(i - 4);
                    if (ts == null) continue;
                    String raw = cols[i].trim();
                    if (raw.isEmpty()) continue;
                    try {
                        long val = (long) Double.parseDouble(raw);
                        aggregated.computeIfAbsent(ts, k -> new ArrayList<>()).add(val);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            log.warning("DataLoader: Error parseando CSV — " + e.getMessage());
        }
    }

    private LocalDateTime parseTimestamp(String col) {
        int gmtIdx = col.indexOf(" GMT");
        if (gmtIdx == -1) return null;
        String rawTs = col.substring(0, gmtIdx).trim();
        try {
            return LocalDateTime.parse(rawTs, TS_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ── Persistencia ──────────────────────────────────────────

    @Transactional
    public int persistData(Map<LocalDateTime, List<Long>> aggregated) {
        List<StoreVisibility> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;

        for (Map.Entry<LocalDateTime, List<Long>> entry : aggregated.entrySet()) {
            long avg = (long) entry.getValue().stream()
                .mapToLong(Long::longValue).average().orElse(0);
            batch.add(new StoreVisibility(entry.getKey(), avg));

            if (batch.size() >= BATCH_SIZE) {
                repository.saveAll(batch);
                total += batch.size();
                batch.clear();
                log.info("DataLoader: " + total + " registros insertados...");
            }
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            total += batch.size();
        }

        return total;
    }

    private Path resolveZipPath() {
        Path p = Paths.get(zipPath);
        return p.isAbsolute() ? p : Paths.get(System.getProperty("user.dir")).resolve(p);
    }
}
