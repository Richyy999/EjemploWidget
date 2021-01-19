package es.rbp.ejemplo_widget.servicios;

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
     * Instacia de {@link Notificacion} para manejar la notificación del servicio
     */
    private Notificacion notificacion;

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
     * Indica si las acciones sobre la cuenta están habilitadas
     */
    private boolean estaHabilitado;
    /**
     * Indica si el servicio está empezado o no
     */
    private boolean servicioEmpezado;

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
    public void onCreate() {
        super.onCreate();
        handler = new Handler();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String accion = intent.getStringExtra(EnviarAccionAServicio.ACCION_EXTRA);
                assert accion != null;
                switch (accion) {
                    case ACCION_REANUDAR:
                        play();
                        break;
                    case ACCION_PAUSAR:
                        pause();
                        break;
                    case ACCION_PARAR:
                        stop();
                        break;
                }
            }
        };

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

        notificacion = Notificacion.crearNotificacion(this);
        crearCanal();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!servicioEmpezado) {
            registerReceiver(receiver, new IntentFilter(EnviarAccionAServicio.FILTRO_INTENT));
            Log.i("SERVICIO", "EMPEZADO");
            Toast.makeText(getApplicationContext(), "Empezado", Toast.LENGTH_SHORT).show();
        } else {
            Log.i("SERVICIO", "REANUDADO");
            Toast.makeText(getApplicationContext(), "Reanudado", Toast.LENGTH_SHORT).show();
        }

        servicioEmpezado = true;

        estaHabilitado = true;

        if (estado == ESTADO_PAUSADO || estado == ESTADO_DETENIDO)
            play();

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Envía el estado y el segundo actual a {@link WidgetProvider}
     */
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
     * @see Notificacion
     */
    private void crearCanal() {
        NotificationChannel channel = new NotificationChannel(Notificacion.CHANNEL_ID, "nombre", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Si el estado de la cuenta del servicio es {@link ServicioContador#ESTADO_DETENIDO} o {@link ServicioContador#ESTADO_PAUSADO} empieza la cuenta
     *
     * @see ServicioContador#ESTADO_PAUSADO
     */
    private void play() {
        if ((estado == ESTADO_PAUSADO || estado == ESTADO_DETENIDO) && estaHabilitado) {
            if (!servicioEmpezado)
                startService(new Intent(this, ServicioContador.class));

            handler.postDelayed(hiloContador, 1000);
            startForeground(Notificacion.FOREGROUND_ID, notificacion.getNotification());
            cambiarEstado(ESTADO_CORRIENDO);
        }
    }

    /**
     * Si el estado de la cuenta del servicio está en marcha, pausa la cuenta
     *
     * @see ServicioContador#ESTADO_PAUSADO
     * @see ServicioContador#ESTADO_CORRIENDO
     */
    public void pause() {
        if (estado == ESTADO_CORRIENDO && estaHabilitado) {
            estaHabilitado = false;
            handler.removeCallbacks(hiloContador);
            stopForeground(false);

            cambiarEstado(ESTADO_PAUSADO);

            Log.i("SERVICIO", "PAUSADO");
            Toast.makeText(getApplicationContext(), "Pausado", Toast.LENGTH_SHORT).show();
            habilitarBotones();
        }
    }

    /**
     * Detiene la cuenta, reinicia los valores, elimina {@link ServicioContador#receiver} y detiene el servicio.
     */
    public void stop() {
        handler.removeCallbacks(hiloContador);
        servicioEmpezado = false;
        segundoActual = 0;
        unregisterReceiver(receiver);
        cambiarEstado(ESTADO_DETENIDO);
        stopSelf();
        stopForeground(true);
        Log.i("SERVICIO", "PARADO");
        Toast.makeText(getApplicationContext(), "Parado", Toast.LENGTH_SHORT).show();
    }

    /**
     * Cambia el estado de la cuenta y lo notifica a los clientes a la escucha
     *
     * @param estado estado de la cuenta
     */
    private void cambiarEstado(int estado) {
        this.estado = estado;
        enviarEstadoBroadcast();

        if (llamada != null)
            llamada.actualizarEstado(estado);
    }

    /**
     * Pasado medio segundo, habilita los botones para evitar una sobrecarga de órdenes
     */
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
