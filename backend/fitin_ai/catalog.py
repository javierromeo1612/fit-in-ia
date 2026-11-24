"""Vocabulario cerrado que comparten backend, prompts y app Android.

Los códigos coinciden EXACTAMENTE con los enums de Kotlin (`Sport` y `Need` en
app/src/main/java/com/marbellup/fitin/data/Model.kt). Si añadís uno aquí, añadidlo allí.
"""

SPORTS: dict[str, str] = {
    "Baloncesto": "Baloncesto",
    "Natacion": "Natación",
    "Tenis": "Tenis",
    "Padel": "Pádel",
    "Golf": "Golf",
    "Senderismo": "Senderismo",
    "Ciclismo": "Ciclismo",
}

# Principio 1 del producto: se pregunta qué necesitas, nunca qué tienes.
NEEDS: dict[str, str] = {
    "SinEscalones": "Acceso sin escalones",
    "Aparcamiento": "Aparcamiento reservado",
    "Vestuario": "Vestuario y baño adaptados",
    "Grua": "Grúa de piscina",
    "Transporte": "Transporte hasta allí",
    "Cubierta": "Instalación cubierta",
    "Signos": "Lengua de signos",
    "Bucle": "Bucle magnético",
    "Guia": "Guía para personas ciegas",
    "Tranquilo": "Entorno tranquilo",
    "LecturaFacil": "Explicaciones sencillas",
    "Pictogramas": "Pictogramas",
    "GrupoReducido": "Grupo pequeño",
    "Material": "Material adaptado",
    "Monitor": "Monitor de apoyo",
    "Acompanante": "Alguien que me acompañe",
}

# Necesidades que dependen de la instalación física (las demás, de la actividad).
FACILITY_NEEDS = {"SinEscalones", "Aparcamiento", "Vestuario", "Grua", "Cubierta", "Bucle", "Tranquilo"}

LEVELS = ["iniciacion", "intermedio", "avanzado"]
DAYS = ["hoy", "manana", "esta_semana", "fin_de_semana", "cualquiera"]
SLOTS = ["manana", "mediodia", "tarde", "noche", "cualquiera"]
COMPANY = ["sola", "con_grupo", "con_acompanante", "indiferente"]

SLOT_HOURS = {
    "manana": (7, 13),
    "mediodia": (13, 16),
    "tarde": (16, 20),
    "noche": (20, 24),
}
