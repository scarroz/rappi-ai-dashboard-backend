# rappi-ai-dashboard-backend
AI-powered analytics backend for store availability insights.  This service processes historical store status data and exposes advanced analytics through REST APIs, including time series, trends, and global metrics. It also integrates an AI-powered chatbot using MCP tools to enable natural language queries over the data.

## Overview

Este backend es el motor analítico encargado de procesar eventos históricos de disponibilidad de tiendas. La aplicación expone insights a través de una arquitectura de APIs REST y un motor de chat potenciado por inteligencia artificial para la interpretación semántica de datos.

El sistema permite:
* Análisis de series temporales.
* Detección de tendencias.
* Cálculo de métricas globales de disponibilidad.
* Consultas en lenguaje natural mediante IA.

## Arquitectura

* **Framework:** Spring Boot.
* **Diseño:** Arquitectura por capas (Controller, Service, Repository).
* **Modelado:** DTOs implementados mediante Java Records.
* **Interacción IA:** Integración de herramientas MCP (Model Context Protocol).

## Integración con IA

El chatbot opera mediante herramientas estructuradas (MCP) que permiten al modelo realizar las siguientes acciones:
* Recuperar estadísticas globales con precisión técnica.
* Analizar tendencias temporales basadas en datos reales.
* Detectar patrones horarios de comportamiento.
* Responder consultas semánticas sobre el dataset sin generar alucinaciones.

## Endpoints Disponibles

### Analytics
* `/analytics/global`
* `/analytics/time-series`
* `/analytics/daily-summary`
* `/analytics/hourly-pattern`

### Chat
* `/chat`

## Origen de Datos

El sistema utiliza un dataset en formato CSV que contiene el registro de cambios de disponibilidad de tiendas (eventos online/offline).

## Ejecución Local

### Comandos de inicio
```bash
mvn spring-boot:run
Acceso al servicio
El servicio estará disponible en: http://localhost:8081
```
---
## Decisiones Técnicas Clave

El diseño del sistema se basó en tres pilares fundamentales para garantizar la robustez y la precisión de la inteligencia artificial aplicada:

### 1. Implementación de MCP (Model Context Protocol)
En lugar de depender exclusivamente de *raw prompting* (instrucciones en texto plano), se optó por el uso de **herramientas estructuradas (MCP)**. Esta decisión permite:
* **Control riguroso:** Define límites claros sobre qué puede y qué no puede hacer el modelo de IA.
* **Exactitud determinista:** Asegura que la IA acceda a cálculos precisos realizados por el código Java en lugar de intentar realizar operaciones matemáticas complejas por cuenta propia.
* **Reducción de alucinaciones:** Al forzar al modelo a usar herramientas específicas, se minimiza la generación de datos falsos.

### 2. DTOs mediante Java Records
Para la transferencia de datos entre capas y hacia el frontend, se implementaron **Java Records**:
* **Inmutabilidad:** Garantiza que los datos no sean alterados una vez procesados.
* **Eficiencia de memoria:** Ideal para manejar grandes volúmenes de datos analíticos (datasets de disponibilidad) con un footprint de memoria reducido.
* **Legibilidad:** Reduce el código repetitivo (*boilerplate*), facilitando el mantenimiento.

### 3. Separación de Responsabilidades (SoC)
Se aplicó un desacoplamiento estricto entre la **lógica analítica** (cálculo de uptime, patrones y tendencias) y la **lógica conversacional** (IA):
* **Escalabilidad independiente:** Permite optimizar o migrar el motor de analítica sin afectar la interfaz de chat.
* **Mantenibilidad:** Facilita la identificación de errores y la implementación de pruebas unitarias específicas para cada módulo.

---

## Mejoras Futuras (Roadmap)

El proyecto tiene trazada una ruta de evolución técnica para escalar de un entorno de prueba a uno de producción masiva:

### Fase 1: Optimización de Rendimiento
* **Capa de Caché (Redis):** Implementación de una memoria intermedia para acelerar la respuesta de consultas analíticas recurrentes, reduciendo la carga sobre el motor de procesamiento principal.

### Fase 2: Procesamiento en Tiempo Real
* **Streaming de Datos:** Integración con **Apache Kafka** o tecnologías similares para procesar eventos de disponibilidad en tiempo real, pasando de un análisis de datos históricos a uno predictivo inmediato.

### Fase 3: Especialización de IA
* **Fine-Tuning de Modelos:** Entrenamiento y refinamiento de modelos de lenguaje (LLMs) específicos para el dominio de logística, última milla y métricas operativas, mejorando la comprensión semántica de las consultas del usuario.

---

**EXTRA:** Estas decisiones reflejan un enfoque de ingeniería orientado a la fiabilidad del dato y la escalabilidad del sistema en entornos de alta demanda.
