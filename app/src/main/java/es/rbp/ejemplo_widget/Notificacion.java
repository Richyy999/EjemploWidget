package es.rbp.ejemplo_widget;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import es.rbp.ejemplo_widget.recivers.EnviarAccionAServicio;
import es.rbp.ejemplo_widget.servicios.ServicioContador;

/**
 * @author Ricardo Bordería Pi
 * <p>
 * Para mantener un servicio vivo aunque el Activity que lo empezo sea destruida se necesita de una notificación que mantenga su ciclo de vida.
 * Esta clase se encarga de la creación y actualización de la notificación. Es una notificación con tres funciones:
 * <p>
 * 1.- Puede reanudar el contador
 * <p>
 * 2.- Puede pausar el contador
 * <p>
 * 3.- Muestra el segundo actual del contador
 */
public class Notificacion {

    /**
     * El ID de la notificación
     */
    public static final int FOREGROUND_ID = 1;

    /**
     * Nombre del canal en el que se crea la notificación
     */
    public static final String CHANNEL_ID = "canalnotificacion";

    /**
     * Instancia de la clase siguiendo el patrón singleton
     */
    private static Notificacion notificacion;

    /**
     * Contexto de la aplicación
     */
    private Context context;

    /**
     * Objeto con el layout personalizado que posee la notificación.
     * <p>
     * Mencionar que el aspecto de los botones por defecto puede verse alterada según el dispositivo en el que se ejecute la aplicación.
     */
    private RemoteViews layoutNotificacion;

    /**
     * Instancia de la clase {@link Notification} que contiene la notificación
     */
    private Notification notification;

    /**
     * Constructor por privado de la clase.
     * <p>
     * En él se crea la notificación que utilizarña el servicio {@link es.rbp.ejemplo_widget.servicios.ServicioContador}
     *
     * @param context contexto de la aplicación
     */
    private Notificacion(Context context) {
        this.context = context.getApplicationContext();

        // Crea el intent para abrir MainActivity. Se activa cuando el usuario pulse la notificación.
        // Hay que añadir la propiedad android:launchMode="singleTop" en la etiqueta del activity que se quiere iniciar.
        // Si no se añade, se crearán varias ventanas una encima de otra, pero sigue funcionando aunque no se añada esta propiedad
        Intent cargarActivityIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingCargarActivityIntent = PendingIntent.getActivity(context, EnviarAccionAServicio.REQUEST_CODE,
                cargarActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent para detener el servicio cuando se elimine la notificación
        Intent deleteIntent = new Intent(context, EnviarAccionAServicio.class).setAction(ServicioContador.ACCION_PARAR);
        PendingIntent pendingIntentDelete = PendingIntent.getBroadcast(context, EnviarAccionAServicio.REQUEST_CODE,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent para reanudar la cuenta. Se ejecuta cuendo el usuario pulsa el botón de Reanudar
        Intent intentReanudar = new Intent(context, EnviarAccionAServicio.class).setAction(ServicioContador.ACCION_REANUDAR);
        PendingIntent pendingIntentReanudar = PendingIntent.getBroadcast(context, EnviarAccionAServicio.REQUEST_CODE,
                intentReanudar, PendingIntent.FLAG_UPDATE_CURRENT);

        // Intent para pausar la cuenta del servicio. Se ejecuta cuendo el usuario pulsa sobre el botón de Pausar
        Intent intentPsausar = new Intent(context, EnviarAccionAServicio.class).setAction(ServicioContador.ACCION_PAUSAR);
        PendingIntent pendingIntentPausar = PendingIntent.getBroadcast(context, EnviarAccionAServicio.REQUEST_CODE,
                intentPsausar, PendingIntent.FLAG_UPDATE_CURRENT);

        this.layoutNotificacion = new RemoteViews(context.getPackageName(), R.layout.notificacion);
        this.layoutNotificacion.setOnClickPendingIntent(R.id.btnEmpezarNotificacion, pendingIntentReanudar);
        this.layoutNotificacion.setOnClickPendingIntent(R.id.btnPararNotificacion, pendingIntentPausar);


        this.notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContent(this.layoutNotificacion)
                .setContentTitle(context.getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDeleteIntent(pendingIntentDelete)
                .setContentIntent(pendingCargarActivityIntent)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.clock)
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(FOREGROUND_ID, this.notification);
    }

    /**
     * Método que devuelve la instancia de la clase. Si no existe ninguna, crea la nueva instancia
     *
     * @param context contexto de la aplicación
     * @return Instancia de la clase
     * @see Notificacion#Notificacion(Context)
     */
    public static Notificacion crearNotificacion(Context context) {
        if (notificacion == null)
            notificacion = new Notificacion(context);

        return notificacion;
    }

    /**
     * Actualiza el contador de la notificación con el segundo que indica el servicio
     *
     * @param segundoActual segundo de la cuenta del servicio
     */
    public void actualizarContador(int segundoActual) {
        notificacion.layoutNotificacion.setTextViewText(R.id.lblContadorNotificacion, String.valueOf(segundoActual));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(FOREGROUND_ID, notificacion.notification);
    }

    /**
     * Método getter de la notificación de la clase
     *
     * @return notificación de la clase
     * @see Notificacion#notification
     */
    public Notification getNotification() {
        return notificacion.notification;
    }
}
