package es.rbp.ejemplo_widget.servicios;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import es.rbp.ejemplo_widget.MainActivity;
import es.rbp.ejemplo_widget.Notificacion;
import es.rbp.ejemplo_widget.providers.WidgetProvider;
import es.rbp.ejemplo_widget.recivers.EnviarAccionAServicio;

/**
 * Cuenta los segundos que psasn desde que se inció el servicio y los envía a {@link MainActivity} y a {@link Notificacion}
 *
 * @author Ricardo Bordería Pi
 */
public class ServicioContador extends Service {

    /**
     * Extra para el valor por defecto para el intent del contador
     */
    public static final int SEGUNDO_POR_DEFECTO = 0;

    /**
     * Estado de la cuenta del servicio que indica que la cuenta está pausada
     */
    public static final int ESTADO_PAUSADO = 1;
    /**
     * Estado de la cuenta del servicio que indica que la cuenta está en marcha
     */
    public static final int ESTADO_CORRIENDO = 2;
    /**
     * Estado de la cuenta del servicio que indica que la cuenta está detenida
     */
    public static final int ESTADO_DETENIDO = 3;

    /**
     * Acción indicando que empieze la cuenta
     */
    public static final String ACCION_EMPEZAR = "accion_empezar";

    /**
     * Acción indicando que pause la cuenta
     */
    public static final String ACCION_PAUSAR = "accion_pausar";
    /**
     * Acción indicando que reanude la marcha
     */
    public static final String ACCION_REANUDAR = "accion_reanudar";
    /**
     * Acción indicando que pare la cuenta y detenga el servicio
     */
    public static final String ACCION_PARAR = "accion_parar";
    /**
     * Acción indicando que se están enviando nuevos datos para actualizar las vistas
     */
    public static final String ACCION_ACTUALIZAR_DATOS = "accion_actualizar_datos";


    /**
     * Extra para indicar que se envía el segundo actual
     *
     * @see ServicioContador#segundoActual
     */
    public static final String EXTRA_ACTUALIZAR_SEGUNDOS = "extra_actualizar_segundos";
    /**
     * Extra para indicar que se envía el estado actual de la cuenta del servicio
     *
     * @see ServicioContador#estado
     */
    public static final String EXTRA_ACTUALIZAR_ESTADO = "extra_actualizar_estado";

    /**
     * Handler para manejar los hilos
     */
    private Handler handler;

    /**
     * Hilo que realiza el conteo y lo envía a las activities que lo escuchen
     */
    private Runnable hiloContador;

    private final IBinder binder = new LocalBinder();

    /**
     * Instancia para recibir los mensajes de broadcast de {@link EnviarAccionAServicio} por parte de {@link Notificacion}
     */
    private BroadcastReceiver receiver;

    /**
     * Instancia de {@link Llamada} para acceder a sus métodos y enviar la información
     */
    private Llamada llamada;

    /**
     * Segundo actual desde el inicio del servicio
     */
    private int segundoActual;

    /**
     * Estado actual de la cuenta del servicio.
     * <p>
     * Puede ser {@link ServicioContador#ESTADO_DETENIDO}, {@link ServicioContador#ESTADO_PAUSADO} o {@link ServicioContador#ESTADO_CORRIENDO}
     */
    private int estado = ESTADO_DETENIDO;

    private boolean estaHabilitado;

    /**
     * Esta clase devuelve la instancia del servicio
     *
     * @author Ricardo Bordería Pi
     */
    public class LocalBinder extends Binder {

        /**
         * Devuelve una instancia de {@link ServicioContador}
         *
         * @return {@link ServicioContador}
         */
        public ServicioContador getServiceInstance() {
            return ServicioContador.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (segundoActual == SEGUNDO_POR_DEFECTO) {
            Log.i("SERVICIO", "EMPEZADO");
            Toast.makeText(getApplicationContext(), "Empezado", Toast.LENGTH_SHORT).show();
        } else {
            Log.i("SERVICIO", "REANUDADO");
            Toast.makeText(getApplicationContext(), "Reanudado", Toast.LENGTH_SHORT).show();
        }

        handler = new Handler();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String accion = intent.getStringExtra(EnviarAccionAServicio.ACCION_EXTRA);
                assert accion != null;
                switch (accion) {
                    case ACCION_REANUDAR:
                        reanudarContador();
                        break;
                    case ACCION_PAUSAR:
                        pausarConteo();
                        break;
                    case ACCION_PARAR:
                        borrarServicio();
                        break;
                }
            }
        };

        try {
            registerReceiver(receiver, new IntentFilter(EnviarAccionAServicio.FILTRO_INTENT));
        } catch (Exception e) {
            Log.e("SERVICIO", e.toString());
        }
        final Notificacion notificacion = Notificacion.crearNotificacion(getApplicationContext());
        crearCanal(notificacion.getNotification());

        hiloContador = new Runnable() {
            @Override
            public void run() {
                segundoActual++;

                if (llamada != null)
                    llamada.actualizarContador(segundoActual);

                notificacion.actualizarContador(segundoActual);
                enviarEstadoBroadcast();
                handler.postDelayed(hiloContador, 1000);
            }
        };
        handler.postDelayed(hiloContador, 1000);

