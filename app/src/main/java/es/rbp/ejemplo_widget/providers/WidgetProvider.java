package es.rbp.ejemplo_widget.providers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import es.rbp.ejemplo_widget.MainActivity;
import es.rbp.ejemplo_widget.R;
import es.rbp.ejemplo_widget.recivers.EnviarAccionAServicio;
import es.rbp.ejemplo_widget.servicios.ServicioContador;

public class WidgetProvider extends AppWidgetProvider {

    /**
     * Código de request del WidgetProvider
     */
    public static final int REQUEST_CODE = 1;

    /**
     * Segundo en el que se encuantra la cuenta del servicio
     */
    private static int segundoActual = ServicioContador.SEGUNDO_POR_DEFECTO;

    /**
     * Estado de la cuenta del servicio
     */
    private static int estadoServicio = ServicioContador.ESTADO_DETENIDO;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
            remoteViews.setTextViewText(R.id.lblContadorWidget, String.valueOf(segundoActual));

            // Lanza MainActivity
            Intent empezarActivityIntent = new Intent(context, MainActivity.class);
            PendingIntent empezarActivityPendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, empezarActivityIntent, 0);

            // Empieza la cuenta
            Intent intentEmpezarReanudar = new Intent(context, WidgetProvider.class).setAction(ServicioContador.ACCION_EMPEZAR);
            PendingIntent pendingIntentEmpezarReanudar = PendingIntent.getBroadcast(context, REQUEST_CODE,
                    intentEmpezarReanudar, PendingIntent.FLAG_UPDATE_CURRENT);

            // Pausa la cuenta
            Intent intentPausar = new Intent(context, EnviarAccionAServicio.class).setAction(ServicioContador.ACCION_PAUSAR);
            PendingIntent pendingIntentPausar = PendingIntent.getBroadcast(context, EnviarAccionAServicio.REQUEST_CODE,
                    intentPausar, PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.rootWidget, empezarActivityPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.btnEmpezarWidget, pendingIntentEmpezarReanudar);
            remoteViews.setOnClickPendingIntent(R.id.btnPausarWidget, pendingIntentPausar);

            if (estadoServicio == ServicioContador.ESTADO_DETENIDO)
                remoteViews.setTextViewText(R.id.btnEmpezarWidget, context.getString(R.string.empezar));
            else if (estadoServicio == ServicioContador.ESTADO_PAUSADO)
                remoteViews.setTextViewText(R.id.btnEmpezarWidget, context.getString(R.string.reanudar));

            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String accion = intent.getAction();
        if (accion.equals(ServicioContador.ACCION_EMPEZAR)) {
            if (estadoServicio == ServicioContador.ESTADO_DETENIDO) {
                Intent servicioIntent = new Intent(context, ServicioContador.class);
                context.startForegroundService(servicioIntent);
            } else if (estadoServicio == ServicioContador.ESTADO_PAUSADO)
                context.sendBroadcast(new Intent(EnviarAccionAServicio.FILTRO_INTENT)
                        .putExtra(EnviarAccionAServicio.ACCION_EXTRA, ServicioContador.ACCION_REANUDAR));
        } else if (accion.equals(ServicioContador.ACCION_ACTUALIZAR_DATOS)) {
            segundoActual = intent.getIntExtra(ServicioContador.EXTRA_ACTUALIZAR_SEGUNDOS, ServicioContador.SEGUNDO_POR_DEFECTO);
            estadoServicio = intent.getIntExtra(ServicioContador.EXTRA_ACTUALIZAR_ESTADO, ServicioContador.ESTADO_DETENIDO);
            Log.d("ESTADO WIDGET", String.valueOf(estadoServicio));
        }

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName esteWidget = new ComponentName(context, WidgetProvider.class);
        int[] appWidgetIds = manager.getAppWidgetIds(esteWidget);
        if (appWidgetIds != null && appWidgetIds.length > 0)
            onUpdate(context, manager, appWidgetIds);
    }
}
