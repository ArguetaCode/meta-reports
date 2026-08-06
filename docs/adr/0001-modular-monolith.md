# ADR 0001: Monolito modular para el MVP

- Estado: Aceptado para la base inicial
- Fecha: 2026-08-03

## Contexto

El sistema debe integrar Meta, normalizar métricas, clasificar anuncios, resolver incidencias y generar archivos para unos 70 clientes. El equipo aún debe validar permisos, modelo y flujo con un piloto. Desplegar y operar múltiples servicios añadiría coordinación, observabilidad y consistencia distribuida antes de conocer límites reales.

## Decisión

Construir una sola aplicación Spring Boot y una base PostgreSQL, organizada por módulos funcionales con dependencias explícitas. Los clientes externos y el almacenamiento se aíslan mediante puertos para permitir sustituciones futuras.

## Consecuencias

Positivas: despliegue, transacciones, pruebas y operación más simples; evolución rápida del modelo; menor duplicación. Negativas: requiere disciplina para evitar acoplamiento, el escalado es inicialmente conjunto y un fallo puede afectar todo el proceso. Se mitigará con límites de módulos, jobs recuperables, timeouts y pruebas de arquitectura.

## Alternativas descartadas

Microservicios: no justifican su costo operacional en el MVP. Aplicación sin módulos: acelera el inicio pero aumenta acoplamiento entre integración, cálculos e informes.

## Revisión

Reevaluar tras el piloto si existen perfiles de carga u ownership claramente independientes, no por cantidad de clases.
