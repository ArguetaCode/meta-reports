# ADR 0002: identificadores, historial y logos

Estado: aceptado — 2026-08-03.

Se usan UUID internos e IDs Meta como cadenas. Hibernate genera UUID para agregados JPA y PostgreSQL declara `gen_random_uuid()` para inserciones JDBC. Las asociaciones cliente-activo conservan vigencia e índices parciales para impedir solapamientos inválidos. Los logos usan la interfaz `LogoStorage` y la base almacena solo la ruta.

Esto evita secuencias y conversiones de IDs externos, conserva transferencias históricas y permite sustituir almacenamiento local por object storage sin cambiar el dominio.