        estado = ESTADO_CORRIENDO;

        estaHabilitado = true;

        return START_NOT_STICKY;
    }

    private void enviarEstadoBroadcast() {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, WidgetProvider.class);
        intent.setAction(ACCION_ACTUALIZAR_DATOS);
        intent.putExtra(EXTRA_ACTUALIZAR_ESTADO, estado);
        intent.putExtra(EXTRA_ACTUALIZAR_SEGUNDOS, segundoActual);
        context.sendBroadcast(intent);
    }

    /**
     * Crea el canal para lanzar la notificación
     *
     * @param notification notificación que mantendrá vivo el servicio
     * @see Notificacion
     */
    private void crearCanal(Notification notification) {
        NotificationChannel channel = new NotificationChannel(Notificacion.CHANNEL_ID, "nombre", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            startForeground(Notificacion.FOREGROUND_ID, notification);
        }
    }

    /**
     * Si el estado de la cuenta del servicio está pausada, reanuda la cuenta.
     *
     * @see ServicioContador#ESTADO_PAUSADO
     */
    private void reanudarContador() {
        if (estado == ESTADO_PAUSADO && estaHabilitado) {
            Intent intent = new Intent(getApplicationContext(), ServicioContador.class);
            estaHabilitado = false;
            startForegroundService(intent);
            Log.i("SERVICIO", "REANUDADO");
            Toast.makeText(getApplicationContext(), "Reanudado", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Si el estado de la cuenta del servicio está en marcha, pausa la cuenta
     *
     * @see ServicioContador#ESTADO_PAUSADO
     * @see ServicioContador#ESTADO_CORRIENDO
     */
    public void pausarConteo() {
        if (estado == ESTADO_CORRIENDO && estaHabilitado) {
            estaHabilitado = false;
            handler.removeCallbacks(hiloContador);
            stopForeground(false);
            estado = ESTADO_PAUSADO;
            enviarEstadoBroadcast();

            if (llamada != null)
                llamada.actualizarEstado(estado);

            Log.i("SERVICIO", "PAUSADO");
            Toast.makeText(getApplicationContext(), "Pausado", Toast.LENGTH_SHORT).show();
            habilitarBotones();
        }
    }

    /**
     * Si la cuenta del servicio está en marcha, detiene el conteo y reinicia el valor del {@link ServicioContador#SEGUNDO_POR_DEFECTO}.
     * <p>
     * Actualiza el estado de la cuenta del servidor a {@link ServicioContador#ESTADO_DETENIDO}
     */
    public void pararConteo() {
        if (estado == ESTADO_CORRIENDO && estaHabilitado) {
            estaHabilitado = false;
            handler.removeCallbacks(hiloContador);
            segundoActual = SEGUNDO_POR_DEFECTO;
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                Log.e("SERVICIO", e.toString());
            }
            stopForeground(true);
            stopSelf();
            estado = ESTADO_DETENIDO;

            Log.i("SERVICIO", "DETENIDO");
            Toast.makeText(getApplicationContext(), "Detenido", Toast.LENGTH_SHORT).show();
        } else if (estado == ESTADO_PAUSADO && estaHabilitado) {
            estaHabilitado = false;
            estado = ESTADO_DETENIDO;

            Log.i("SERVICIO", "DETENIDO");
            Toast.makeText(getApplicationContext(), "Detenido", Toast.LENGTH_SHORT).show();
        }
        if (llamada != null)
            llamada.actualizarEstado(estado);

        enviarEstadoBroadcast();
    }

    /**
     * Detiene el servicio por completo
     */
    private void borrarServicio() {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.e("SERVICIO", e.toString());
        }
        estado = ESTADO_DETENIDO;
        segundoActual = SEGUNDO_POR_DEFECTO;
        enviarEstadoBroadcast();
        stopSelf();
        Log.i("SERVICIO", "BORRADO POR NOTIFICACION");
    }

    private void habilitarBotones() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                estaHabilitado = true;
            }
        }, 500);
    }

    /**
     * Indica al activity el estado de la cuenta del servicio, y el segundo actual de la misma
     *
     * @return segundo actual de la cuenta del servicio
     */
    public int cargarSegundo() {
        llamada.actualizarEstado(estado);
        return segundoActual;
    }

    /**
     * Registra la activity desde la que es llamado
     *
     * @param activity activity que implemente {@link Llamada}
     */
    public void registrarActivity(Llamada activity) {
        this.llamada = activity;
    }

    /**
     * Interfaz para enviar información desde el servicio hasta el activity que la implemente
     */
    public interface Llamada {
        /**
         * Envía el segundo actual
         *
         * @param segundoActual {@link ServicioContador#segundoActual}
         */
        void actualizarContador(int segundoActual);

        /**
         * Envía el estado actual de la cuenta del servicio
         *
         * @param estado estado actual de la cuenta.
         * @see ServicioContador#estado
         */
        void actualizarEstado(int estado);
    }
}
