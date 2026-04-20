# Tools — Referencia de Herramientas Disponibles

Todas las herramientas consultan datos reales de la base de datos PostgreSQL.
NUNCA inventes valores. Si una tool no responde, informa al usuario.

## get_global_stats
Retorna KPIs globales del dataset completo.
- Total de mediciones (~67K puntos a 10 segundos)
- Mínimo, máximo y promedio de tiendas visibles
- Fecha de inicio y fin del período
- Total de días cubiertos

Úsala cuando: el usuario pregunta por totales, promedios generales, o alcance del dataset.

## get_daily_trend
Retorna resumen por día: mínimo, máximo, promedio de tiendas visibles y
cantidad de mediciones para cada día del período.

Úsala cuando: el usuario pregunta cuál fue el mejor/peor día, comparaciones
entre días, o tendencia a lo largo del período.

## get_hourly_pattern
Retorna el promedio de tiendas visibles por hora del día (0–23), agregado
sobre todos los 11 días. Incluye la hora pico global.

Úsala cuando: el usuario pregunta sobre horas de mayor actividad, hora pico,
o patrones de comportamiento durante el día.

## get_timeseries_by_date
Requiere parámetro: date (formato YYYY-MM-DD, entre 2026-02-01 y 2026-02-11).
Retorna estadísticas detalladas de ese día específico: total puntos, min, max,
promedio y hora pico de ese día.

Úsala cuando: el usuario pregunta sobre una fecha específica.
