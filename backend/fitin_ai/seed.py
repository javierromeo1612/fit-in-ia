"""Datos de ejemplo para la demo.

TODO ES FICTICIO: instalaciones (con nombres genéricos), personas, reseñas y cifras.
Las fechas se calculan respecto al día de hoy para que «hoy por la tarde» siempre
tenga algo que encontrar durante la demo. Los ids de las seis actividades originales
coinciden con los de SampleData en la app Android.
"""

from __future__ import annotations

from datetime import date, datetime, time, timedelta

# Coordenadas aproximadas de zonas de Marbella (centroides, no direcciones reales).
ZONES = {
    "Zona centro": (36.5101, -4.8826),
    "Zona oeste": (36.4890, -4.9560),
    "Zona este": (36.4985, -4.8290),
    "Zona norte": (36.5265, -4.8935),
    "Sierra": (36.5420, -4.8720),
}


def _at(today: date, offset: int, hhmm: str) -> datetime:
    h, m = (int(x) for x in hhmm.split(":"))
    return datetime.combine(today + timedelta(days=offset), time(h, m))


def _next_weekday(today: date, weekday: int) -> int:
    """Días hasta el próximo `weekday` (0 = lunes). Si hoy lo es, cuenta hoy."""
    return (weekday - today.weekday()) % 7


