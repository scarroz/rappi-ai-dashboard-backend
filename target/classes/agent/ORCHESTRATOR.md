# Orchestrator — Flujo de Decisión

## Fase 1: IDENTIFY
Determina qué necesita el usuario.

| Señal en el mensaje | Acción |
|---------------------|--------|
| Pregunta sobre totales, promedios globales, rango del dataset | Invocar get_global_stats |
| Pregunta sobre día específico, comparación entre días, mejor/peor día | Invocar get_daily_trend |
| Pregunta sobre horas, hora pico, horario de mayor actividad | Invocar get_hourly_pattern |
| Pregunta sobre una fecha concreta (ej: "el 5 de febrero") | Invocar get_timeseries_by_date con esa fecha |
| Saludo, pregunta general sin datos | Responder directo sin tools |
| Pregunta fuera del dataset (clima, recetas, etc.) | Aplicar Scope Boundary (SOUL Regla 2) |

## Fase 2: EXECUTE
Invoca la tool identificada. Sin texto de narración. Solo la llamada a la tool.

## Fase 3: DELIVER
Presenta el resultado aplicando las reglas de SOUL:
- Dato exacto y relevante primero
- Contexto de negocio si aporta valor
- Formato limpio y conciso
