# SODI Meta Reports

Monolito modular interno para administrar clientes y activos que posteriormente alimentarán informes de Meta Ads. Incluye seguridad persistente, catálogo, sincronización simulada, clasificación, incidencias, métricas diarias, tasas mensuales y períodos de revisión; todavía no llama a Meta ni genera informes finales.

## Ejecución

Requiere JDK 21 y Docker Desktop. El Maven Wrapper fija Maven 3.9.11.

```bash
docker compose up -d db
./mvnw spring-boot:run
./mvnw clean test
```

En macOS ejecute el wrapper en el host para que Testcontainers use Docker Desktop. Testcontainers 2.0.5 soporta Docker Engine 29. Copie `.env.example` a `.env` y complete solo valores locales.

## Primer administrador

Defina `INITIAL_ADMIN_ENABLED=true` y el resto de `INITIAL_ADMIN_*`. Se crea un `SUPER_ADMIN` solo si no existe ningún usuario; la contraseña se cifra con BCrypt y no se registra. Desactive la bandera después del bootstrap.

La administración está en `/admin`, el login en `/login`, las ejecuciones simuladas en `/admin/synchronizations` y la salud en `/actuator/health`. Datos ficticios opt-in: perfil `dev` más `APP_DEMO_DATA_ENABLED=true`.

Flyway es la autoridad del esquema y JPA usa `validate`. PostgreSQL guarda UUID/JSONB; los logos quedan fuera de la base. Consulte [seguridad](docs/SECURITY_MODEL.md), [clientes y activos](docs/CLIENT_ASSET_MODEL.md), [sincronización simulada](docs/SIMULATED_SYNCHRONIZATION.md), [arquitectura](docs/ARCHITECTURE.md) y [diccionario](docs/DATA_DICTIONARY.md).
