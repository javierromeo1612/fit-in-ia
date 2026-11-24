// Generador de datos de entrenamiento del modelo propio de «Cuéntanoslo».
//
//   node modelo/generar_datos.mjs   →   modelo/datos/entrenamiento.tsv
//
// Cada línea: texto <TAB> etiquetas separadas por comas (vacío = ninguna).
// Etiquetas: los nombres de Need (SinEscalones, Bucle…), D_<Deporte>, FAMILIAR (habla
// alguien de otra persona) y F_Manana / F_Tarde / F_Noche (franja preferida).
//
// Las frases base las escribimos nosotros (equipo + Claude, revisadas a mano): cómo diría
// una persona cada necesidad con sus palabras, en primera y en tercera persona. El
// generador las combina en relatos con introducciones, frases neutras y conectores.
// IMPORTANTE: el conjunto de prueba (datos/prueba.tsv) se escribe aparte y NUNCA se usa
// aquí, para medir con honestidad cómo se porta con frases que no ha visto.

import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const aqui = dirname(fileURLToPath(import.meta.url))

// Semilla fija: el mismo conjunto cada vez (entrenamiento reproducible).
let semilla = 20261124
const azar = () => ((semilla = (semilla * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff)
const uno = (lista) => lista[Math.floor(azar() * lista.length)]
const quizas = (p) => azar() < p

const NOMBRES = ['Lucía', 'Hugo', 'Marta', 'Pedro', 'Ana', 'Carmen', 'Dani', 'Rocío', 'Tomás', 'Elena', 'Javi', 'Pilar', 'Manuel', 'Irene', 'Sergio', 'Lola', 'Antonio', 'Nerea', 'Pablo', 'Rosa', 'Álvaro', 'Inés', 'Kike', 'Paco', 'Sofía', 'Mario']
const EDAD = () => String(6 + Math.floor(azar() * 80))
const rellenar = (s) => s.replaceAll('{N}', uno(NOMBRES)).replaceAll('{E}', EDAD())

// [etiquetas, frases]. Las etiquetas se aplican a todas las frases del grupo.
const FRASES = [
  // ---------- Para moverte ----------
  [['SinEscalones', 'PuertasAnchas', 'BanoAdaptado'], [
    'va en silla', 'va en silla de ruedas', 'voy en silla de ruedas', 'usa silla de ruedas', 'se mueve en silla',
    'usa una silla eléctrica', 'desde el accidente voy en silla', 'tiene una lesión medular y va en silla',
    'es usuario de silla de ruedas', 'necesito la silla para moverme', 'tiene una silla eléctrica', 'mi silla es eléctrica', 'lleva silla de ruedas desde pequeña',
    'tiene parálisis cerebral y se mueve en silla', 'mi silla es manual', 'no camina, va en silla', 'soy usuaria de silla',
  ]],
  [['SinEscalones', 'PuertasAnchas'], [
    'anda con andador', 'lleva andador', 'camina con andador', 'uso andador para caminar', 'necesita el andador para todo',
    'se ayuda de un andador', 'voy con mi andador a todas partes',
  ]],
  [['SinEscalones'], [
    'camina con muletas', 'voy con muletas', 'usa bastón', 'camino con bastón', 'anda con dificultad', 'cojea bastante',
    'tiene movilidad reducida', 'necesita rampa para entrar', 'los escalones me cuestan', 'se tropieza con los escalones',
    'le cuesta caminar', 'camina mal desde el ictus', 'tiene problemas de movilidad',
  ]],
  [['SinEscalones', 'Ascensor'], [
    'le cuestan mucho las escaleras', 'no puede subir escaleras', 'las escaleras son un problema para mí',
    'no puedo subir escalones', 'subir escaleras le resulta imposible', 'con las escaleras no puede',
  ]],
  [['Ascensor'], [
    'necesito ascensor', 'si hay que subir a una planta tiene que haber ascensor', 'el sitio tiene que tener ascensor',
    'si está en un primer piso no podemos subir',
  ]],
  [['PuertasAnchas'], ['la silla es bastante ancha', 'con la silla no paso por puertas estrechas', 'los pasillos estrechos son un problema']],
  [['Aparcamiento'], [
    'tenemos tarjeta de aparcamiento', 'tiene la tarjeta azul de aparcamiento', 'vamos en coche adaptado',
    'necesito aparcar cerca de la entrada', 'conduzco un coche adaptado', 'necesitamos una plaza de aparcamiento reservada',
    'vamos siempre en coche y hay que aparcar cerca',
  ]],
  [['DistanciasCortas'], [
    'se cansa al caminar', 'no puede andar mucho rato', 'me ahogo si camino mucho', 'camina poco', 'necesita sentarse cada poco',
    'andar distancias largas le cuesta', 'a los diez minutos andando tengo que parar', 'me duelen las piernas si camino mucho',
    'no aguanta mucho de pie', 'necesita sitios para sentarse', 'no puede estar mucho tiempo de pie',
  ]],
  [['IntensidadBaja', 'DistanciasCortas'], [
    'tiene artrosis en las rodillas', 'le duele la cadera', 'tengo las rodillas fatal', 'me operaron de la cadera',
    'tiene problemas en las articulaciones', 'le duelen mucho las piernas',
  ]],
  [['Transporte'], [
    'no tenemos coche', 'no conduzco', 'no tengo cómo llegar', 'dependo de que alguien me lleve', 'vivimos lejos y no hay autobús',
    'necesitaría que alguien la recogiera', 'no tengo coche', 'no tiene coche ni carnet', 'no tenemos cómo ir', 'no puede coger el autobús sola', 'el transporte es un problema',
    'no tiene forma de ir hasta allí', 'vive en un pueblo sin transporte', 'nadie puede llevarle',
  ]],

  // ---------- Vestuario y baño ----------
  [['BanoAdaptado'], ['necesita barras para ir al baño', 'el baño tiene que estar adaptado', 'necesito un aseo adaptado', 'necesita un baño grande para la silla']],
  [['Vestuario', 'AyudaCambiarse'], [
    'para cambiarse necesita tumbarse', 'necesita una camilla para vestirse', 'en el vestuario necesita espacio y una camilla',
    'hay que cambiarle tumbado',
  ]],
  [['AyudaCambiarse'], [
    'no puede vestirse solo', 'necesita ayuda para cambiarse', 'hay que ayudarle a ponerse el bañador', 'no se viste sola',
    'yo le ayudo a cambiarse siempre', 'para ponerse la ropa necesita ayuda', 'necesito que me ayuden a vestirme',
  ]],
  [['Grua'], [
    'no puede entrar al agua por la escalera', 'para meterse en la piscina necesita grúa', 'hay que meterla en el agua con grúa',
    'necesito ayuda para entrar en la piscina', 'necesita grúa para la piscina', 'no puede bajar la escalera de la piscina',
  ]],

  // ---------- Para ver ----------
  [['Guia', 'Audiodescripcion'], [
    'es ciego', 'soy ciega', 'no ve nada', 'tiene ceguera total', 'perdió la vista', 'es invidente', 'no tengo vista',
    've muy poquito, casi nada', 'se quedó ciego hace años',
  ]],
  [['Guia'], ['necesita a alguien que le guíe', 'necesito una persona que me guíe', 'necesita ir del brazo de alguien']],
  [['AltoContraste', 'Iluminacion'], [
    'tiene baja visión', 'tengo baja visión', 'tengo muy poca vista', 'tiene restos visuales', 'veo mal de lejos', 've muy poco', 'le cuesta leer los carteles', 'tiene poca vista',
    'veo borroso', 'tiene una enfermedad en la vista', 'no ve bien de cerca ni de lejos',
  ]],
  [['AltoContraste'], ['lee en braille', 'necesita letras grandes y contraste', 'necesito carteles grandes', 'no distingue los colores claros']],
  [['Iluminacion'], ['con poca luz no ve', 'necesito sitios bien iluminados', 've mal cuando está oscuro', 'de noche no veo bien', 'si hay poca luz no se orienta']],
  [['Audiodescripcion'], ['necesita que le expliquen en voz lo que pasa', 'que le cuenten lo que ocurre alrededor', 'me tienen que describir lo que pasa']],
  [['PerroAsistencia'], [
    'va con perro guía', 'tengo un perro de asistencia', 'siempre va con su perro de asistencia', 'mi perro guía me acompaña a todas partes',
    'tiene un perro de asistencia', 'necesito que dejen entrar a mi perro',
  ]],

  // ---------- Para oír ----------
  [['Signos', 'AvisosVisuales'], [
    'es sordo y se comunica en lengua de signos', 'soy sorda y uso lengua de signos', 'habla en signos', 'es sorda profunda',
    'se comunica en lengua de signos', 'no oye nada', 'es sordo de nacimiento', 'soy sordo',
  ]],
  [['Signos'], ['necesita intérprete de lengua de signos', 'necesito intérprete']],
  [['Bucle'], ['usa audífono', 'lleva audífonos', 'tiene implante coclear', 'llevo un implante coclear', 'usa audífonos en los dos oídos']],
  [['Bucle', 'Vocaliza'], ['oye mal', 'oigo regular', 'oigo mal', 'no oigo bien', 'oigo poco', 'oigo bastante mal', 'no oye casi nada por un oído', 'le cuesta oír', 'está un poco sorda', 'oye muy poco', 'no oye bien', 'tengo pérdida auditiva']],
  [['AvisosVisuales'], ['no oye el silbato', 'no se entera de los avisos por megafonía', 'necesita avisos con luces', 'no escucha cuando le llaman']],
  [['Vocaliza'], ['lee los labios', 'necesita que le hablen de frente', 'si le hablan de espaldas no se entera', 'hay que vocalizar bien', 'me tienen que hablar despacio y de frente']],

  // ---------- Para entender ----------
  [['LecturaFacil', 'PasoAPaso', 'MasTiempo'], [
    'tiene discapacidad intelectual', 'tiene síndrome de Down', 'tiene un retraso madurativo', 'le cuesta entender las cosas',
    'tiene dificultades de aprendizaje',
  ]],
  [['LecturaFacil'], [
    'le cuesta entender los textos', 'no lee bien', 'necesita explicaciones sencillas', 'las cosas complicadas no las entiende',
    'necesito que me lo expliquen fácil', 'los papeles le cuestan mucho',
  ]],
  [['Pictogramas'], ['entiende mejor con dibujos', 'no sabe leer', 'usa pictogramas', 'con imágenes lo entiende todo', 'se guía por dibujos']],
  [['Saac', 'Pictogramas'], [
    'no habla', 'no habla mucho', 'se comunica con una tablet', 'usa un tablero de comunicación', 'es no verbal',
    'se comunica con pictogramas', 'casi no habla', 'habla muy poco y usa una app para comunicarse',
  ]],
  [['PasoAPaso'], [
    'se pierde si le dan muchas instrucciones', 'hay que explicarle las cosas de una en una', 'necesita instrucciones paso a paso',
    'si le dices muchas cosas a la vez se lía', 'necesita que le expliquen las cosas poco a poco',
  ]],
  [['MasTiempo'], ['va a su ritmo', 'necesita más tiempo para todo', 'es lento haciendo las cosas', 'tarda en entender', 'no le metáis prisa', 'necesito ir a mi ritmo']],

  // ---------- Para estar a gusto ----------
  [['Tranquilo'], [
    'con mucho ruido se agobia', 'no soporta el ruido', 'el ruido le pone nervioso', 'le molestan los gritos', 'necesita sitios tranquilos',
    'tiene hipersensibilidad al sonido', 'los ruidos fuertes le asustan', 'me agobio con el ruido', 'prefiere sitios sin jaleo',
    'con tanto ruido se bloquea', 'se tapa los oídos con el ruido', 'necesita un ambiente tranquilo', 'quiere gente tranquila',
  ]],
  [['Tranquilo', 'AvisarCambios', 'ConocerAntes', 'EspacioCalma'], [
    'tiene autismo', 'es autista', 'tiene TEA', 'está diagnosticado de autismo', 'tiene un trastorno del espectro autista',
  ]],
  [['LuzSuave'], ['le molestan los focos', 'las luces fuertes le molestan', 'le duelen los ojos con luz intensa', 'tiene fotofobia', 'no aguanta las luces muy blancas']],
  [['GrupoReducido'], [
    'le agobia mucha gente', 'en grupos grandes lo pasa mal', 'prefiere grupos pequeños', 'con tanta gente se bloquea',
    'no le gustan las multitudes', 'mejor con pocas personas', 'me agobio cuando hay mucha gente',
  ]],
  [['EspacioCalma'], [
    'a veces necesita parar y estar solo', 'cuando se desborda necesita un rincón tranquilo', 'si se agobia tiene que salir un rato',
    'a veces tiene crisis y necesita calmarse aparte', 'necesita un sitio para retirarse',
  ]],
  [['ConocerAntes'], [
    'le cuesta lo nuevo', 'necesita conocer el sitio antes', 'las sorpresas le generan ansiedad', 'tiene ansiedad con los sitios nuevos',
    'me gustaría ver el lugar antes', 'los sitios nuevos le dan miedo', 'tengo ansiedad',
  ]],
  [['AvisarCambios'], [
    'le cuestan los cambios', 'necesita rutina', 'si cambian el horario se descoloca', 'necesita que le avisen de cualquier cambio',
    'los cambios de última hora le alteran', 'necesita saber qué va a pasar',
  ]],
  [['Acogida'], [
    'es la primera vez que hago deporte', 'soy muy tímido', 'no sé a quién dirigirme al llegar', 'me da vergüenza llegar sin conocer a nadie',
    'le cuesta entrar solo en un sitio nuevo', 'es muy vergonzosa', 'nunca ha hecho deporte en grupo',
  ]],

  // ---------- Durante la actividad ----------
  [['Monitor'], [
    'necesita a alguien pendiente durante la actividad', 'necesita ayuda durante el ejercicio', 'requiere atención constante',
    'solo no puede hacer los ejercicios', 'necesita un monitor de apoyo', 'hay que estar encima de él',
  ]],
  [['Material'], [
    'necesita una silla deportiva', 'usa bicicleta adaptada', 'necesita material adaptado', 'juega con una silla de deporte',
    'necesitaría una handbike', 'necesita un triciclo adaptado', 'necesita una pala adaptada',
  ]],
  [['IntensidadBaja', 'Pausas'], [
    'se cansa rápido', 'me canso enseguida', 'se agota enseguida', 'se agota', 'acaba agotado', 'termina reventada', 'se cansa en nada', 'le falta el aire enseguida', 'tiene poca resistencia', 'se fatiga mucho', 'se cansa muchísimo',
    'me operaron de la rodilla', 'estoy recuperándome de una lesión', 'tiene fibromialgia', 'acaba agotada enseguida',
    'tengo poco fondo', 'se queda sin fuerzas pronto',
  ]],
  [['IntensidadBaja'], [
    'tiene problemas de corazón', 'le cuesta respirar cuando hace esfuerzo', 'no puede hacer mucho esfuerzo', 'tiene asma',
    'nada de deportes intensos', 'tiene que ir suave', 'el médico me ha dicho que nada de esfuerzos',
  ]],
  [['Pausas'], ['necesita descansar a menudo', 'tiene que parar cada poco', 'necesito pausas', 'necesita hacer descansos']],

  // ---------- Compañía ----------
  [['Acompanante'], [
    'está sola', 'no tiene con quién ir', 'estoy muy solo', 'me siento solo', 'vive sola desde que murió su marido', 'me gustaría ir con alguien', 'no tiene amigos', 'pasa mucho tiempo sola', 'me da miedo ir solo', 'no tengo amigos con quien hacer deporte', 'vive solo',
    'se siente sola', 'no conozco a nadie para ir', 'nadie puede acompañarle', 'no tengo con quién ir', 'iría si alguien viniera conmigo',
  ]],
  [['Familiar'], [
    'quiere ir con su madre', 'quiere ir conmigo', 'vendría conmigo', 'iríamos juntos', 'quiere venir con su hijo', 'me gustaría ir con mi hijo', 'prefiere que vaya su padre con ella', 'queremos hacerlo en familia',
    'iría con mi pareja', 'me gustaría hacer deporte con mi nieto', 'quiero ir con mi hermana',
  ]],
  [['MismoGrupo'], [
    'le cuesta conocer gente nueva', 'prefiere ver siempre a las mismas personas', 'le gusta repetir con el mismo grupo',
    'las caras nuevas le cuestan', 'necesita estar con gente conocida', 'prefiero ir siempre con las mismas personas',
  ]],

  // ---------- Cuidados ----------
  [['PrimerosAuxilios'], [
    'tiene epilepsia', 'a veces tiene convulsiones', 'puede tener crisis', 'a veces se marea', 'le pueden dar bajadas de azúcar',
    'se ha desmayado alguna vez', 'tiene ataques', 'tengo crisis epilépticas',
  ]],
  [['PrimerosAuxilios', 'Medicacion'], ['es diabético', 'tiene diabetes', 'se pincha insulina', 'soy diabética']],
  [['Medicacion'], [
    'toma pastillas a una hora fija', 'tiene que tomar su medicación a las seis', 'necesita parar para medicarse',
    'tomo medicación cada pocas horas', 'tiene que tomar la medicación a su hora',
  ]],
  [['SombraAgua'], [
    'le afecta mucho el calor', 'me mareo con el sol', 'el sol le hace daño', 'tiene que estar a la sombra', 'necesita beber mucha agua', 'con el calor se marea', 'se deshidrata fácil', 'necesito sombra', 'no aguanto el sol',
    'el calor le sienta fatal', 'tiene que beber agua a menudo',
  ]],
  [['Gratuita'], [
    'no podemos pagar mucho', 'vamos justos de dinero', 'tiene que ser gratis o barato', 'cobro una pensión pequeña',
    'no me lo puedo permitir si es caro', 'estoy en paro', 'no tenemos dinero para cuotas',
  ]],

  // ---------- Diagnósticos frecuentes → necesidades típicas (la persona siempre confirma) ----------
  [['SinEscalones', 'PuertasAnchas', 'BanoAdaptado'], ['tiene una lesión medular', 'es parapléjico', 'tiene tetraplejia', 'soy parapléjica', 'tiene una paraplejia']],
  [['SinEscalones', 'PuertasAnchas', 'BanoAdaptado', 'BanoCerca'], ['tiene espina bífida', 'nació con espina bífida']],
  [['SinEscalones'], ['le amputaron una pierna', 'lleva una prótesis en la pierna', 'tiene una amputación', 'camina con prótesis']],
  [['IntensidadBaja', 'Pausas', 'SinEscalones'], ['tiene ELA', 'tiene distrofia muscular', 'tiene atrofia muscular']],
  [['MasTiempo', 'DistanciasCortas', 'Pausas'], ['tiene párkinson', 'le diagnosticaron párkinson', 'tengo párkinson']],
  [['SinEscalones', 'MasTiempo'], ['tuvo un ictus', 'le dio un ictus', 'tiene hemiplejia', 'se le quedó medio cuerpo paralizado']],
  [['IntensidadBaja', 'Pausas'], ['tiene esclerosis múltiple', 'tiene EPOC', 'tiene insuficiencia respiratoria', 'le falta el aire', 'tiene cáncer y está en tratamiento']],
  [['IntensidadBaja', 'PrimerosAuxilios'], ['lleva marcapasos', 'tiene una cardiopatía', 'le operaron del corazón']],
  [['MismoGrupo', 'PasoAPaso', 'Acompanante'], ['tiene alzhéimer', 'tiene demencia', 'empieza a perder la memoria', 'se le olvidan las cosas']],
  [['PasoAPaso'], ['tiene TDAH', 'tiene déficit de atención', 'le cuesta concentrarse']],
  [['IntensidadBaja'], ['tiene artritis', 'tiene artritis reumatoide', 'le duelen las articulaciones']],
  // ---------- Necesidades añadidas el 24 nov ----------
  [['DuchaAdaptada'], [
    'necesita ducharse sentada', 'después de nadar se ducha sentado', 'necesito una ducha con asiento', 'en la ducha necesita silla',
    'no puede ducharse de pie', 'la ducha tiene que estar adaptada', 'me ducho sentado en una silla de ducha',
  ]],
  [['VestuarioIndividual'], [
    'la cambio yo y soy su padre', 'necesitamos un vestuario donde entremos los dos', 'le cambia su madre y es un chico',
    'necesita intimidad para cambiarse', 'prefiero un vestuario individual', 'no puede cambiarse en un vestuario compartido',
    'necesita un vestuario familiar', 'como soy su padre no puedo entrar al vestuario de chicas',
  ]],
  [['BanoCerca'], [
    'usa pañal', 'tiene incontinencia', 'necesita el baño a mano', 'usa sonda', 'tiene que ir al baño muy a menudo',
    'el baño tiene que estar cerca', 'si el baño está lejos no llega', 'tengo problemas de vejiga',
  ]],
  [['InfoEscrita'], [
    'prefiero que me lo manden por escrito', 'mejor por mensaje que por teléfono', 'no me entero por megafonía, mejor un cartel',
    'necesita la información escrita', 'que me avisen por whatsapp', 'las llamadas no me sirven, mejor mensajes',
  ]],
  [['Sentado'], [
    'tiene que hacerlo sentada', 'no aguanta de pie', 'se marea si está de pie mucho rato', 'tiene poco equilibrio',
    'necesito hacer los ejercicios sentado', 'de pie se cae', 'pierde el equilibrio con facilidad', 'solo puede hacer deporte sentado',
  ]],
  [['SinCompetir'], [
    'le agobia competir', 'no quiere competir', 'sin marcador ni presión', 'le cuesta mucho perder', 'quiero disfrutar sin competir',
    'nada de competiciones', 'prefiere jugar sin presión', 'odio que cuenten los puntos', 'a mi ritmo, sin compararme con nadie',
  ]],
  [['MismaEdad'], [
    'quiere estar con chicas de su edad', 'busco gente joven', 'quiere ir con gente de su edad', 'me gustaría gente de mi quinta',
    'busco gente mayor como yo', 'es adolescente y quiere amigos de su edad', 'quiero hacer deporte con jóvenes',
    'con niños pequeños se aburre', 'prefiere gente mayor',
  ]],
  [['AguaTemplada'], [
    'el agua fría le deja rígida', 'necesita el agua calentita', 'con el frío se agarrota', 'tiene espasticidad y el frío la empeora',
    'necesito piscina climatizada', 'en agua fría no me puedo mover', 'el frío le sienta fatal a los músculos', 'el agua tiene que estar templada',
  ]],
  // ---------- Deportes ----------
  [['D_Baloncesto'], ['le encanta el baloncesto', 'juega a baloncesto', 'quiere jugar al básquet', 'me gusta el basket', 'siempre ha querido jugar al baloncesto', 'le gusta encestar']],
  [['D_Natacion'], ['le gusta nadar', 'le encanta la piscina', 'vamos a la piscina', 'disfruta en el agua', 'le gusta mucho el agua', 'algo en el agua', 'chapotear en el agua', 'me gustaría nadar', 'le encanta el agua', 'quiere ir a la piscina', 'nadaba de joven', 'le gusta la natación', 'quiere aprender a nadar']],
  [['D_Tenis'], ['quiere probar el tenis', 'algo de raqueta', 'deportes de raqueta', 'le gusta darle a la raqueta', 'jugaba al tenis', 'me gusta el tenis', 'le encanta el tenis']],
  [['D_Golf'], ['le gustaría probar el golf', 'juego al golf', 'el golf me llama la atención', 'le gusta el golf']],
  [['D_Senderismo'], ['le gusta caminar por el monte', 'salir al monte', 'andar por el campo', 'rutas a pie', 'pasear por la naturaleza', 'antes iba mucho al monte', 'me encanta la montaña', 'le gusta hacer rutas por la naturaleza', 'le gusta el senderismo', 'quiere salir al campo', 'me gusta pasear por la sierra']],
  [['D_Ciclismo'], ['le gusta montar en bici', 'quiere ir en bicicleta', 'me encanta el ciclismo', 'le gusta pedalear', 'salía mucho en bici']],

  // ---------- Franja ----------
  [['F_Manana'], ['por las mañanas', 'mejor por la mañana', 'solo puede de mañana', 'por la mañana temprano']],
  [['F_Tarde'], ['por las tardes', 'mejor por la tarde', 'después del colegio', 'a partir de las cinco', 'por la tarde le viene mejor']],
  // (Quitadas a propósito frases como «trabajo por las mañanas» → tarde: un modelo de palabras
  // no puede razonar esa inversión y aprendía que «mañanas» significa tarde.)
  [['F_Noche'], ['por la noche', 'a última hora del día', 'al final del día', 'cuando anochece', 'cuando sale de trabajar por la noche', 'a última hora', 'después de cenar', 'mejor de noche']],
  // Frases con dos etiquetas a la vez que aparecen mucho.
  [['Acompanante', 'F_Tarde'], ['por las tardes está sola', 'por la tarde no tiene con quién ir', 'por las tardes se aburre en casa sola']],
  [['Acompanante', 'F_Manana'], ['por las mañanas está solo en casa']],
]

// Quién habla: un familiar (FAMILIAR) o la propia persona (sin etiqueta).
const INTRO_FAMILIAR = [
  'es para mi hija {N}', 'es para mi hijo {N}', 'mi hijo {N} tiene {E} años', 'mi hija tiene {E} años', 'te escribo por mi padre',
  'lo gestiono yo porque es mi madre', 'cuido de mi hermano {N}', 'mi abuela {N} tiene {E} años', 'es para mi marido',
  'soy su madre', 'soy la cuidadora de {N}', 'mi sobrino {N}', 'os cuento lo de mi padre', 'mi mujer', 'mi hermana {N}',
  'es para mi nieta', 'mi niño tiene {E} años', 'mi padre tiene {E} años', 'apunto a mi hija', 'busco algo para mi hijo',
  'mi pareja', 'mi novio {N}', 'mi novia', 'mi abuelo tiene {E} años', 'mi abuela', 'mi tío', 'mi tía {N}', 'mi primo {N}', 'mi suegra',
  'mi cuñado', 'mi chico', 'mi niña tiene {E} años', 'es para mi pareja', 'te escribo por mi abuelo',
]
const INTRO_PROPIA = [
  'soy {N}', 'me llamo {N} y tengo {E} años', 'hola, soy {N}', 'tengo {E} años', 'os escribo yo', 'me llamo {N}',
  'busco hacer deporte', 'quiero empezar a hacer deporte', 'hola', 'soy {N} y tengo {E} años',
]
const NEUTRAS = [
  'tiene {E} años', 'es muy alegre', 'vivimos en Málaga', 'le gusta la música', 'está jubilado', 'estudia en el instituto',
  'es muy sociable', 'acabamos de mudarnos', 'es un chico muy cariñoso', 'trabaja en una oficina', 'me encanta el cine',
  'tiene dos hermanos', 'es muy simpática', 'le gustan los animales', 'tiene muchas ganas', 'es muy buena gente',
  'vivimos en el centro', 'es la primera vez que uso la app', 'me lo recomendó una amiga', 'quiere hacer amigos',
]
// Negaciones y aclaraciones: frases que se parecen a una necesidad pero NO la piden.
const NEGACIONES = [
  'oye bien', 'no usa silla', 'camina perfectamente', 'el ruido no le molesta', 've bien', 'puede ir sola sin problema',
  'no necesita ayuda para cambiarse', 'no toma ninguna medicación', 'el dinero no es problema', 'tenemos coche',
  'entiende todo perfectamente', 'habla sin problema', 'no se cansa', 'no tiene problemas de movilidad', 'no le importa la gente',
  'sube escaleras sin problema', 'lee perfectamente', 'no necesita gafas', 'se viste sola', 'no le afecta el calor',
  'tiene con quién ir', 'el horario le da igual', 'no tiene ninguna enfermedad', 'conduce', 'le da igual el sitio',
  'se ducha de pie sin problema', 'le encanta competir', 'le da igual la edad del grupo', 'el agua fría no le molesta',
  'aguanta de pie sin problema', 'no necesita el baño cerca', 'le vale que se lo expliquen hablando',
]
const CONECTORES = [', ', ' y ', '. ', ', y ', ', pero ', '. Además, ', ' y además ', '; ', '. También ']
const ADORNOS = ['', '', '', 'a veces ', 'la verdad es que ', 'pues ', 'un poco ', 'bastante ', 'además ']

const fraseDe = (grupo) => rellenar(uno(grupo[1]))

function relato() {
  const etiquetas = new Set()
  const partes = []
  const familiar = quizas(0.5)
  if (quizas(0.8)) {
    partes.push(rellenar(uno(familiar ? INTRO_FAMILIAR : INTRO_PROPIA)))
    if (familiar) etiquetas.add('FAMILIAR')
  }
  const n = 1 + Math.floor(azar() * 4)
  for (let i = 0; i < n; i++) {
    const g = uno(FRASES)
    partes.push(uno(ADORNOS) + fraseDe(g))
    g[0].forEach((e) => etiquetas.add(e))
  }
  if (quizas(0.35)) partes.splice(1 + Math.floor(azar() * partes.length), 0, rellenar(uno(NEUTRAS)))
  if (quizas(0.25)) partes.splice(Math.floor(azar() * (partes.length + 1)), 0, uno(NEGACIONES))
  return [unir(partes), etiquetas]
}

function unir(partes) {
  let t = partes[0]
  for (let i = 1; i < partes.length; i++) t += uno(CONECTORES) + partes[i]
  t = t.charAt(0).toUpperCase() + t.slice(1)
  return quizas(0.7) ? t + '.' : t
}

const lineas = []
const anadir = (texto, etiquetas) => lineas.push(`${texto.replace(/\t|\n/g, ' ')}\t${[...etiquetas].sort().join(',')}`)

// 1) Cada frase suelta, varias veces y con adornos (lo que ve el modelo en cada trozo).
for (const g of FRASES) for (const f of g[1]) for (let k = 0; k < 4; k++) anadir(uno(ADORNOS) + rellenar(f), g[0])
// 2) Introducciones, neutras y negaciones sueltas: sin necesidades.
for (let k = 0; k < 6; k++) {
  for (const f of INTRO_FAMILIAR) anadir(rellenar(f), ['FAMILIAR'])
  for (const f of [...INTRO_PROPIA, ...NEUTRAS, ...NEGACIONES]) anadir(rellenar(f), [])
}
// 3) Relatos combinados.
for (let k = 0; k < 14000; k++) {
  const [t, e] = relato()
  anadir(t, e)
}

const salida = join(aqui, 'datos', 'entrenamiento.tsv')
mkdirSync(dirname(salida), { recursive: true })
writeFileSync(salida, lineas.join('\n') + '\n', 'utf8')
const frases = FRASES.reduce((s, g) => s + g[1].length, 0)
console.log(`${lineas.length} ejemplos (${frases} frases base en ${FRASES.length} grupos) → ${salida}`)
