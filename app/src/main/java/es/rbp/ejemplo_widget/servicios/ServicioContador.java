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
     * Handler para manejar los hilos
     */
    private Handler handler;

    /**
     * Hilo que realiza el conteo y lo envía a las activities que lo escuchen
     */
    private Runnable hiloContador;

    private final IBinder binder = new LocalBinder();

    /**
     * Instancia para recibir los mensajes de broadcast de {@link ServicioAccionNotificacion} por parte de {@link Notificacion}
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
                String accion = intent.getStringExtra(ServicioAccionNotificacion.ACCION_EXTRA);
                assert accion != null;
                switch (accion) {
                    case Notificacion.ACCION_REANUDAR:
                        reanudarContador();
                        break;
                    case Notificacion.ACCION_PAUSAR:
                        pausarConteo();
                        break;
                    case Notificacion.ACCION_PARAR:
                        borrarServicio();
                        break;
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(ServicioAccionNotificacion.FILTRO_INTENT));

        final Notificacion notificacion = Notificacion.crearNotificacion(getApplicationContext());
        crearCanal(notificacion.getNotification());

        hiloContador = new Runnable() {
            @Override
            public void run() {
                segundoActual++;
                llamada.actualizarContador(segundoActual);
                notificacion.actualizarContador(segundoActual);
                handler.postDelayed(hiloContador, 1000);
            }
        };
        handler.postDelayed(hiloContador, 1000);

        estado = ESTADO_CORRIENDO;
        llamada.actualizarEstado(estado);

        return START_NOT_STICKY;
    }

    /**
     * Crea el canal para lanzar la notificación
     *
     * @param notification notificación que mantendrá vivo el servicio
     * @see Notificacion
     */
    private void crearCanal(Notification notification) {
        NotificationChannel channel = new NotificationChannel(Notificacion.CHANNEL_ID, "nombre", NotificationManager.IMPORTANCE_LOW);
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
        if (estado == ESTADO_PAUSADO) {
            Intent intent = new Intent(getApplicationContext(), ServicioContador.class);
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
        if (estado == ESTADO_CORRIENDO) {
            handler.removeCallbacks(hiloContador);
            stopForeground(false);
            estado = ESTADO_PAUSADO;
            llamada.actualizarEstado(estado);
            Log.i("SERVICIO", "PAUSADO");
            Toast.makeText(getApplicationContext(), "Pausado", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Si la cuenta del servicio está en marcha, detiene el conteo y reinicia el valor del {@link ServicioContador#SEGUNDO_POR_DEFECTO}.
     * <p>
     * Actualiza el estado de la cuenta del servidor a {@link ServicioContador#ESTADO_DETENIDO}
     */
    public void pararConteo() {
        if (estado == ESTADO_CORRIENDO) {
            handler.removeCallbacks(hiloContador);
            segundoActual = SEGUNDO_POR_DEFECTO;
            unregisterReceiver(receiver);
            stopForeground(true);
            estado = ESTADO_DETENIDO;
            llamada.actualizarEstado(estado);
            Log.i("SERVICIO", "DETENIDO");
            Toast.makeText(getApplicationContext(), "Detenido", Toast.LENGTH_SHORT).show();
        } else if (estado == ESTADO_PAUSADO) {
            estado = ESTADO_DETENIDO;
            llamada.actualizarEstado(estado);
            Log.i("SERVICIO", "DETENIDO");
            Toast.makeText(getApplicationContext(), "Detenido", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Detiene el servicio por completo
     */
    private void borrarServicio() {
        stopSelf();
        unregisterReceiver(receiver);
        Log.i("SERVICIO", "BORRADO POR NOTIFICACION");
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
