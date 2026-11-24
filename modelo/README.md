# Modelo propio de «Cuéntanoslo»

Un modelo **entrenado por el equipo** que lee lo que una persona cuenta con sus palabras y lo traduce al catálogo de Fit-In: 50 necesidades, 6 deportes, si habla un familiar y la franja horaria. Funciona **dentro del móvil**: sin internet, sin coste, sin límite de uso y sin enviar a ningún servidor lo que se cuenta, que son datos de salud y discapacidad.

## Por qué un modelo propio aquí

- **Privacidad:** lo que cuenta una familia sobre su hija no tiene por qué salir del teléfono.
- **Siempre disponible:** no depende del wifi ni del cupo de una API. Responde en milisegundos.
- **Explicable:** para cada necesidad enseña qué frase la indica («porque dijiste "va en silla"»), y cuando duda, **pregunta** en vez de decidir. La persona siempre confirma.

Para «Fit-In te acompaña» seguimos usando un modelo grande en la nube: tiene que razonar con señales que chocan y redactar mensajes, y eso un modelo pequeño no lo hace bien.

## Cómo funciona

1. **El texto se trocea** en frases y subfrases, más ventanas cortas de 5 palabras (`Rasgos.kt`).
2. **Cada trozo se convierte en rasgos:** palabras, parejas de palabras («no_oye», «va_en») y trocitos de 3 a 5 letras, para que «cansa», «cansada» y «cansancio» se parezcan y aguante faltas de ortografía. Las dos palabras que siguen a «no», «ni», «nunca» o «tampoco» llevan además un rasgo de negación, para que «no usa silla» no se parezca a «usa silla».
3. **Una regresión logística por etiqueta** suma los pesos de esos rasgos y da una probabilidad (`ModeloPropio.kt`). Para cada necesidad se queda con el trozo que más la indica, que es la cita que se enseña.
4. **Por encima del umbral, se propone; algo por debajo, se pregunta** (`CuentanosloPropio.kt`). El nombre y el resumen salen de reglas sencillas.

El archivo del modelo (`app/src/main/assets/modelo_cuentanoslo.bin`) ocupa unos 570 KB.

## Datos

- **Entrenamiento** (`generar_datos.mjs`): unas 550 frases base escritas a mano y revisadas por el equipo, con cómo diría una persona cada necesidad en primera y en tercera persona. También hay negaciones («oye bien», «no usa silla») para que no marque de más. Incluye diagnósticos frecuentes con las necesidades que suelen traer (ictus → sin escalones y más tiempo), como pista: la persona siempre confirma. Las pistas del catálogo (`Need` en `Model.kt`) también se usan como ejemplos. El generador las combina en unos 16.800 relatos de ejemplo. La generación es reproducible (semilla fija). Las frases base las redactamos con ayuda de Claude y las revisó el equipo; está declarado en `docs/USO-IA.md`.
- **Desarrollo** (`datos/desarrollo.tsv`, 45 relatos): sirvió para encontrar huecos y ampliar las frases de entrenamiento.
- **Prueba** (`datos/prueba.tsv`, 25 relatos): escritos aparte, **nunca usados para entrenar ni para ajustar nada**. Es la cifra honesta.
- **Necesidades nuevas** (`datos/prueba_nuevas.tsv`, 12 relatos): escritos antes de entrenar las 8 necesidades añadidas el 24 nov.
- **Prueba ciega** (`datos/prueba_ciega.tsv`, 10 relatos con diagnósticos y familiares): escrita a la vez que las pistas de diagnósticos, así que es menos independiente que la de prueba.

Las métricas están en [INFORME.md](INFORME.md). Resumen con los relatos de prueba: el 97 % de lo que marca es correcto, propone el 85 % de lo que hay y, sumando las preguntas de confirmación, llega al 89 % (la primera versión, con 42 necesidades: 98 %, 77 % y 88 %). Con las necesidades nuevas: 94 %, 88 % y 91 %.

## Volver a entrenar

```powershell
node modelo/generar_datos.mjs
cd android; ./gradlew testDebugUnitTest -Pentrenar=1 -Ppeso=2 --tests "*EntrenarModelo*"
```

Tarda unos segundos en un portátil. `-Ppeso=2` da el doble de peso a los ejemplos positivos de cada etiqueta (encuentra más sin marcar mucho de más). Reescribe el archivo del modelo y `INFORME.md`.

**Para mejorarlo:** añadid frases reales en `generar_datos.mjs` (cómo lo dicen las familias y las asociaciones) y relatos nuevos en `datos/prueba.tsv`, escritos por personas distintas a las que escriben el entrenamiento.

## Limitaciones conocidas

- Mira qué palabras aparecen, no razona. No deduce, por ejemplo, que «trabajo por las mañanas» significa «mejor por la tarde». Por eso esas frases se quitaron del entrenamiento.
- Con expresiones muy distintas de las de entrenamiento puede no ver una necesidad. Por eso la pantalla pregunta las dudas y deja marcar a mano.
- Los datos de entrenamiento son sintéticos. Con relatos reales, anonimizados y con permiso, mejoraría.

## En esta versión de la app

Traído de la versión de Talent Land del equipo. El código del modelo está en
`app/src/main/java/com/marbellup/fitin/ai/` (`propio/Rasgos.kt`, `propio/ModeloPropio.kt`,
`propio/CuentanosloPropio.kt` y `PerfilIA.kt`). Para reentrenarlo hace falta el entrenador
(`EntrenarModeloTest.kt`) de la versión de Talent Land.
