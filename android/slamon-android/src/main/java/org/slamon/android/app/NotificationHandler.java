package org.slamon.android.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.slamon.android.R;
import org.slamon.android.SLAMonAndroidAgentService;

/**
 * SLAMon application notification handler
 */
public class NotificationHandler implements MessageHandler {
    private static final String TAG = NotificationHandler.class.getCanonicalName();
    private static final int NOTIFICATION_ID = 1;
    private Class<? extends SlamonActivity> activityClass;

    public NotificationHandler(Class<? extends SlamonActivity> activityClass) {
        this.activityClass = activityClass;
    }

    @Override
    public void onMessage(Context context, Bundle bundle) {
        String message = bundle.getString("alert");
        if (message == null) {
            Log.i(TAG, "Received a push notification with no message");
            return;
        }
        Log.i(TAG, "Received message push notification: " + message);

        // Send as intent broadcast
        sendBroadcast(context, message);

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.slamon_icon)
                .setContentTitle("SLAMon Notifications")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();

        // Send as notification
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public void onDeleteMessage(Context context, Bundle bundle) {
        Log.i(TAG, "Received delete message push notification");
    }

    @Override
    public void onError() {
        Log.i(TAG, "Received error push notification");
    }

    private void sendBroadcast(Context context, String message) {
        Intent intent = new Intent().setAction(SLAMonAndroidAgentService.IntentConstants.PUSH_NOTIFICATION)
                .putExtra("message", message);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
