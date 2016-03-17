package de.vinode.henne.pollbot.Background;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.vinode.henne.pollbot.Background.Logic.Targets.Studienkonto;
import de.vinode.henne.pollbot.Data.Environment;
import de.vinode.henne.pollbot.Data.Grade;
import de.vinode.henne.pollbot.MyActivity;
import de.vinode.henne.pollbot.R;

public class PollService extends Service {

    private NotificationManager mNotificationManager;
    private Timer mTimer = new Timer();
    private int m_lastupdate;
    int m_time_interval;
    private static boolean isRunning = false;
    private Studienkonto m_studienkonto;

    private List<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;
    public static final int MSG_SET_TIME_INTERVAL = 3;
    public static final int MSG_PERSISTENT_LIST = 4;
    public static final int MSG_NEW_LOGIN = 5;

    public PollService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOGTAG, "onBind");
        return mMessenger.getBinder();
    }


    final static String ACTION = "NotifyServiceAction";
    final static String STOP_SERVICE_BROADCAST_KEY = "StopServiceBroadcastKey";
    final static int RQS_STOP_SERVICE = 1;

    NotifyServiceReceiver notifyServiceReceiver;

    private final Messenger mMessenger = new Messenger(new IncomingMessageHandler()); // Target we publish for clients to send messages to IncomingHandler.
    private static final String LOGTAG = "PollService";

    /**
     * Handle incoming messages from MainActivity
     */
    private class IncomingMessageHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            Log.d(LOGTAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_TIME_INTERVAL:
                    m_time_interval = msg.arg1;
                    mTimer.cancel();
                    mTimer.scheduleAtFixedRate(new MyTask(), 0, m_time_interval);
                    break;
                case MSG_NEW_LOGIN:
                    Log.d(LOGTAG, "setting new login");
                    Bundle data = msg.getData();
                    Studienkonto.getInstance(getApplication()).set_login(
                            getApplication(),
                            data.getString(MyActivity.EXTRA_LOGIN),
                            data.getString(MyActivity.EXTRA_PASSWORD)
                    );
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class MyTask extends TimerTask {
        @Override
        public void run() {
            Log.i(LOGTAG, "start retrieving data." + Calendar.getInstance().getTime());
            try {
                LinkedHashMap<String, Grade> polled_list = m_studienkonto.poll();
                Log.d(LOGTAG, "got list: " + polled_list);
                LinkedHashSet<String> found = Environment.getInstance().verifyPersistentData(getApplication() , polled_list);
                if (found.size() > 0) {
                    for (String entry : found) {
                        showNotification_new_grade(polled_list.get(entry).id(), entry);
                        Log.d(LOGTAG, "new notification for: " + entry);
                    }
                } else {
                    Log.d(LOGTAG, "no new grades found.");
                }

            } catch (Throwable t) { //you should always ultimately catch all exceptions in timer tasks.
                Log.e("TimerTick", "Timer Tick Failed.", t);
            }
        }
    }

    private void sendMessageToUI(int intvaluetosend) {
        Iterator<Messenger> messengerIterator = mClients.iterator();
        while (messengerIterator.hasNext()) {
            Messenger messenger = messengerIterator.next();
            try {
                messenger.send(Message.obtain(null, MSG_SET_TIME_INTERVAL, intvaluetosend, 0));

                // Send data as a String
                Bundle bundle = new Bundle();
                bundle.putSerializable("gradesmap", Environment.getInstance().PERSISTENT_LIST());
                ;
                Message msg = Message.obtain(null, MSG_PERSISTENT_LIST);
                msg.setData(bundle);
                messenger.send(msg);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list.
                mClients.remove(messenger);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOGTAG, "Service Started.");
        if (m_time_interval < 300) {
            m_time_interval = 300;
        }
        mTimer.scheduleAtFixedRate(new MyTask(), 0, m_time_interval * 1000);
        Log.i(LOGTAG, "Initalized timer: " + m_time_interval);
        isRunning = true;
        m_studienkonto = Studienkonto.getInstance(getApplication());
        Environment.getInstance().readPersistentData(getApplication());
    }
    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION);
        registerReceiver(notifyServiceReceiver, intentFilter);

        // Send Notification
        String notificationTitle = "Demo of Notification!";
        String notificationText = "Course Website";
        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(myBlog));
        PendingIntent pendingIntent
                = PendingIntent.getActivity(getBaseContext(),
                0, myIntent,
                Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText).setSmallIcon(R.drawable.ic_launcher_for_note)
                .setContentIntent(pendingIntent).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification.flags = notification.flags
                | Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0, notification);

        return super.onStartCommand(intent, flags, startId);
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "Received start id " + startId + ": " + intent);
        return START_STICKY; // Run until explicitly stopped.
    }


    /**
     * Display a notification in the notification bar.
     */
    private void showNotification_new_grade(int _id_for_notification, String _message) {

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MyActivity.class), 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(this.getString(R.string.neue_note_erhaten))
                .setContentText(_message).setSmallIcon(R.drawable.ic_launcher_for_note)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOngoing(false)
                .setGroupSummary(true)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .setLights(Color.DKGRAY, 3000, 3000)
                .build();
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //notification.flags = notification.flags
        //        | Notification.DEFAULT_VIBRATE | Notification.FLAG_GROUP_SUMMARY;
        //notification.flags |= Notification.FLAG_AUTO_CANCEL;

        mNotificationManager.notify(_id_for_notification, notification);

    }


    public class NotifyServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            int rqs = arg1.getIntExtra(STOP_SERVICE_BROADCAST_KEY, 0);

            if (rqs == RQS_STOP_SERVICE) {
                stopSelf();
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                        .cancelAll();
            }
        }
    }

    public static class BootServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
                arg0.getApplicationContext().startService(new Intent(arg0, PollService.class));
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mNotificationManager != null)
            mNotificationManager.cancelAll(); // Cancel the persistent notification.
        Log.i(LOGTAG, "Service Stopped.");
        isRunning = false;
    }

}
