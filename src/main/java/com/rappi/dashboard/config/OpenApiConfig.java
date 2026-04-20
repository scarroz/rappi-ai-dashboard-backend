package com.rappi.dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Rappi Store Availability — REST API + MCP Server")
                .version("2.0.0")
                .description("""
                    Backend dual-purpose:
                    
                    **REST API** — Endpoints de analytics para el frontend React.
                    Datos históricos de `synthetic_monitoring_visible_stores`
                    del período 2026-02-01 al 2026-02-11 (granularidad 10s).
                    
                    **MCP Server** — Expone 4 tools al agente Gemma via SSE en `/sse`.
                    El agente invoca las tools según la pregunta del usuario,
                    obtiene solo los datos necesarios y genera la respuesta.
                    
                    Tools disponibles para el agente:
                    - `get_global_stats` — KPIs globales del dataset
                    - `get_daily_trend` — Resumen por día
                    - `get_hourly_pattern` — Patrón por hora del día
                    - `get_timeseries_by_date` — Estadísticas de un día específico
                    """)
                .contact(new Contact()
                    .name("Rappi Engineering")
                    .url("https://rappi.com")
                )
            )
            .servers(List.of(
                new Server().url("http://localhost:8081").description("Local Development")
            ));
    }
}
