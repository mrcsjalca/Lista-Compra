package org.insbaixcamp.cestadecompra

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Utilidad sencilla para gestionar notificaciones locales en el móvil.
 * Muestra una notificación emergente en la barra de estado cuando recibes una invitación a una lista.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "canal_invitaciones"
    private const val CHANNEL_NAME = "Invitaciones de Listas"

    /**
     * Crea el canal de notificación (necesario a partir de Android 8.0).
     */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos al recibir invitaciones para colaborar en listas de compra"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(canal)
        }
    }

    /**
     * Muestra la notificación en el teléfono indicando quién invitó y a qué lista.
     */
    fun lanzarNotificacion(context: Context, deNombre: String, listaNombre: String, notifId: Int) {
        crearCanal(context)

        // Abre la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Invitación a lista de compra")
            .setContentText("$deNombre te ha invitado a \"$listaNombre\"")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$deNombre te ha invitado a unirte a la lista \"$listaNombre\". Entra para aceptar o rechazar.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Si el usuario aún no concedió permiso de notificación en Android 13+, la app continúa sin cerrarse
        }
    }
}
