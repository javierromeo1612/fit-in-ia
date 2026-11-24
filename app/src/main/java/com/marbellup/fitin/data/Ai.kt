package com.marbellup.fitin.data

/*
 * Tipos auxiliares de la IA del móvil: avisos y mensajes del asistente.
 * La IA no interpreta texto libre: todo se elige con opciones, para que el modelo reciba datos limpios.
 */

data class AppNotification(
    val id: String,
    /** match (actividad que encaja) | info */
    val kind: String,
    val title: String,
    val body: String,
    val eventId: String?,
    val fromName: String?,
    /** pending | opened | dismissed | info */
    val status: String,
    /** Probabilidad de encaje que calculó el modelo. */
    val compat: Double?,
)

data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val recommendations: List<Scored> = emptyList(),
)
