package com.marbellup.fitin.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marbellup.fitin.MainActivity
import com.marbellup.fitin.R
import com.marbellup.fitin.data.AppNotification
import com.marbellup.fitin.data.Activity
import com.marbellup.fitin.data.Interaction
import com.marbellup.fitin.data.LinearModel
import com.marbellup.fitin.data.Profile
import com.marbellup.fitin.data.Recommender
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Scored
import com.marbellup.fitin.data.Store
import java.util.concurrent.TimeUnit

/**
 * Avisos proactivos: el modelo revisa las actividades y avisa SOLO de las que encajan
 * (probabilidad ≥ 70 %) y no ha visto, no ha descartado y en las que no está apuntada.
 */
object Alerts {
    const val THRESHOLD = 0.70
    const val EXTRA_ACTIVITY = "activityId"
    private const val CHANNEL = "recomendaciones"

    /** Catálogo que ve el modelo: el de ejemplo, lo creado y lo «publicado» en la demo. */
    fun catalog(store: Store): List<Activity> {
        val published = store.loadSet("published")
        return (SampleData.all + SampleData.upcoming.filter { it.id in published }).distinctBy { it.id }
    }

    /** La mejor actividad de la que aún no se ha avisado, si supera el umbral. */
    fun candidate(store: Store, model: LinearModel, profile: Profile, history: List<Interaction>): Scored? {
        val notified = store.loadSet("notified")
        val joined = store.loadSet("joined")
        val (_, disliked) = Recommender.tastes(history)
        return Recommender.rank(profile, catalog(store), model, history, known = SampleData.allKnown)
            .firstOrNull {
                it.probability >= THRESHOLD && it.activity.id !in notified &&
                    it.activity.id !in joined && it.activity.id !in disliked
            }
    }

    fun toInbox(s: Scored, title: String): AppNotification = AppNotification(
        id = "n_${s.activity.id}_${System.currentTimeMillis()}",
        kind = "match",
        title = title,
        body = "${s.activity.title} · ${s.activity.shortDate}. ${s.explanation}",
        eventId = s.activity.id,
        fromName = null,
        status = "pending",
        compat = s.probability,
    )

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Actividades que encajan contigo", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "La IA de Fit-In te avisa de actividades que encajan con tus necesidades y gustos"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Notificación del sistema que abre la ficha de la actividad. */
    fun post(context: Context, n: AppNotification) {
        if (!canPost(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ACTIVITY, n.eventId)
        val pending = PendingIntent.getActivity(
            context, n.eventId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(n.eventId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permiso retirado: el aviso queda en la bandeja de la app.
        }
    }

    /** Revisión periódica en segundo plano (cada 6 h), aunque la app esté cerrada. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecommendationWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("recomendaciones", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class RecommendationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = Store(applicationContext)
        if (!store.getBool("alerts", true)) return Result.success()
        val s = Alerts.candidate(store, store.loadModel(), store.loadProfile(), store.loadHistory()) ?: return Result.success()
        val n = Alerts.toInbox(s, "✨ Te puede encajar: ${s.activity.sport.label.lowercase()}")
        store.saveInbox(listOf(n) + store.loadInbox())
        store.saveSet("notified", store.loadSet("notified") + s.activity.id)
        Alerts.post(applicationContext, n)
        return Result.success()
    }
}
