# Contexto de traspaso para Codex

Actualizado: 2026-08-06, después de implementar la fase 3. Zona horaria: America/Guatemala.

## Ubicación y Git

- Repositorio local: `/Users/sodi/proyectos/meta-reports`
- Repositorio remoto: `https://github.com/ArguetaCode/meta-reports.git`
- Rama: `main`
- Commit verificado: `b309c6e Initial commit`
- `main` está sincronizada con `origin/main` y el workspace estaba limpio al elaborar este documento.

## Propósito

Sistema interno de SODI Consultores para automatizar progresivamente la clasificación de actividad de Meta Ads y generar informes por cliente. Es un monolito modular. La fase 2 construyó la base administrativa persistente; todavía no se deben realizar llamadas reales a Meta ni implementar WhatsApp o la generación completa de Excel/PDF.

Referencias originales del análisis: informe estadístico PDF de Diamerad, hoja de costos/informes de Facebook y captura del portafolio de Meta Ads. Esos archivos externos no forman parte necesariamente del repositorio; el conocimiento derivado está documentado en `docs/`.

## Stack obligatorio

- Java 21
- Spring Boot 3.5.7
- Maven Wrapper 3.9.11
- PostgreSQL 16
- Flyway
- Spring Data JPA/Hibernate
- Spring Security con sesiones
- Thymeleaf y Bootstrap 5
- Testcontainers 2.0.5
- Docker y Docker Compose

Testcontainers 2.0.5 fue elegido porque Docker Desktop 29 exige API mínima 1.44; la versión anterior 1.21.3 fallaba al consultar el daemon.

## Estado implementado

### Seguridad

- Usuarios persistentes en `app_user`, UUID, `@Version`, username/email únicos y normalizados.
- Roles: `SUPER_ADMIN`, `ADMIN`, `ANALYST`, `READ_ONLY`.
- Veinte permisos persistidos y relaciones normalizadas `user_role`/`role_permission`.
- Login por formulario, logout, CSRF, acceso denegado y protección de `/admin/**`.
- `PersistentUserDetailsService` carga roles y permisos desde PostgreSQL.
- BCrypt con costo configurable.
- Intentos fallidos, `locked_until`, bloqueo persistente, último login y desbloqueo administrativo.
- Bootstrap opcional del primer superadministrador mediante `INITIAL_ADMIN_*`; solo crea si no existe ningún usuario y nunca registra la contraseña.

### Clientes y activos

- `Client` con código normalizado, moneda ISO, zona horaria, estados y desactivación lógica.
- `ClientBranding` separado: colores, textos y ruta de logo.
- `LogoStorage`/`LocalLogoStorage`: binarios fuera de PostgreSQL, nombre UUID, validación de extensión/firma/tamaño y protección de rutas.
- Páginas Facebook, cuentas Instagram, portafolios y cuentas publicitarias.
- IDs Meta como `VARCHAR`; cuentas publicitarias se guardan sin `act_`.
- Asociaciones históricas cliente-activo mediante `active_from`/`active_until` e índices parciales.
- Prefijos normalizados y validador para correcto, desconocido, de otro cliente o campaña sin prefijo.
- Datos ficticios opt-in únicamente con perfil `dev` y `APP_DEMO_DATA_ENABLED=true`.

### Auditoría y UI

- `AuditService` centralizado con `audit_log`, `previous_data`/`new_data` JSONB.
- Dashboard y vistas Thymeleaf para login, usuarios, clientes, branding, activos y prefijos.
- Listado global básico de activos.
- Errores 404, conflictos y errores internos sin mostrar stack trace al usuario.

## Migraciones

- `V1__create_schema_marker.sql`
- `V2__create_security_tables.sql`
- `V3__seed_roles_and_permissions.sql`
- `V4__create_client_tables.sql`
- `V5__create_meta_asset_tables.sql`
- `V6__create_campaign_prefix_tables.sql`
- `V7__create_audit_tables.sql`

No modificar una migración ya aplicada en entornos compartidos. Crear V8+ para cualquier cambio posterior. Hibernate usa `ddl-auto=validate`; Flyway es la autoridad del esquema.

## Comandos verificados

```bash
cd /Users/sodi/proyectos/meta-reports
docker compose up -d db
./mvnw clean test
./mvnw spring-boot:run
```

Última suite registrada: 5 pruebas, 0 fallos, 0 errores, 0 omitidas. La prueba de integración levantó PostgreSQL 16 real con Testcontainers y verificó Flyway, roles, permisos y JSONB. `docker compose config -q` también fue aprobado.

## Configuración inicial

Copiar `.env.example` a `.env`. Para el primer usuario definir temporalmente:

```text
INITIAL_ADMIN_ENABLED=true
INITIAL_ADMIN_USERNAME=...
INITIAL_ADMIN_EMAIL=...
INITIAL_ADMIN_PASSWORD=...
INITIAL_ADMIN_FIRST_NAME=...
INITIAL_ADMIN_LAST_NAME=...
```

Arrancar una vez, confirmar el usuario y cambiar `INITIAL_ADMIN_ENABLED=false`. No versionar `.env`, tokens, contraseñas ni datos reales.

