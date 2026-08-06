# Modelo de seguridad

## Autenticación

Spring Security autentica contra `app_user` mediante formulario y sesión; no usa JWT. CSRF está activo, logout invalida la sesión y las cookies son HttpOnly/SameSite=Lax. Producción sobre HTTPS debe habilitar la bandera `secure` de cookie.

Las contraseñas usan BCrypt con costo configurable y longitud administrativa mínima de 12. DTO, vistas, auditoría y logs no exponen contraseña ni hash. Los fallos incrementan un contador persistente; al alcanzar el máximo, `account_locked` y `locked_until` sobreviven reinicios. El éxito limpia el contador y registra `last_login_at`. Administración permite activar, desactivar, desbloquear y cambiar contraseña.

## Matriz inicial

| Rol | Alcance |
|---|---|
| SUPER_ADMIN | Todos los permisos |
| ADMIN | Usuarios, clientes y activos |
| ANALYST | Consulta y operación de activos/procesos |
| READ_ONLY | Consulta y descargas autorizadas |

Permisos, roles y relaciones son tablas normalizadas. `@PreAuthorize` consume autoridades y la matriz central vive en `V3__seed_roles_and_permissions.sql`.

El bootstrap depende exclusivamente de variables de entorno y solo corre con tabla de usuarios vacía. Pendientes: recuperación por correo, MFA, rotación forzada, edición completa de roles y asignación de usuarios a clientes.
