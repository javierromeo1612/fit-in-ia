# Informe del modelo propio de «Cuéntanoslo»

Generado por `EntrenarModeloTest`. Entrenado con 15536 ejemplos; umbral 0.7 (elegido en validación); pesos recortados por debajo de 0.3; archivo de 553 KB.

## Prueba: 25 relatos que el modelo nunca ha visto

Escritos aparte (`modelo/datos/prueba.tsv`) y no usados para entrenar ni para ajustar nada. Es la cifra honesta.

| Qué detecta | Precisión (lo que marca, ¿es correcto?) | Exhaustividad (de lo que había, ¿cuánto encuentra?) | F1 |
|---|---|---|---|
| Necesidades | 97 % | 85 % | 91 % |
| Deportes | 100 % | 91 % | 95 % |
| Habla un familiar | 100 % | 100 % | 100 % |
| Franja horaria | 100 % | 100 % | 100 % |

Contando también las preguntas de confirmación (como mucho 2 por relato), encuentra el **89 %** de las necesidades.

## Desarrollo: 45 relatos usados para encontrar huecos

(`modelo/datos/desarrollo.tsv`). Sus fallos sirvieron para ampliar las frases de entrenamiento, así que esta cifra es optimista.

| Qué detecta | Precisión (lo que marca, ¿es correcto?) | Exhaustividad (de lo que había, ¿cuánto encuentra?) | F1 |
|---|---|---|---|
| Necesidades | 92 % | 93 % | 93 % |
| Deportes | 100 % | 100 % | 100 % |
| Habla un familiar | 96 % | 100 % | 98 % |
| Franja horaria | 88 % | 88 % | 88 % |

Contando también las preguntas de confirmación (como mucho 2 por relato), encuentra el **97 %** de las necesidades.

## Prueba, relato por relato

- ✅ Os escribo por mi hijo Álvaro, de 9 años. Tiene una parálisis y va en silla de ruedas manual. Le flipa el baloncesto y querría jugar los sábados por la mañana.
- ⚠️ Me llamo Carmen y tengo 67 años. Me cuesta mucho andar y me fatigo en cuanto hago un poco de esfuerzo. Me gustaría probar la natación.
  - No vio: Pausas, SinEscalones
- ⚠️ Mi hija Marta es sorda y usamos lengua de signos en casa. Es muy deportista y le encanta la bici.
  - No vio: AvisosVisuales
- ✅ Tengo 52 años, soy diabético y me tengo que pinchar insulina antes de comer. Busco algo tranquilo por la tarde.
- ✅ Mi hermano tiene autismo y cuando hay mucho jaleo se pone muy nervioso. Necesita saber de antemano adónde va.
- ⚠️ Desde que me jubilé no tengo con quién hacer deporte y me aburro mucho en casa. Me encantaría hacer senderismo.
  - No vio: D_Senderismo
- ⚠️ Es para mi madre: tiene 82 años, ve muy poco y le cuesta oír. No quiere ir sola.
  - No vio: Acompanante, AltoContraste
- ✅ Soy usuaria de silla de ruedas y voy a todas partes con mi perro de asistencia. Me gustaría jugar al tenis.
- ✅ Mi hijo tiene síndrome de Down, necesita que le expliquen las cosas despacio y con dibujos. Le gusta mucho el agua.
- ✅ Tengo epilepsia y tomo pastillas cada ocho horas. Prefiero grupos pequeños.
- ✅ Mi abuelo camina con andador y no puede con las escaleras. Le haría ilusión jugar al golf.
- ⚠️ Hola, me llamo Kike. Tengo ansiedad social y entrar en un sitio donde no conozco a nadie me cuesta un mundo.
  - No vio: Acogida
- ✅ Mi hija no habla y se comunica con pictogramas en una tablet. Necesita que alguien esté con ella durante la actividad.
- ✅ No tengo coche y vivo en las afueras, así que necesito que me lleven. Me apunto a lo que sea por la tarde.
- ⚠️ Mi pareja usa audífonos y lee los labios. Queremos hacer algo juntos los fines de semana.
  - No vio: Familiar
- ✅ Llevo muletas por una lesión de tobillo y estoy recuperándome. Me gustaría nadar un poco.
- ⚠️ Mi hija Inés tiene TEA y le molestan muchísimo las luces fuertes y los ruidos.
  - No vio: AvisarCambios, ConocerAntes, EspacioCalma
- ✅ Soy ciega y me gustaría hacer rutas por la montaña con alguien que me guíe.
- ✅ Mi padre tiene problemas de corazón y no puede hacer esfuerzos. El calor le sienta fatal.
- ✅ Cobro una pensión muy pequeña y necesito algo barato. Me gusta montar en bicicleta.
- ✅ Mi niño tiene discapacidad intelectual, es muy cariñoso y se agobia cuando hay mucha gente.
- ⚠️ Tengo una lesión medular y uso una handbike. Necesito vestuario con camilla porque me cambio tumbado.
  - Marcó de más: VestuarioIndividual
