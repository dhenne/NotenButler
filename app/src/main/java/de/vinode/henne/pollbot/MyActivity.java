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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

import de.vinode.henne.pollbot.Background.PollService;
import de.vinode.henne.pollbot.Data.Grade;

public class MyActivity extends AppCompatActivity implements View.OnClickListener, ServiceConnection {

    public final static String EXTRA_LOGIN = "de.vinode.henne.firstapp.LOGIN";
    public final static String EXTRA_PASSWORD = "de.vinode.henne.firstapp.PASSWORD";

    private Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
    private TextView textStatus, textIntValue, textStrValue;
    private Messenger mServiceMessenger = null;
    boolean mIsBound;

    private static final String LOGTAG = "MainActivity";
    private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());

    private ServiceConnection mConnection = this;

    /**
     * Called when the user clicks the Send button
     */
    public void sendLogin(View view) {
        //Intent intent = new Intent(this, DisplayMessageActivity.class);
        if (!mIsBound) {
            if (!PollService.isRunning()) {
                startService(new Intent(MyActivity.this, PollService.class));
            }
            doBindService();
        }
        EditText login = (EditText) findViewById(R.id.login_dialog);
        EditText password = (EditText) findViewById(R.id.password_dialog);
        Bundle data = new Bundle();
        data.putString(EXTRA_LOGIN, login.getText().toString());
        data.putString(EXTRA_PASSWORD, password.getText().toString());

        if (mIsBound) {
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, PollService.MSG_NEW_LOGIN);
                    msg.setData(data);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnBind = (Button) findViewById(R.id.btnBind);
        btnUnbind = (Button) findViewById(R.id.btnUnbind);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textIntValue = (TextView) findViewById(R.id.textIntValue);
        textStrValue = (TextView) findViewById(R.id.textStrValue);
        btnUpby1 = (Button) findViewById(R.id.btnUpby1);
        btnUpby10 = (Button) findViewById(R.id.btnUpby10);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnBind.setOnClickListener(this);
        btnUnbind.setOnClickListener(this);
        btnUpby1.setOnClickListener(this);
        btnUpby10.setOnClickListener(this);

        automaticBind();

    }

    private void automaticBind() {
        if (PollService.isRunning()) {
            doBindService();
        }

    }

    private void doBindService() {
        bindService(new Intent(this, PollService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textStatus.setText("Binding.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        mServiceMessenger = null;
        textStatus.setText("Disconnected.");
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceMessenger = new Messenger(service);
        textStatus.setText("Attached.");
        try {
            Message msg = Message.obtain(null, PollService.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            // In this case the service has crashed before we could even do anything with it
        }
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

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // Log.d(LOGTAG,"IncomingHandler:handleMessage");
            switch (msg.what) {
                case PollService.MSG_SET_TIME_INTERVAL:
                    textIntValue.setText("Int Message: " + msg.arg1);
                    break;
                case PollService.MSG_PERSISTENT_LIST:
                    LinkedHashMap<String, Grade> gradesmap = (LinkedHashMap<String, Grade>) msg.getData().getSerializable("gradesmap");
                    StringBuilder builder = new StringBuilder();
                    for (Map.Entry<String, Grade> entry : gradesmap.entrySet()) {
                        builder.append(entry.getKey()).append(": ").append(entry.getValue().grade()).append(System.lineSeparator());
                    }
                    textStrValue.setText(builder);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Un-bind this Activity to TimerService
     */
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
            textStatus.setText("Unbinding.");
        }
    }


    /**
     * Handle button clicks
     */
    @Override
    public void onClick(View v) {
        if (v.equals(btnStart)) {
            startService(new Intent(MyActivity.this, PollService.class));
        } else if (v.equals(btnStop)) {
            doUnbindService();
            stopService(new Intent(MyActivity.this, PollService.class));
        } else if (v.equals(btnBind)) {
            doBindService();
        } else if (v.equals(btnUnbind)) {
            doUnbindService();
        } else if (v.equals(btnUpby1)) {
            sendMessageToService(1);
        } else if (v.equals(btnUpby10)) {
            sendMessageToService(10);
        }
    }

    /**
     * Send data to the service
     *
     * @param intvaluetosend The data to send
     */
    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, PollService.MSG_SET_TIME_INTERVAL, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("textStatus", textStatus.getText().toString());
        outState.putString("textIntValue", textIntValue.getText().toString());
        outState.putString("textStrValue", textStrValue.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            textStatus.setText(savedInstanceState.getString("textStatus"));
            textIntValue.setText(savedInstanceState.getString("textIntValue"));
            textStrValue.setText(savedInstanceState.getString("textStrValue"));
        }
        super.onRestoreInstanceState(savedInstanceState);
    }
}
