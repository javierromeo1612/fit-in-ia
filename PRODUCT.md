# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

delegated: Kotlin + Jetpack Compose + Material 3, igual que los otros proyectos Android del equipo (AGP 9.4.1, Compose BOM 2026.02.01). Versión 2: recomendador con aprendizaje automático dentro del móvil (regresión logística en línea, sin APIs ni Internet; ver ARQUITECTURA-IA.md) y avisos con WorkManager. Datos de ejemplo.

## Users

- Personas con discapacidad (física, sensorial o cognitiva) que viven en Marbella o la visitan y quieren hacer deporte pero no saben dónde ni con quién.
- Familiares, amigos y voluntarios que quieren acompañar.
- Ayuntamiento de Marbella: gestiona instalaciones accesibles y quiere medir el impacto de sus políticas de inclusión.

## Product Purpose

Que ninguna persona con discapacidad en Marbella se quede sin hacer deporte por no tener con quién ni dónde. El usuario indica qué necesita y la app le propone actividad, instalación verificada y compañía.

## Positioning

Deporte inclusivo, no separado. El producto es el emparejamiento de necesidad, actividad y acompañante; el mapa y las reservas son soporte.

## Operating Context

Proyecto de la hackathon Marbellup; el jurado es el Ayuntamiento de Marbella. La primera versión sirve de demo de tres minutos: la historia de Lucía (ficticia) reservando natación en seis pantallas, más el panel municipal.

## Capabilities and Constraints

- Perfil por necesidades, no por diagnóstico.
- Verificación en dos niveles: sello oficial y reseñas de usuarios con fecha.
- Voluntarios avalados por asociaciones.
- Panel municipal: ocupación en horas valle, participantes nuevos, barreras reportadas, demanda no atendida.
- Fuera de esta versión: registro, chat, pagos, mapa completo.
- Sin decidir: instalaciones reales del piloto.

## Brand Commitments

Nombre: Fit-In. Estilo visual de la versión 2 (sustituye al azul de la versión 1, que sigue en el repo original): app deportiva luminosa en lila y violeta, fondos en degradado suave, tarjetas blancas tipo cristal muy redondeadas, botones en píldora con degradado, fotos de cada deporte y una mascota robot que hace de asistente. Tipografía: Atkinson Hyperlegible. Las personas de la demo llevan avatares de iniciales, no fotos de caras.

## Evidence on Hand

No hay datos reales de instalaciones, usuarios ni cifras municipales. Todo dato de la demo es de ejemplo y debe ir marcado como tal; no inventar nombres de instalaciones reales ni estadísticas.

## Product Principles

1. Se pregunta qué necesitas, nunca qué tienes.
2. La confianza se gana con verificación visible: quién lo comprobó y cuándo.
3. Nadie va solo si no quiere.
4. La app es tan accesible como los sitios que recomienda.

## Accessibility & Inclusion

Compatible con TalkBack, objetivos táctiles de 48 dp como mínimo, contraste AA, tamaño de texto ajustable, modo de lectura fácil y respeto del ajuste de reducir animaciones.
