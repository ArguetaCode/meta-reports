# Sincronización simulada y clasificación

## Alcance de fase 3

La aplicación procesa fixtures JSON locales mediante el puerto `MetaDataSource`. `ClasspathFixtureMetaDataSource` es la única implementación habilitada: no usa red, tokens ni endpoints Meta. Cada fixture contiene páginas con cursores opacos y anuncios con su jerarquía campaña/conjunto.

`SynchronizationService` crea una ejecución con correlation ID y delega cada página a `SyncPageProcessor`, que abre una transacción corta, normaliza IDs y ejecuta upserts PostgreSQL. Repetir el mismo fixture crea una nueva ejecución auditable, pero no duplica campañas, conjuntos, anuncios ni su clasificación vigente.

## Clasificación

Prioridad:

1. Página de Facebook asociada actualmente.
2. Cuenta de Instagram asociada actualmente.
3. Cuenta publicitaria exclusiva.
4. El prefijo solo valida; nunca asigna automáticamente por sí solo.

Si señales fuertes o el prefijo contradicen al propietario seleccionado se conserva la señal prioritaria y se abre `CONFLICTING_SIGNALS`. Solo prefijo produce `PREFIX_ONLY`; prefijo desconocido sin señal fuerte produce `UNKNOWN_PREFIX`; sin señales produce `UNCLASSIFIED`.

## Ejecución

Desde `/admin/synchronizations`, un usuario con `SYNC_EXECUTE` puede procesar `phase3-demo`. Los resultados aparecen en la misma pantalla y las incidencias en `/admin/incidents`. El fixture espera los IDs ficticios documentados dentro del propio JSON; primero deben existir en el catálogo.

Las tablas creadas por V8 son `sync_execution`, `meta_campaign`, `meta_ad_set`, `meta_ad`, `ad_classification` y `classification_incident`. El payload original se conserva en JSONB sin secretos.

## Resolución y reprocesamiento

La migración V9 agrega `manual_ad_assignment` e `incident_resolution_history`. Un usuario con `INCIDENT_RESOLVE` puede resolver asignando un cliente activo, ignorar con motivo o reprocesar un anuncio individual. Toda acción exige una nota, conserva actor y fecha y escribe en la auditoría central.

Una asignación manual tiene prioridad sobre las señales automáticas y persiste entre sincronizaciones. El reprocesamiento vuelve a evaluar únicamente el anuncio de la incidencia; si las señales ya son suficientes, cierra la incidencia, y si no lo son la mantiene abierta dejando constancia del intento.

## Pendiente

Resolución administrativa de incidencias, métricas diarias, reanudación de ejecuciones fallidas, reportes asíncronos, cliente HTTP real y políticas de retención corresponden a fases posteriores.