## Decisiones importantes

- Monolito modular, no microservicios.
- Sesiones, no JWT en esta etapa.
- UUID internos; IDs Meta siempre como texto.
- Asociaciones con vigencia para no destruir historial.
- Logos detrás de una interfaz de almacenamiento; PostgreSQL solo conserva referencias.
- Page ID e Instagram ID serán señales primarias de clasificación; cuenta exclusiva después; prefijo solo señal secundaria.
- Auditoría mediante servicio, no lógica repetida en controllers.
- No usar H2: pruebas de persistencia con PostgreSQL real.

## Riesgos y deuda conocida

- La cobertura actual es pequeña frente a la matriz solicitada: faltan pruebas exhaustivas de login real, bloqueo, bootstrap, restricciones de asociaciones, uploads y vistas.
- La UI administrativa es funcional pero básica. Faltan flujos completos de edición, desasociación con cierre de vigencia y administración avanzada de roles.
- `AssetService` usa JDBC y `Client`/usuarios usan JPA; conservar separación de responsabilidades y no duplicar reglas.
- Revisar y reforzar la regla de exclusividad de cuentas publicitarias cuando se implemente asociación de activos ya existentes.
- En producción HTTPS habilitar cookie de sesión `secure` y desactivar Swagger si no es necesario.
- La asignación de usuarios a clientes/tenants todavía no existe. No asumir aislamiento por cliente hasta diseñarlo explícitamente.
- Antes de fase 3 conviene hacer una prueba manual renderizada de todas las vistas Thymeleaf.

## Fase 3 implementada

V8 incorpora `sync_execution`, campaña/conjunto/anuncio, clasificación vigente e incidencias. `phase3-demo.json` se consume mediante `MetaDataSource`, procesa dos páginas con transacciones cortas y usa upserts idempotentes. La clasificación prioriza Page, Instagram y cuenta exclusiva; el prefijo no asigna por sí solo. Las pantallas están en `/admin/synchronizations` y `/admin/incidents`. Consulte `docs/SIMULATED_SYNCHRONIZATION.md`.

## Fase 4 implementada

V9 incorpora `manual_ad_assignment` e `incident_resolution_history`. `IncidentService` permite resolver con un cliente activo, ignorar con motivo y reprocesar un anuncio. La asignación manual tiene prioridad y no se pierde en sincronizaciones posteriores. Todas las acciones requieren `INCIDENT_RESOLVE` y generan historial y auditoría.

## Métricas y períodos implementados

V10 incorpora `daily_ad_insight`, `insight_action`, `exchange_rate` y `report_period`. El fixture `phase5-metrics.json` es idempotente. `/admin/metrics` permite importar, registrar tasas y crear períodos mensuales; `/admin/reports/{id}/review` consolida únicamente anuncios clasificados para el cliente y mantiene separado el gasto original del convertido.

## Próxima fase recomendada

Fase 4: métricas pagadas tipadas, acciones, tipo de cambio e inicio del flujo de resolución de incidencias. No conectar la API real hasta validar permisos, campos y fixtures.

Orden sugerido:

1. Ejecutar `./mvnw clean test` y revisar documentación.
2. Aumentar pruebas de fase 2 antes de ampliar el dominio.
3. Definir fixtures JSON anonimizados y contratos de normalización.
4. Implementar ejecución de sincronización simulada, paginación y upserts.
5. Clasificar por Page ID, Instagram ID, cuenta exclusiva y prefijo.
6. Registrar contradicciones o faltantes como incidencias, sin correcciones destructivas.

## Documentos que deben leerse primero

1. `README.md`
2. `docs/PROJECT_PLAN.md`
3. `docs/ARCHITECTURE.md`
4. `docs/SECURITY_MODEL.md`
5. `docs/CLIENT_ASSET_MODEL.md`
6. `docs/DATA_DICTIONARY.md`
7. `docs/META_INTEGRATION_REQUIREMENTS.md`
8. `docs/adr/0001-modular-monolith.md`
9. `docs/adr/0002-identifiers-history-and-logo-storage.md`

## Prompt listo para la otra cuenta

```text
Continúa el proyecto ubicado en /Users/sodi/proyectos/meta-reports (o clona https://github.com/ArguetaCode/meta-reports.git). Lee completamente docs/CODEX_HANDOFF.md y los documentos que allí se enumeran. Inspecciona el código y ejecuta ./mvnw clean test antes de modificarlo. Respeta Java 21, Spring Boot 3.5.x, PostgreSQL 16, Flyway, sesiones Spring Security, Thymeleaf/Bootstrap, Testcontainers y el monolito modular. No modifiques migraciones V1–V7 ya aplicadas: usa V8+. No uses H2, no expongas secretos y no realices llamadas reales a Meta, WhatsApp ni generación final de informes salvo que la nueva solicitud lo autorice expresamente. Conserva UUID internos, IDs Meta como String, asociaciones históricas y auditoría centralizada. Informa el estado encontrado y propone cambios mínimos compatibles antes de implementarlos.
```