- ✅ Mi hija es muy tímida y es su primera vez. Prefiere estar siempre con las mismas compañeras.
- ⚠️ Soy Manuel, tengo 45 años y oigo perfectamente, pero tengo baja visión y con poca luz me pierdo. Me gustaría jugar al baloncesto por la noche.
  - Marcó de más: Vocaliza
- ⚠️ Mi mujer tiene fibromialgia y tiene que parar a menudo. Le gusta caminar por la playa.
  - No vio: IntensidadBaja

## Prueba de las necesidades nuevas: 12 relatos (modelo/datos/prueba_nuevas.tsv)

Escritos antes de entrenar, con las 8 necesidades añadidas el 24 nov. No se usan para ajustar nada.

| Qué detecta | Precisión (lo que marca, ¿es correcto?) | Exhaustividad (de lo que había, ¿cuánto encuentra?) | F1 |
|---|---|---|---|
| Necesidades | 94 % | 88 % | 91 % |
| Deportes | 100 % | 80 % | 89 % |
| Habla un familiar | 100 % | 100 % | 100 % |
| Franja horaria | 100 % | 100 % | 100 % |

Contando también las preguntas de confirmación (como mucho 2 por relato), encuentra el **91 %** de las necesidades.

- ⚠️ Mi hija tiene 15 años y lo que más le apetece es estar con chicas de su edad. Va en silla y después de la piscina necesita ducharse sentada.
  - No vio: D_Natacion
- ⚠️ Soy Ramón, tengo 72 años y me mareo si estoy mucho rato de pie. Me gustaría algo tranquilo con gente de mi quinta.
  - No vio: Sentado, Tranquilo
  - Marcó de más: Gratuita
- ✅ Cambio yo a mi hija porque no puede vestirse sola, y como soy su padre necesito un vestuario donde podamos entrar los dos.
- ✅ Mi hermano usa pañal y tiene que tener el baño a mano. Le encanta montar en bici.
- ✅ Soy sordo y prefiero que me manden la información por mensaje, no por megafonía.
- ✅ A mi hijo le agobia muchísimo competir, quiere jugar al baloncesto pero sin marcador ni presión.
- ✅ Tengo esclerosis múltiple: el frío me deja rígida y en el agua fría no me puedo mover. Me encanta nadar.
- ⚠️ Mi madre tiene poco equilibrio y tiene que hacer los ejercicios sentada. Por las mañanas está libre.
  - Marcó de más: Monitor
- ✅ Mi hijo tiene autismo, no soporta los ruidos fuertes y le cuesta mucho perder: mejor algo sin competición.
- ✅ Soy Lorena, tengo 19 años, voy en silla y quiero hacer deporte con gente joven. Me gusta el tenis.
- ⚠️ Mi padre usa sonda y necesita un baño cerca; además oye fatal.
  - No vio: Bucle, Vocaliza
- ✅ Tengo parálisis cerebral, la espasticidad empeora con el frío y necesito el agua calentita. Después me ducho sentado.

## Prueba ciega con diagnósticos y familiares: 10 relatos (modelo/datos/prueba_ciega.tsv)

Escritos antes de entrenar, con las 8 necesidades añadidas el 24 nov. No se usan para ajustar nada.

| Qué detecta | Precisión (lo que marca, ¿es correcto?) | Exhaustividad (de lo que había, ¿cuánto encuentra?) | F1 |
|---|---|---|---|
| Necesidades | 85 % | 92 % | 88 % |
| Deportes | 100 % | 100 % | 100 % |
| Habla un familiar | 86 % | 100 % | 92 % |
| Franja horaria | 100 % | 100 % | 100 % |

Contando también las preguntas de confirmación (como mucho 2 por relato), encuentra el **92 %** de las necesidades.

- ⚠️ Mi suegra tiene párkinson, tarda en hacer las cosas y se cansa al caminar. Le gustaría algo por la mañana.
  - Marcó de más: PasoAPaso, Pausas
- ✅ Tengo EPOC y me falta el aire con nada. Me encantaría volver a la piscina.
- ⚠️ Mi novio es ciego desde los veinte años y quiere probar el tándem en bici.
  - No vio: Audiodescripcion, Guia
- ⚠️ Soy Andrea, tengo 16 años y uso audífonos. Busco gente de mi edad para jugar al baloncesto.
  - Marcó de más: FAMILIAR
- ✅ A mi tío le amputaron una pierna y ahora lleva prótesis. Se agobia con mucha gente.
- ✅ Mi hijo tiene espina bífida, va en silla y usa sonda.
- ⚠️ Mi abuela tiene alzhéimer: se pierde con facilidad y necesita caras conocidas.
  - Marcó de más: Acompanante, Sentado
- ✅ Tengo TDAH y si me dan muchas instrucciones a la vez me pierdo. Prefiero no competir.
- ✅ Mi pareja va en silla eléctrica y necesitamos un vestuario donde pueda ayudarle a cambiarse.
- ✅ Tengo artritis y el agua fría me destroza las manos. Me gustaría nadar por la tarde.
