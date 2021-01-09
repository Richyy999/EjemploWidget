package es.rbp.controlarservicedesdenotificacion;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import es.rbp.controlarservicedesdenotificacion.servicios.ServicioAccionNotificacion;

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
     * Acción que manda la notificación al servicio indicando que pause la cuenta
     */
    public static final String ACCION_PAUSAR = "accion_pausar";
    /**
     * Acción que manda la notificación al servicio indicando que reanude la marcha
     */
    public static final String ACCION_REANUDAR = "accion_reanudar";
    /**
     * Acción que manda la notificación al servicio indicando que pare la cuenta y detenga el servicio
     */
    public static final String ACCION_PARAR = "accion_parar";

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
     * En él se crea la notificación que utilizarña el servicio {@link es.rbp.controlarservicedesdenotificacion.servicios.ServicioContador}
     *
     * @param context contexto de la aplicación
     */
    private Notificacion(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.context = context.getApplicationContext();

            Intent deleteIntent = new Intent(context, ServicioAccionNotificacion.class).setAction(ACCION_PARAR);
            PendingIntent pendingIntentDelete = PendingIntent.getBroadcast(context, ServicioAccionNotificacion.REQUEST_CODE,
                    deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentEmpezar = new Intent(context, ServicioAccionNotificacion.class).setAction(ACCION_REANUDAR);
            PendingIntent pendingIntentEmpezar = PendingIntent.getBroadcast(context, ServicioAccionNotificacion.REQUEST_CODE,
                    intentEmpezar, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentParar = new Intent(context, ServicioAccionNotificacion.class).setAction(ACCION_PAUSAR);
            PendingIntent pendingIntentParar = PendingIntent.getBroadcast(context, ServicioAccionNotificacion.REQUEST_CODE,
                    intentParar, PendingIntent.FLAG_UPDATE_CURRENT);

            this.layoutNotificacion = new RemoteViews(context.getPackageName(), R.layout.notificacion);
            this.layoutNotificacion.setOnClickPendingIntent(R.id.btnEmpezarNotificacion, pendingIntentEmpezar);
            this.layoutNotificacion.setOnClickPendingIntent(R.id.btnPararNotificacion, pendingIntentParar);


            this.notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContent(this.layoutNotificacion)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setDeleteIntent(pendingIntentDelete)
                    .setSmallIcon(R.drawable.clock)
                    .build();

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(FOREGROUND_ID, this.notification);
        }
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
