package de.vinode.henne.pollbot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

import de.vinode.henne.pollbot.Background.PollService;
import de.vinode.henne.pollbot.Data.Environment;
import de.vinode.henne.pollbot.Data.Grade;

public class GradesList extends AppCompatActivity implements ServiceConnection {

    private Messenger mServiceMessenger =  null;
    private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
    private ServiceConnection mConnection = this;
    private static final String LOGTAG = "GradesList";

    boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grades_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        doBindService();
    }

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // Log.d(LOGTAG,"IncomingHandler:handleMessage");
            switch (msg.what) {

                case PollService.MSG_PERSISTENT_LIST:
                    updateText();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        mServiceMessenger = null;
    }


    private void updateText() {
        LinkedHashMap<String, Grade> gradesmap = Environment.getInstance().PERSISTENT_LIST();
        if (gradesmap == null || gradesmap.size() == 0)
            return;

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Grade> entry : gradesmap.entrySet()) {
            builder.append(entry.getKey()).append(": " +"\t").append(entry.getValue().grade()).append(System.lineSeparator());
        }

        //textStrValue.setText(builder);
        TextView text = (TextView) findViewById(R.id.grades_list_textview);
        text.setText(builder);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceMessenger = new Messenger(service);
        updateText();
        try {
            Message msg = Message.obtain(null, PollService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even do anything with it
        }
    }

    private void doBindService() {
        bindService(new Intent(this, PollService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e(LOGTAG, "Failed to unbind from the service", t);
        }
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, PollService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
}
