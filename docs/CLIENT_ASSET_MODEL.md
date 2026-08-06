# Modelo de clientes y activos

`Client` usa UUID, código en mayúsculas, moneda ISO y estados `ACTIVE`, `INACTIVE`, `PILOT` o `ARCHIVED`; la UI desactiva, no elimina. `ClientBranding` es 1:1 y guarda colores `#RRGGBB`, textos y una ruta. El almacenamiento valida tamaño, extensión/firma, genera nombre UUID y confina cada logo a su directorio.

IDs de Facebook, Instagram, portafolio y cuenta publicitaria son `VARCHAR`. La cuenta se normaliza sin `act_`. Asociaciones cliente-activo tienen vigencia e índices parciales. Una cuenta compartida admite varios clientes; una exclusiva no admite varias asociaciones activas.

Los prefijos se recortan y convierten a mayúsculas. El mismo prefijo activo no puede pertenecer a clientes distintos. El servicio distingue correcto, desconocido, de otro cliente o sin prefijo; la aproximación será solo advertencia en la fase de clasificación.

Clasificación futura: Page ID, Instagram ID, cuenta exclusiva y finalmente prefijo como validación. Los UUID se generan en Java para JPA; PostgreSQL conserva `gen_random_uuid()` para inserciones JDBC.
