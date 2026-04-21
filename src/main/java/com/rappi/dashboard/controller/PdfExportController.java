package com.rappi.dashboard.controller;

import com.rappi.dashboard.service.PdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@Tag(
        name = "Reports",
        description = "Exportación de reportes PDF con insights analíticos del dashboard."
)
public class PdfExportController {

    private final PdfExportService pdfExportService;

    public PdfExportController(PdfExportService pdfExportService) {
        this.pdfExportService = pdfExportService;
    }

    @Operation(
            summary = "Exportar PDF de insights ejecutivos",
            description = "Genera un PDF con KPIs globales, resumen diario, patrón horario, anomalías y recomendaciones."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF generado correctamente",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)
            )
    })
    @GetMapping(value = "/insights/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<ByteArrayResource> exportInsightsPdf() {
        byte[] pdfBytes = pdfExportService.exportImportantInsightsPdf();
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=insights-rappi-dashboard.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }
}