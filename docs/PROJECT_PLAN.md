# Plan del proyecto SODI Meta Reports

> Estado al 2026-08-03: fase 2 implementada con usuarios/RBAC persistentes, bloqueo y bootstrap seguro, clientes, branding, activos e historiales, prefijos, auditoría, UI administrativa y migraciones V2–V7. PostgreSQL/Testcontainers se ejecuta realmente con el Maven Wrapper. Meta, clasificación completa e informes permanecen en fases posteriores.

## Contexto y problema

SODI Consultores administra aproximadamente 70 clientes distribuidos entre cuentas publicitarias exclusivas y compartidas. En una cuenta compartida, una campaña incluso puede contener anuncios de clientes distintos. El proceso mensual actual exporta Meta Ads a Excel, clasifica por prefijo, calcula indicadores, toma capturas de Page Insights y arma manualmente un PDF en Canva.

El repositorio estaba vacío al iniciar esta fase. Las referencias revisadas fueron el Excel de julio de 2026 (dos hojas, 155 filas de detalle) y el PDF Diamerad de mayo (18 páginas), además de la captura del portafolio SODI Consultores. La captura confirma cinco cuentas visibles: Clientes Sodi, Clientes Sodi 2, SODI DIAMERAD, SODI IMPOCAR y SODI Multiproyectos CGR; esto no demuestra que sean las únicas ni que todas sean accesibles por API.

## Objetivos

- Clasificar cada anuncio por Page ID, Instagram Account ID y, solo como respaldo, cuenta exclusiva y prefijo.
- Conservar métricas pagadas, orgánicas y acciones sin mezclarlas.
- Mantener histórico auditable, sincronización idempotente e incidencias resolubles.
- Generar Excel y PDF reproducibles, sin capturas manuales ni afirmaciones no sustentadas.
- Proteger credenciales, activos y archivos internos.

## Alcance del MVP

Piloto con una cuenta publicitaria y entre uno y tres clientes ficticios o anonimizados: autenticación administrativa, catálogo de clientes y activos, fixtures Meta, sincronización simulada paginada, clasificación a nivel anuncio, incidencias, métricas diarias, tipo de cambio mensual, vista de revisión, Excel/PDF básicos, historial, auditoría y pruebas PostgreSQL. La conexión real solo se activa después de validar permisos, modelo y fixtures.

## Fuera de alcance

Envíos automáticos, creación o edición de campañas, presupuestos, facturación, portal público, app móvil, microservicios, Kubernetes e integraciones ajenas a Meta.

## Proceso manual resumido

1. Seleccionar periodo y exportar campañas de Ads Manager.
2. Asignar empresa usando el prefijo del nombre.
3. Convertir USD a GTQ (en el archivo se usa implícitamente 8.00).
4. Crear tabla dinámica y seleccionar datos por cliente.
5. Consultar Page Insights, elegir publicaciones y capturar gráficas.
6. Copiar cifras/capturas a Canva, añadir textos promocionales y exportar PDF.

## Arquitectura y módulos

Monolito modular Spring Boot con módulos de `auth`, `user`, `role`, `client`, `branding`, `metaaccount`, `metaintegration`, `synchronization`, `classification`, `campaign`, `adset`, `ad`, `insight`, `organic`, `metric`, `exchangerate`, `report`, `excel`, `pdf`, `filemanagement`, `incident`, `audit`, `scheduling`, `configuration` y `shared`. Detalle en `ARCHITECTURE.md`.

## Modelo preliminar

El modelo separa identidad interna (UUID o bigint) de identificadores Meta (`VARCHAR`). El núcleo propuesto es:

- `Client` 1:1 `ClientBranding`; N:M `FacebookPage`, `InstagramAccount` y `MetaAdAccount`; 1:N `CampaignPrefix`.
- `MetaBusinessPortfolio` 1:N `MetaAdAccount`; cuenta 1:N campañas; campaña 1:N conjuntos; conjunto 1:N anuncios; anuncio 1:1/N creativos según la respuesta real.
- `MetaAd` 1:N `DailyAdInsight`; cada insight 1:N `InsightAction` para no colapsar tipos de resultado.
- Activos orgánicos 1:N insights diarios y publicaciones.
- `SyncExecution` 1:N errores; `ClassificationIncident` referencia anuncio, señales detectadas, resolución y auditoría.
- `ExchangeRate` es único por fecha, moneda origen, destino y fuente.
- `Report` referencia cliente y periodo; 1:N `ReportFile`, con snapshot de configuración y métricas.

