# Requisitos de integración con Meta

> Documento de preparación. Los nombres, permisos y métricas deben confirmarse contra la versión de Graph API elegida y el App Dashboard antes de conectar producción.

## APIs y activos

- Marketing API / Ads Insights: portafolios accesibles, cuentas, campañas, conjuntos, anuncios, creativos, insights y reportes asíncronos.
- Page Graph API / Page Insights: páginas administradas, publicaciones e insights orgánicos disponibles.
- Instagram Graph API / Insights: cuenta profesional vinculada a una página y sus medios/insights permitidos.
- Creative endpoints: Page ID, Instagram actor/account ID, thumbnail e información necesaria para clasificar a nivel anuncio.

Activos mínimos: Meta App empresarial, negocio/portafolio SODI, usuario del sistema, cuentas publicitarias, páginas y cuentas profesionales de Instagram asignadas a ese usuario/app.

## Credenciales y permisos esperados

- App ID y App Secret (el secreto nunca se envía al navegador).
- Token de usuario del sistema o mecanismo aprobado para servidor, con expiración y activos asignados.
- Business ID y lista verificada de Ad Account IDs, Page IDs e Instagram Account IDs.
- Para anuncios, normalmente `ads_read`; `ads_management` no debe solicitarse si solo se lee.
- Para administración/descubrimiento empresarial, puede requerirse `business_management`.
- Para páginas, validar necesidades de `pages_show_list`, `pages_read_engagement` y permisos de insights aplicables.
- Para Instagram Insights, validar `instagram_basic` y `instagram_manage_insights`, además de la vinculación/roles requeridos.

Los permisos anteriores son hipótesis de diseño, no garantía. Deben confirmarse en App Review, Access Token Debugger y llamadas de prueba con la versión configurada.

## Tokens y seguridad

- Variables `META_APP_ID`, `META_APP_SECRET`, `META_ACCESS_TOKEN`, `META_BUSINESS_ID` y `META_GRAPH_API_VERSION`.
- Nunca persistir o registrar tokens sin cifrado/gestión de secretos; no incluirlos en query logs.
- Registrar solo fingerprint, tipo, propietario técnico, scopes detectados y vencimiento.
- Rotación documentada, mínimo privilegio y revocación inmediata ante incidente.

## Comportamiento del cliente

- Versión Graph en una propiedad central, no repetida.
- WebClient separado por Ads, Page, Instagram y Creative.
- Timeouts configurables; paginación por `after/before`; no construir URLs `next` sin validarlas.
- Reportes asíncronos para rangos grandes; polling acotado y recuperable.
- Reintentos solo para fallos transitorios/429/5xx, con backoff exponencial y jitter; respetar `Retry-After` cuando exista.
- No reintentar ciegamente autenticación, permisos o solicitudes inválidas.
- Persistir cursor, rango, cuenta, versión, breakdowns, atribución y estado.
- Upsert idempotente por claves naturales; payload original JSONB solo cuando aporte auditoría.

## Límites y datos no garantizados

- Rate limits dependen de app/cuenta/uso; inspeccionar encabezados de uso y reducir concurrencia.
- Algunas métricas, breakdowns y combinaciones no pueden pedirse juntas.
- Datos demográficos pueden omitirse por privacidad, tamaño mínimo o cambios de producto.
- Page/Instagram Insights y nombres de métricas cambian o expiran entre versiones.
- Alcance no es aditivo; ventanas de atribución y modelado pueden reajustar históricos.
- Page ID/Instagram ID puede requerir enriquecer anuncio/creative y no estar en todas las filas de Insights.
- WhatsApp/messaging depende del objetivo, destino, permisos y action types; nunca asumir ausencia de conversaciones por una sola pantalla.
- Publicaciones eliminadas o activos sin permiso pueden impedir recuperar creativos/insights.

## Datos pagados vs. orgánicos

- Pagado: spend, impresiones, alcance, frecuencia, clics, CPM/CPC/CTR, acciones y conversiones desde Ads Insights.
- Facebook orgánico: publicaciones, audiencia y Page Insights vigentes.
- Instagram orgánico: medios, audiencia y account/media insights vigentes.
- Cada hecho guarda `source`, `metric_name`, nivel, periodo y definición; no se combinan salvo una vista explícitamente etiquetada.

## Preguntas pendientes

1. ¿La app ya existe y tiene modo Business/verificación y revisión de permisos?
2. ¿Qué usuario del sistema tiene acceso a cada cuenta/página/Instagram?
3. ¿Qué IDs forman el piloto y cuáles cuentas son realmente exclusivas?
4. ¿Qué action types/ventanas son KPI contractual por cliente?
5. ¿Se requiere histórico anterior a la conexión y cómo se importará?
6. ¿Qué métricas orgánicas aparecen en una llamada de prueba hoy para Diamerad?
7. ¿Cuál es la retención permitida de payloads, creativos y demografía?

## Referencias oficiales a validar

- https://developers.facebook.com/docs/marketing-apis/
- https://developers.facebook.com/docs/marketing-api/insights/
- https://developers.facebook.com/docs/graph-api/overview/rate-limiting/
- https://developers.facebook.com/docs/facebook-login/guides/access-tokens/
- https://developers.facebook.com/docs/instagram-platform/instagram-api-with-facebook-login/insights/
