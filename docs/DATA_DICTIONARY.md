# Diccionario de datos preliminar

> Fase 2 ejecutable en V2–V7: `app_user`, roles/permisos, `client`, branding, activos Meta, asociaciones históricas, prefijos y `audit_log`. UUID es identidad interna, IDs Meta son `VARCHAR` y snapshots son `JSONB`. Las migraciones prevalecen sobre propuestas preliminares de este documento.

## Excel revisado

El libro contiene `Worksheet` (detalle exportado/enriquecido) y `Hoja1` (tabla dinámica). En la hoja detalle se observaron 19 columnas físicas, con columnas vacías intermedias y valores desplazados en las últimas columnas. El total dinámico muestra USD 3,100.00 y GTQ 24,800.00, lo que implica un tipo de cambio fijo de 8.00 sin fuente ni fecha.

| Nombre actual | Nombre propuesto | Tipo | Fuente | Regla/unidad | Observaciones |
|---|---|---|---|---|---|
| Nombre de la campaña | `campaign_name` | VARCHAR | Marketing API/exportación | Texto | No identifica al cliente; contiene errores ortográficos. |
| Empresa | `client_assignment` | Relación | Manual | Cliente | No viene de Meta y parece derivada del prefijo. |
| Resultados | No usar como único campo | Acción tipada | Ads Insights `actions` | Conteo por `action_type` | Mezcla reproducciones, mensajes u otras acciones; no es sumable entre tipos. |
| Alcance | `reach` | BIGINT | Ads/Page/IG Insights | Personas/cuentas estimadas | No es aditivo entre campañas; fuente y nivel obligatorios. |
| Impresiones | `impressions` | BIGINT | Ads/Page/IG Insights | Conteo | Sí admite suma bajo dimensiones compatibles. |
| Costo por resultados | `cost_per_action` derivado | NUMERIC(19,6) | Cálculo | moneda/acción | `spend / acciones del mismo tipo`; no sumar costos individuales. |
| Importe gastado (USD) | `spend_original` | NUMERIC(19,6) | Ads Insights | USD | Moneda debe venir de la cuenta. |
| Importe gastado Q | `spend_converted` | NUMERIC(19,6) | Cálculo | GTQ | Guardar tasa, fecha y fuente; el archivo usa 8.00 implícito. |
| Presupuesto del conjunto de anuncios | `adset_budget` | NUMERIC(19,6) | Marketing API/exportación | moneda/configuración | No equivale al gasto; aclarar diario o total. |
| Contactos de mensajes totales | `messaging_contacts` | BIGINT/acción | Ads Insights | contactos | Confirmar `action_type` exacto y atribución. |
| Nuevos contactos de mensajes | `new_messaging_contacts` | BIGINT/acción | Ads Insights | contactos | Disponibilidad/definición depende de producto y API. |
| Compras | `purchases` | BIGINT/acción | Ads Insights | compras atribuidas | Requiere tipo de acción y ventana. |
| Finalización | `campaign_or_adset_end_date` | DATE | Marketing API/exportación | fecha | El encabezado no identifica el nivel. |
| Costo por compra (USD) | `cost_per_purchase` | NUMERIC(19,6) | Cálculo/Ads | USD/compra | En el libro aparecen valores aislados y aparentemente desplazados. |
| Interacciones con la publicación | `post_engagements` | BIGINT/acción | Ads Insights | interacciones | Muchos valores parecen corridos desde otras filas/columnas; requiere reexportación. |

## Métricas del PDF

| Rótulo | Nombre técnico propuesto | Fuente/ámbito | Observación |
|---|---|---|---|
| Alcance total 48,355 | `reach` | Ambiguo: orgánico + pagado o pagado | El propio PDF dice distribución orgánica o pagada; luego lo reutiliza como alcance pautado. |
| Personas que interactuaron 1,383 | `page_engaged_users` o métrica equivalente vigente | Page Insights | Debe registrar nombre exacto y periodo. |
| Seguidores incrementados 46 | `follows_net`/crecimiento | Page Insights | Falta base inicial/final y definición neta/bruta. |
| Visualizaciones de publicaciones | `post_views`/video views | Page/Post Insights | Las capturas no permiten identificar consistentemente la métrica. |
| Edad y sexo | `audience_demographics` | Page/Instagram Insights | Puede tener umbrales y disponibilidad limitada. |
| Ciudades | `audience_city` | Page/Instagram Insights | Dato orgánico/audiencia; sujeto a privacidad. |
| Impresiones 92,612 | `impressions` | Pagado según cierre, pero no etiquetado | Consistente con frecuencia 1.91 si alcance es 48,355. |
| Frecuencia 1.91 | `frequency` | Pagado | `92,612 / 48,355 = 1.915`; válido si mismo nivel/periodo/fuente. |
| Inversión Q790.96 | `spend_converted` | Pagado | Falta USD, tasa, fecha y fuente. |
| “Visualización por usuario Q0.0085” | `cost_per_impression` | Derivado pagado | `790.96 / 92,612 = Q0.00854`; no es costo por usuario. |

## Problemas y ambigüedades verificadas

- La tabla dinámica suma `Costo por resultados` (total 185.9745), operación matemáticamente inválida.
- La tabla dinámica suma alcance (1,704,330) entre campañas y no puede presentarlo como usuarios únicos.
- `Resultados` mezcla tipos: ejemplos con decenas de miles parecen reproducciones/interacciones, mientras otros coinciden con contactos.
- Filas sin resultados se convierten a cero en la tabla dinámica; cero y no disponible no son equivalentes.
- Columnas vacías y datos desplazados: `Costo por compra` e `Interacciones` contienen valores que coinciden con campos de otras campañas.
- La hoja dinámica renderiza importes GTQ como fechas por formato incorrecto.
- El PDF mezcla “últimos 28 días”, “todo el mes” y “mayo” sin rango exacto.
- El PDF contiene “Objetivos de septiembre” dentro de un informe de mayo.
- El dato contextual sobre usuarios de Facebook en Honduras carece de fuente y fecha.
- El PDF afirma resultados “increíbles” y causalidad inversión-resultados sin umbral o comparación.
- Las capturas manuales no conservan definición API, filtros, fecha de extracción ni auditabilidad.

## Reglas de cálculo propuestas

- Frecuencia: `impressions / reach`, solo con alcance mayor a cero y mismas dimensiones.
- CPM: `spend / impressions * 1000`.
- CPC: `spend / clicks`.
- CTR: `clicks / impressions * 100`.
- Costo por impresión: `spend / impressions`.
- Costo por resultado: `sum(spend) / sum(action_count)` agrupado por tipo/atribución/ventana.
- Valor no calculable: `NULL`, presentado como “No disponible”, nunca como cero.