def build(today: date | None = None) -> dict:
    today = today or date.today()
    sat = _next_weekday(today, 5)
    sun = _next_weekday(today, 6)

    facilities = [
        {
            "id": "f_piscina", "name": "Piscina cubierta municipal", "zone": "Zona centro",
            "lat": 36.5112, "lon": -4.8851, "covered": True,
            "features": ["SinEscalones", "Aparcamiento", "Vestuario", "Grua", "Cubierta"],
            "seal": "Ayuntamiento", "verified_on": "28 de octubre de 2026",
            "open": ("08:00", "21:00"), "sports": ["Natacion"],
        },
        {
            "id": "f_pabellon", "name": "Pabellón polideportivo", "zone": "Zona oeste",
            "lat": 36.4902, "lon": -4.9530, "covered": True,
            "features": ["SinEscalones", "Aparcamiento", "Vestuario", "Cubierta", "Bucle"],
            "seal": "Asociacion", "verified_on": "2 de noviembre de 2026",
            "open": ("09:00", "22:00"), "sports": ["Baloncesto", "Padel"],
        },
        {
            "id": "f_padel_cubierto", "name": "Club de pádel cubierto", "zone": "Zona este",
            "lat": 36.4995, "lon": -4.8320, "covered": True,
            "features": ["SinEscalones", "Aparcamiento", "Vestuario", "Cubierta"],
            "seal": "Asociacion", "verified_on": "12 de noviembre de 2026",
            "open": ("09:00", "22:00"), "sports": ["Padel"],
        },
        {
            "id": "f_padel_parque", "name": "Pistas de pádel del parque", "zone": "Zona norte",
            "lat": 36.5251, "lon": -4.8912, "covered": False,
            "features": ["SinEscalones"],
            "seal": "Ninguno", "verified_on": "",
            "open": ("08:00", "21:00"), "sports": ["Padel"],
        },
        {
            "id": "f_tenis", "name": "Pistas de tenis municipales", "zone": "Zona centro",
            "lat": 36.5089, "lon": -4.8790, "covered": False,
            "features": ["SinEscalones", "Aparcamiento"],
            "seal": "Asociacion", "verified_on": "20 de octubre de 2026",
            "open": ("08:00", "21:00"), "sports": ["Tenis"],
        },
        {
            "id": "f_golf", "name": "Campo de golf de par 3", "zone": "Zona norte",
            "lat": 36.5302, "lon": -4.8990, "covered": False,
            "features": ["SinEscalones", "Tranquilo"],
            "seal": "Ninguno", "verified_on": "",
            "open": ("09:00", "19:00"), "sports": ["Golf"],
        },
        {
            "id": "f_paseo", "name": "Carril bici del paseo marítimo", "zone": "Zona este",
            "lat": 36.5040, "lon": -4.8550, "covered": False,
            "features": ["SinEscalones", "Aparcamiento", "Vestuario"],
            "seal": "Ayuntamiento", "verified_on": "15 de octubre de 2026",
            "open": ("08:00", "20:00"), "sports": ["Ciclismo"],
        },
        {
            "id": "f_sendero", "name": "Sendero de la sierra", "zone": "Sierra",
            "lat": 36.5420, "lon": -4.8720, "covered": False,
            "features": [],
            "seal": "Asociacion", "verified_on": "10 de octubre de 2026",
            "open": ("08:00", "18:00"), "sports": ["Senderismo"],
        },
    ]

    def ev(**kw):
        base = dict(
            level="todos", duration_min=60, reviews=[], group=[], volunteers=[],
            min_participants=2, participants=[], off_peak=False,
            how_to_arrive="", meeting_note="", confirmations=0,
        )
        base.update(kw)
        return base

    samir = {"name": "Samir", "backed_by": "Club deportivo de ejemplo", "training": "Formación en deporte inclusivo", "sessions": 21}
    rocio = {"name": "Rocío", "backed_by": "Asociación de ejemplo", "training": "Formación en apoyo en el agua", "sessions": 14}
    marta = {"name": "Marta", "backed_by": "Asociación de ejemplo", "training": "Formación en guía de montaña", "sessions": 9}

    events = [
        ev(
            id="padel_inclusivo", sport="Padel", facility_id="f_padel_cubierto",
            start=_at(today, 0, "18:30"), duration_min=90, level="intermedio",
            title="Pádel inclusivo, nivel intermedio",
            description="Partido de pádel mixto en pista cubierta y ancha. Se juega de pie o en silla; la red y las bolas se adaptan si hace falta.",
            features=["Material", "Monitor"], capacity=4, min_participants=4,
            participants=["marcos", "ana"],
            group=[{"name": "Marcos", "how": "Juega en silla, busca pareja"}, {"name": "Ana", "how": "Nivel intermedio, viene sola"}],
            volunteers=[samir], confirmations=7,
            reviews=[{"author": "Marcos", "date": "15 de noviembre", "text": "Pista ancha y puerta sin escalón. El vestuario adaptado está junto a la pista."}],
            how_to_arrive="Entrada a nivel de calle. Dos plazas reservadas en la puerta.",
            meeting_note="Se queda en la pista 3 diez minutos antes.",
        ),
        ev(
            id="padel_parque", sport="Padel", facility_id="f_padel_parque",
            start=_at(today, 1, "10:00"), level="iniciacion",
            title="Pádel para empezar",
            description="Primeros pasos en pádel al aire libre, con monitor y sin prisas.",
            features=["Monitor", "GrupoReducido"], capacity=4, participants=["bea", "jorge", "lola"],
            group=[{"name": "Bea", "how": "Empieza este mes"}, {"name": "Jorge", "how": "Viene con su hermana Lola"}],
            confirmations=2, off_peak=True,
            how_to_arrive="Acceso a nivel desde el parque.", meeting_note="Se queda junto a la pista 1.",
        ),
        ev(
            id="baloncesto", sport="Baloncesto", facility_id="f_pabellon",
            start=_at(today, sat, "11:00"), level="todos",
            title="Baloncesto en silla, equipo mixto",
            description="Partido de baloncesto en silla abierto a todo el mundo. Prestan sillas deportivas: juega quien quiera, la use a diario o no.",
            features=["Material", "Signos", "Monitor"], capacity=12, min_participants=8,
            participants=["dani", "lola", "kike", "sergio", "u_x1", "u_x2", "u_x3", "u_x4", "u_x5", "u_x6"],
            group=[{"name": "Dani", "how": "Capitán del equipo"}, {"name": "Lola y Kike", "how": "Hermanos, juegan juntos"}, {"name": "Sergio", "how": "Intérprete de lengua de signos"}],
            volunteers=[samir], confirmations=14,
            reviews=[{"author": "Dani", "date": "8 de noviembre", "text": "Prestan sillas deportivas. Juega quien quiera, use silla a diario o no."}],
            how_to_arrive="Rampa en la entrada principal. Ascensor hasta la pista.",
            meeting_note="El equipo queda en la pista quince minutos antes.",
        ),
        ev(
            id="natacion", sport="Natacion", facility_id="f_piscina",
            start=_at(today, 1, "11:00"), level="iniciacion",
            title="Natación en grupo",
            description="Natación tranquila en calle reservada. Cada uno va a su ritmo y el monitor ayuda a entrar y salir del agua.",
            features=["Monitor", "Acompanante", "GrupoReducido", "Material"], capacity=8, min_participants=3,
            participants=["paco", "ines", "carla", "tomas", "elena"],
            group=[{"name": "Paco", "how": "Viene todas las semanas"}, {"name": "Inés y su hija Carla", "how": "Vienen juntas"}, {"name": "Tomás", "how": "Primera vez"}, {"name": "Elena", "how": "Vecina del barrio"}],
            volunteers=[rocio, {"name": "Andrés", "backed_by": "Club de natación de ejemplo", "training": "Formación en apoyo en el agua", "sessions": 6}],
            confirmations=12, off_peak=True,
            reviews=[
                {"author": "Paco", "date": "5 de noviembre", "text": "La grúa funciona y el socorrista sabe usarla. Entré al agua en dos minutos."},
                {"author": "Inés", "date": "22 de octubre", "text": "El vestuario adaptado tiene camilla. La puerta pesa un poco."},
            ],
            how_to_arrive="Entrada por la puerta lateral, a nivel de calle. Dos plazas reservadas junto a la entrada.",
            meeting_note="El grupo queda en la entrada diez minutos antes.",
        ),
        ev(
            id="ciclismo", sport="Ciclismo", facility_id="f_paseo",
            start=_at(today, sat, "12:00"),
            title="Ciclismo adaptado en el paseo",
            description="Paseo en bicicleta adaptada por el paseo marítimo: handbike, triciclo o tándem con guía. Ritmo tranquilo y en buena compañía.",
            features=["Material", "Monitor", "Acompanante", "Guia"], capacity=8, participants=["nuria", "alvaro", "u_x7", "u_x8"],
            group=[{"name": "Nuria", "how": "Viene con su pareja"}, {"name": "Álvaro", "how": "Viene con dos amigos"}],
            volunteers=[samir], confirmations=9,
            reviews=[{"author": "Nuria", "date": "1 de noviembre", "text": "Carril ancho y sin escalones. Nos prestaron una handbike y alguien nos acompañó todo el paseo."}],
            how_to_arrive="Salida junto al paseo marítimo, a nivel de calle. Hay bicicletas adaptadas para prestar.",
            meeting_note="El grupo queda en el punto de salida diez minutos antes.",
        ),
        ev(
            id="tenis", sport="Tenis", facility_id="f_tenis",
            start=_at(today, sat, "10:00"),
            title="Tenis para todos",
            description="Tenis adaptado con monitor, en silla de ruedas o de pie. Pista ancha y pelotas de bote bajo para quien las necesite.",
            features=["Material", "Monitor"], capacity=4, participants=["carmen", "luis"],
            group=[{"name": "Carmen", "how": "Juega en pareja"}, {"name": "Luis", "how": "Busca pareja"}],
            volunteers=[samir], confirmations=4, off_peak=True,
            reviews=[{"author": "Carmen", "date": "25 de octubre", "text": "Pista ancha y monitor atento. El baño adaptado está en otro edificio."}],
            how_to_arrive="Acceso a nivel desde el aparcamiento.", meeting_note="Se queda en la pista 2.",
        ),
        ev(
            id="golf", sport="Golf", facility_id="f_golf",
            start=_at(today, 2, "18:00"),
            title="Golf adaptado",
            description="Golf tranquilo en grupo pequeño, con buggy adaptado y sin prisas. El monitor explica cada golpe con calma.",
            features=["GrupoReducido", "LecturaFacil", "Material", "Monitor"], capacity=8, participants=["sofia", "hugo"],
            group=[{"name": "Sofía", "how": "Viene con su madre"}, {"name": "Hugo", "how": "Viene solo"}],
            confirmations=3, off_peak=True,
            reviews=[{"author": "Sofía", "date": "29 de octubre", "text": "Poca gente y mucha calma. Prestan buggy adaptado y el monitor explica cada golpe."}],
            how_to_arrive="Acceso a nivel desde el aparcamiento. El buggy adaptado espera en la entrada.",
            meeting_note="El monitor espera en la salida del primer hoyo diez minutos antes.",
        ),
        ev(
            id="senderismo", sport="Senderismo", facility_id="f_sendero",
            start=_at(today, sun, "09:00"), duration_min=240,
            title="Senderismo con silla de montaña",
            description="Ruta por la sierra con silla de montaña. El grupo se turna para llevarla y todos llegan juntos al mirador.",
            features=["Material", "Acompanante", "Transporte", "Guia"], capacity=12, participants=["julian", "pilar", "u_x9", "u_x10"],
            group=[{"name": "Julián", "how": "Viene con su hijo"}, {"name": "Pilar", "how": "Camina con guía"}],
            volunteers=[marta], confirmations=6,
            reviews=[{"author": "Julián", "date": "12 de octubre", "text": "Cuatro personas llevan la silla por turnos. Llegamos al mirador todos."}],
            how_to_arrive="Autobús adaptado desde el centro. El sendero no tiene baño.",
            meeting_note="El autobús sale a las 9:00 en punto.",
        ),
    ]

    def user(uid, name, zone, sports, level, needs, prefs, avail, discoverable=True, jitter=(0.0, 0.0)):
        lat, lon = ZONES[zone]
        return {
            "id": uid, "name": name, "zone": zone, "lat": lat + jitter[0], "lon": lon + jitter[1],
            "sports": sports, "level": level, "needs": needs, "preferences": prefs,
            "availability": avail, "discoverable": discoverable,
        }

    users = [
        user("lucia", "Lucía", "Zona centro", ["Natacion", "Padel"], "intermedio",
             ["SinEscalones", "Vestuario", "Aparcamiento"],
             "Me gusta el deporte tranquilo en grupo, mejor por la tarde y en sitios cubiertos.",
             ["tarde"], jitter=(0.0045, -0.0035)),
        user("raul", "Raúl", "Zona este", ["Padel", "Tenis"], "intermedio", ["SinEscalones"],
             "Busco gente para jugar pádel nivel medio por las tardes, mejor en pista cubierta.", ["tarde", "noche"], jitter=(0.003, 0.004)),
        user("irene", "Irene", "Zona centro", ["Padel"], "intermedio", [],
             "Juego pádel desde hace dos años, me apetece un grupo mixto e inclusivo entre semana por la tarde.", ["tarde"], jitter=(-0.002, 0.003)),
        user("luis", "Luis", "Zona centro", ["Tenis", "Padel"], "intermedio", ["Vestuario"],
             "Busco pareja para raqueta: tenis o pádel. Prefiero tardes.", ["tarde", "manana"], jitter=(0.004, 0.002)),
        user("bea", "Bea", "Zona norte", ["Padel"], "iniciacion", [],
             "Estoy empezando con el pádel, me viene bien por las mañanas.", ["manana"]),
        user("carmen", "Carmen", "Zona centro", ["Tenis", "Padel"], "intermedio", ["SinEscalones"],
             "Juego al tenis en pareja y me gustaría probar pádel por la tarde.", ["tarde"], discoverable=False),
        user("marcos", "Marcos", "Zona este", ["Padel", "Baloncesto"], "intermedio", ["SinEscalones", "Vestuario", "Material"],
             "Juego en silla. Pádel cubierto por las tardes, nivel intermedio.", ["tarde"], jitter=(-0.003, 0.002)),
        user("ana", "Ana", "Zona este", ["Padel"], "intermedio", [],
             "Pádel por las tardes, me da igual si es cubierto.", ["tarde"]),
        user("tomas", "Tomás", "Zona centro", ["Natacion"], "iniciacion", ["Monitor", "Acompanante"],
             "Natación tranquila con alguien que me ayude a entrar al agua.", ["manana"]),
        user("hugo", "Hugo", "Zona norte", ["Golf", "Senderismo"], "iniciacion", ["Tranquilo"],
             "Deportes tranquilos al aire libre y con poca gente.", ["tarde"]),
        user("nuria", "Nuria", "Zona este", ["Ciclismo"], "todos", ["Material", "Acompanante"],
             "Paseos en bici adaptada el fin de semana.", ["manana"]),
        user("pilar", "Pilar", "Sierra", ["Senderismo"], "todos", ["Guia"],
             "Rutas de montaña con guía, los domingos.", ["manana"]),
        user("dani", "Dani", "Zona oeste", ["Baloncesto"], "avanzado", ["Material"],
             "Baloncesto en silla, competitivo pero buen rollo.", ["manana", "tarde"]),
        user("sergio", "Sergio", "Zona oeste", ["Baloncesto", "Natacion"], "intermedio", [],
             "Intérprete de lengua de signos, me gusta acompañar en actividades de grupo.", ["manana", "tarde"]),
    ]

    return {"facilities": facilities, "events": events, "users": users}