No se crean aún tablas de dominio: requieren aprobación y fixtures representativos.

### Índices y restricciones prioritarios

- Unicidad por `meta_id` dentro del ámbito correcto; todos los IDs Meta como `VARCHAR` no vacío.
- Unicidad de Page ID e Instagram Account ID; si se admite transferencia histórica, vigencia temporal sin solapamientos.
- Unicidad de insight por anuncio, fecha, nivel, ventana y fuente.
- Unicidad de acción por insight, tipo, atribución y ventana.
- Índices por cliente/estado, cuenta/periodo, anuncio/fecha, ejecución/estado y incidencia/estado.
- Checks para importes no negativos, monedas ISO de tres letras, periodos válidos y estados enumerados.
- FKs obligatorias; borrado restringido para histórico y desactivación lógica de catálogos.

## Flujos

### Sincronización

Crear ejecución -> validar activos y rango -> solicitar reporte simulado/asíncrono -> paginar -> guardar payload original -> normalizar IDs y acciones -> upsert idempotente -> clasificar anuncios -> calcular indicadores derivados -> registrar incidencias/errores -> cerrar ejecución con contadores. Un reintento continúa desde cursor o reinicia con upserts seguros.

### Clasificación

1. Coincidencia de Page ID.
2. Coincidencia de Instagram Account ID.
3. Cuenta publicitaria marcada como exclusiva.
4. Prefijo solo valida o propone.
5. Sin señal inequívoca: pendiente manual.

Una contradicción no cambia el dueño determinado por el activo real; abre una incidencia. La resolución manual registra usuario, observación, instante y relación reutilizable.

### Informes

Congelar periodo y configuración -> comprobar sincronización completa -> mostrar métricas por fuente/tipo -> resolver o aceptar incidencias -> generar snapshot -> producir Excel y PDF desde el mismo dataset -> validar totales y archivos -> registrar checksum/ruta -> habilitar descarga manual.

## Fases

1. Descubrimiento y base técnica (esta entrega).
2. Modelo aprobado, seguridad, usuarios y catálogo.
3. Fixtures, normalización, sincronización simulada y clasificación.
4. Métricas, tipo de cambio e incidencias.
5. Excel/PDF básicos con validación de Diamerad.
6. Preparación y prueba controlada de API real.
7. Piloto, observabilidad, respaldo y endurecimiento productivo.

## Riesgos

- Permisos, revisión de aplicación, propiedad de activos y token de usuario del sistema.
- Métricas retiradas, renombradas o no disponibles por privacidad/tamaño de audiencia.
- Identidad Page/Instagram no presente en todos los campos de Insights y necesidad de consultar creativo/anuncio.
- Alcance no aditivo y ventanas de atribución que cambian resultados históricos.
- Errores humanos en nombres y relaciones de activos.
- Datos personales/demográficos y retención de payloads.
- Tokens caducados, rate limits, reportes asíncronos y sincronizaciones parciales.
- Calidad del tipo de cambio y diferencias entre fecha de gasto, cierre y reporte.

## Criterios de aceptación de la fase 1

- Referencias y errores documentados.
- Arquitectura, modelo, fases y preguntas pendientes descritos.
- Proyecto Java 21/Spring Boot 3.x compilable con PostgreSQL/Flyway.
- Actuator, OpenAPI, perfiles, Docker y Compose configurados sin secretos.
- Migración mínima únicamente de conectividad.
- Prueba Testcontainers de PostgreSQL y prueba de protección de rutas presentes.
- Sin llamadas Meta reales ni tablas definitivas.

## Preguntas pendientes

- ¿Quién posee la app Meta, el portafolio y cada activo, y qué usuarios del sistema existen?
- ¿Qué cuentas, páginas e Instagram IDs formarán el piloto?
- ¿Qué resultado contractual debe reportarse por cliente y con qué ventana de atribución?
- ¿El tipo de cambio será compra, venta o referencia y cuál será su fuente/fecha?
- ¿El informe mensual debe ser calendario exacto o últimos 28/30 días?
- ¿Cuál es la política de retención, respaldo y acceso a informes/payloads?
- ¿Qué textos del PDF son obligatorios y cuáles pueden eliminarse por falta de evidencia?
