# rappi-ai-dashboard-backend
AI-powered analytics backend for store availability insights.  This service processes historical store status data and exposes advanced analytics through REST APIs, including time series, trends, and global metrics. It also integrates an AI-powered chatbot using MCP tools to enable natural language queries over the data.


**OVERVIEW**

Backend dual-purpose construido con Spring Boot 3 que sirve simultáneamente como:
- **API REST** para alimentar un dashboard de visualización de disponibilidad de tiendas
- **Servidor MCP (Model Context Protocol)** que expone herramientas analíticas a un agente de IA local

El agente conversacional está potenciado por **Gemma 4 E2B**, modelo de lenguaje de Google corriendo localmente vía Ollama, sin dependencia de APIs externas de pago.

---

## Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Stack Tecnológico](#stack-tecnológico)
- [Dataset](#dataset)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Prerrequisitos](#prerrequisitos)
- [Configuración y Arranque](#configuración-y-arranque)
- [Endpoints REST](#endpoints-rest)
- [Agente Conversacional](#agente-conversacional)
- [Decisiones de Diseño](#decisiones-de-diseño)

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend React                           │
│              (dashboard + chat UI)                           │
└──────────────────┬──────────────────┬───────────────────────┘
                   │ REST              │ POST /api/chat
                   ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│               Spring Boot Backend  :8081                     │
│                                                              │
│   REST API  /api/analytics/**   →  dashboard charts         │
│   ChatController  /api/chat     →  chatbot UI               │
│   MCP Server SSE  /sse          →  agentes externos          │
│                                                              │
│   ┌──────────────────────────────────────────────────────┐  │
│   │              StoreVisibilityMcpTools                  │  │
│   │   @Tool  get_global_stats       (+ caché)            │  │
│   │   @Tool  get_daily_trend        (+ caché)            │  │
│   │   @Tool  get_hourly_pattern     (+ caché)            │  │
│   │   @Tool  get_timeseries_by_date (+ caché por fecha)  │  │
│   │   @Tool  analyze_patterns       (+ caché)            │  │
│   └───────────────────┬──────────────────────────────────┘  │
│                        │                                      │
│   ┌────────────────────▼──────────────────────────────────┐  │
│   │      AnalyticsService  +  InsightService               │  │
│   └────────────────────┬──────────────────────────────────┘  │
│                        │                                      │
│   ┌────────────────────▼──────────────────────────────────┐  │
│   │          StoreVisibilityRepository (JPQL)              │  │
│   └────────────────────┬──────────────────────────────────┘  │
│                        │                                      │
└────────────────────────┼──────────────────────────────────── ┘
                         │
          ┌──────────────▼──────────┐   ┌───────────────────────┐
          │   PostgreSQL  :5432      │   │   Ollama  :11434       │
          │   DB: rappi_stores       │   │   Modelo: gemma4:e2b   │
          │   Tabla: store_visibility│   │   (tool calling local) │
          └─────────────────────────┘   └───────────────────────┘
```

### Flujo del Chatbot

```
Usuario pregunta → ChatController → ChatService
                                         │
                               Gemma recibe el mensaje
                               + system prompt (Markdown)
                               + 5 tools disponibles
                                         │
                               Gemma decide qué tool invocar
                                         │
                               Spring AI ejecuta la tool
                               (consulta PostgreSQL)
                                         │
                               Gemma recibe datos reales
                               y genera respuesta en español
                                         │
                                    Respuesta al usuario
```

**Principio clave:** el modelo nunca calcula — solo interpreta. Todo el análisis estadístico ocurre en Java (`InsightService`) antes de que el modelo vea un número.

---

## Stack Tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 21 | Lenguaje |
| Spring Boot | 3.4.1 | Framework principal |
| Spring AI | 1.0.0 | ChatClient, `@Tool`, MCP Server |
| Spring AI Ollama | 1.0.0 | Integración con Gemma local |
| Spring AI MCP Server WebMVC | 1.0.0 | Protocolo MCP sobre SSE |
| Spring Data JPA + Hibernate | (boot managed) | ORM |
| PostgreSQL | 16 | Base de datos |
| Springdoc OpenAPI | 2.5.0 | Swagger UI |
| Maven | 3.x | Build |
| **Ollama** | latest | Runtime del modelo local |
| **Gemma 4 E2B** | gemma4:e2b | Modelo de lenguaje (Google) |

---

## Dataset

El backend procesa **201 archivos CSV** exportados desde **SignalFx/Splunk Observability**, cada uno conteniendo mediciones de la métrica `synthetic_monitoring_visible_stores` — el número de tiendas Rappi visibles en cada instante.

| Característica | Valor |
|---|---|
| Período | 2026-02-01 al 2026-02-11 |
| Granularidad | 1 medición cada 10 segundos |
| Registros únicos | ~67,141 |
| Rango de valores | 0 — 6,198,472 |
| Pico diario | 13:00–17:00h (hora Colombia) |
| Fuente | ZIP con 201 CSVs de SignalFx |

**Patrón identificado en los datos:**
- Días de semana tienen mayor actividad que fines de semana
- El pico de disponibilidad ocurre entre las 17h con promedio de ~5.2M tiendas visibles
- La madrugada (01h–06h) es el valle con menos de 700K tiendas visibles

---

## Estructura del Proyecto

```
backend/
├── data/
│   └── Archivo.zip                    ← ZIP con los 201 CSVs (no incluido en repo)
├── src/main/
│   ├── java/com/rappi/dashboard/
│   │   ├── DashboardApplication.java
│   │   ├── config/
│   │   │   ├── AppConfig.java         ← CORS, ObjectMapper, ToolCallbackProvider (MCP)
│   │   │   └── OpenApiConfig.java     ← Swagger metadata
│   │   ├── controller/
│   │   │   ├── AnalyticsController.java
│   │   │   └── ChatController.java
│   │   ├── dto/
│   │   │   ├── analytics/             ← 7 Records para endpoints REST
│   │   │   ├── chat/                  ← 2 Records para el chatbot
│   │   │   └── mcp/                   ← 5 Records compactos para el agente
│   │   ├── mcp/
│   │   │   └── StoreVisibilityMcpTools.java  ← 5 @Tool con caché ConcurrentHashMap
│   │   ├── model/
│   │   │   └── StoreVisibility.java   ← @Entity JPA
│   │   ├── repository/
│   │   │   └── StoreVisibilityRepository.java
│   │   └── service/
│   │       ├── AnalyticsService.java  ← Queries → DTOs
│   │       ├── InsightService.java    ← Análisis estadístico puro Java
│   │       ├── ChatService.java       ← Orquesta Gemma + tools
│   │       └── DataLoaderService.java ← Carga el ZIP al arrancar
│   └── resources/
│       ├── application.properties
│       └── agent/                     ← Workspace del agente (patrón Hercules)
│           ├── IDENTITY.md            ← Quién es el agente
│           ├── SOUL.md                ← Reglas de comportamiento
│           ├── ORCHESTRATOR.md        ← Tabla de routing de tools
│           └── TOOLS.md               ← Documentación de cada tool
└── pom.xml
```

---

## Prerrequisitos

Verifica que tienes todo instalado antes de continuar:

```bash
java --version    # Java 21+
mvn --version     # Maven 3.9+
ollama --version  # Ollama instalado
psql --version    # PostgreSQL accesible
```

**Instalación de prerrequisitos:**
- Java 21: https://adoptium.net
- Maven: https://maven.apache.org/download.cgi
- Ollama: https://ollama.com/download
- PostgreSQL: usa tu instalación local con PGAdmin

---

## Configuración y Arranque

### Paso 1 — Descargar el modelo

```bash
ollama pull gemma4:e2b
```

> El modelo pesa aproximadamente 2.5GB. Descárgalo antes de continuar.

### Paso 2 — Arrancar Ollama

```bash
ollama serve
```

Ollama debe estar corriendo en `http://localhost:11434` antes de arrancar el backend.

### Paso 3 — Crear la base de datos

En PGAdmin o psql, crea la base de datos:

```sql
CREATE DATABASE rappi;
```

Spring Boot creará la tabla `store_visibility` automáticamente al primer arranque gracias a `ddl-auto=update`.

### Paso 4 — Colocar el dataset

Crea la carpeta `data/` dentro de `backend/` y coloca el ZIP con los CSVs:

```
backend/
└── data/
    └── Archivo.zip    ← aquí va el ZIP con los 201 CSVs
```

### Paso 5 — Configurar variables de entorno

En IntelliJ: **Run → Edit Configurations → Environment Variables**

```
DB_URL=jdbc:postgresql://localhost:5432/rappi_stores
DB_USER=tu_usuario_postgres
DB_PASSWORD=tu_password_postgres
DATA_ZIP_PATH=data/Archivo.zip
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=gemma4:e2b
```

O si prefieres editar directamente `application.properties`, reemplaza los placeholders con los valores reales.

### Paso 6 — Compilar y correr

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

O desde IntelliJ: **Build → Rebuild Project** → Run `DashboardApplication`.

### Qué esperar al arrancar

```
DataLoader: Iniciando carga desde .../data/Archivo.zip
DataLoader: 201 CSVs procesados, 67141 timestamps únicos encontrados.
DataLoader: 500 registros insertados...
...
DataLoader: 67141 registros insertados en ~10s
Tomcat started on port 8080
```

> La carga del dataset solo ocurre la primera vez. Los arranques siguientes detectan que la tabla tiene datos y omiten la carga.

---

## Endpoints REST

El backend expone 4 endpoints de analytics y 1 endpoint de chat.

| Método | URL | Descripción | Response |
|---|---|---|---|
| `GET` | `/api/analytics/stats` | KPIs globales del dataset | `GlobalStatsDto` |
| `GET` | `/api/analytics/trend` | Resumen por día (11 días) | `DailyTrendDto` |
| `GET` | `/api/analytics/hourly-pattern` | Patrón por hora 0–23 + hora pico | `HourlyPatternResponseDto` |
| `GET` | `/api/analytics/timeseries?date=2026-02-06` | Serie de tiempo de un día (cada 10s) | `TimeSeriesDto` |
| `POST` | `/api/chat` | Mensaje al agente conversacional | `ChatResponseDto` |

### Swagger UI

Documentación interactiva disponible en:

```
http://localhost:8080/swagger-ui.html
```

Todos los endpoints tienen ejemplos, schemas de request/response y descripción de parámetros.

### Ejemplo de uso del chatbot

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "¿Cuál fue el día con más tiendas activas?"}'
```

```json
{
  "response": "El día con mayor actividad fue el jueves 5 de febrero de 2026, con un promedio de 3,851,511 tiendas visibles.",
  "toolUsed": null
}
```

---

## Agente Conversacional

### Cómo funciona

El agente utiliza **tool calling** — capacidad nativa de Gemma 4 E2B para invocar funciones externas cuando necesita datos reales. El flujo es:

1. El usuario hace una pregunta
2. Gemma analiza el mensaje y decide qué herramienta invocar
3. Spring AI ejecuta el método Java correspondiente
4. PostgreSQL retorna los datos reales
5. Gemma genera la respuesta en español con datos exactos

### Herramientas disponibles

| Tool | Cuándo se invoca |
|---|---|
| `get_global_stats` | Totales, promedios globales, alcance del dataset |
| `get_daily_trend` | Mejor/peor día, comparaciones entre días |
| `get_hourly_pattern` | Hora pico, franjas horarias, patrones del día |
| `get_timeseries_by_date` | Pregunta sobre fecha específica |
| `analyze_patterns` | Insights, anomalías, weekday vs weekend, análisis completo |

### Caché de herramientas

Los resultados de cada tool se cachean en memoria (`ConcurrentHashMap`) al primer llamado. Como el dataset es estático (no cambia en tiempo real), las consultas siguientes evitan el round-trip a PostgreSQL. Esto reduce significativamente la latencia en sesiones con múltiples preguntas.

### Workspace del agente

El comportamiento del agente está definido en 4 archivos Markdown en `src/main/resources/agent/`:

| Archivo | Contenido |
|---|---|
| `IDENTITY.md` | Rol, idioma (siempre español), capacidades y restricciones |
| `SOUL.md` | Reglas: no inventar datos, no salirse del tema, formato de respuesta |
| `ORCHESTRATOR.md` | Tabla de decisión: señal en el mensaje → tool a invocar |
| `TOOLS.md` | Documentación detallada de cada herramienta |

Para modificar el comportamiento del agente basta con editar el Markdown correspondiente y reiniciar la aplicación. Sin tocar código Java.

### Preguntas de prueba

```
# Estadísticas generales
¿Cuántos datos tiene el dataset?
Dame un resumen general del período

# Análisis por día
¿Cuál fue el día con más tiendas activas?
¿Los lunes tienen más actividad que los viernes?

# Patrones horarios
¿A qué hora hay más tiendas disponibles?
¿A qué hora debería tener más operadores disponibles?

# Fecha específica
Dame el resumen del 6 de febrero
¿Cómo estuvo el domingo 8 de febrero?

# Análisis profundo
Analiza los datos
¿Hay alguna anomalía en los datos?
¿Cómo se comparan los fines de semana con los días de semana?
Dame una recomendación de negocio basada en los datos
```

---

## Decisiones de Diseño

### Modelo local vs API cloud

Se eligió **Gemma 4 E2B via Ollama** en lugar de APIs cloud (Gemini, OpenAI) por tres razones:
- **Sin costo por llamada** — el modelo corre completamente local
- **Sin rate limiting** — no hay restricciones de requests por minuto
- **Sin dependencia externa** — funciona sin conexión a internet

### Cálculo en Java, no en el modelo

Los modelos pequeños como Gemma 4 E2B son poco confiables con aritmética. `InsightService` calcula toda la estadística en Java (tendencias, volatilidad, anomalías, comparaciones weekday/weekend) y entrega los resultados ya procesados al modelo. Gemma solo genera el lenguaje natural — no calcula.

### DTOs desacoplados por responsabilidad

Hay tres familias de DTOs con propósitos distintos:
- `dto/analytics/` — para el frontend REST, incluyen tipos Java ricos (`LocalDate`, `LocalDateTime`)
- `dto/chat/` — para el chatbot, simples strings de entrada/salida
- `dto/mcp/` — para el agente, compactos y serializados a JSON mínimo

### Caché a nivel de tools

Los datos del dataset no cambian después de la carga inicial. El caché vive en `StoreVisibilityMcpTools` (no en el service) porque es el punto de contacto con el agente. Esto mantiene `AnalyticsService` sin estado y reutilizable por los endpoints REST sin efectos colaterales.

### MCP Server como canal secundario

Además del chatbot integrado, el backend expone un **servidor MCP sobre SSE** en `/sse`. Esto permite que cualquier agente externo compatible con el protocolo MCP se conecte y use las mismas herramientas. El `ToolCallbackProvider` en `AppConfig` registra las tools para este canal, independiente del `ChatClient` que usa Gemma.

---

## Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/rappi` | URL de conexión a PostgreSQL |
| `DB_USER` | `postgres` | Usuario de la base de datos |
| `DB_PASSWORD` | `postgres` | Password de la base de datos |
| `DATA_ZIP_PATH` | `data/Archivo.zip` | Ruta al ZIP con los CSVs |
| `OLLAMA_URL` | `http://localhost:11434` | URL de Ollama |
| `OLLAMA_MODEL` | `gemma4:e2b` | Modelo a usar |
| `CORS_ORIGINS` | `http://localhost:5173,...` | Orígenes permitidos para CORS |

**EXTRA:** Estas decisiones reflejan un enfoque de ingeniería orientado a la fiabilidad del dato y la escalabilidad del sistema en entornos de alta demanda.
