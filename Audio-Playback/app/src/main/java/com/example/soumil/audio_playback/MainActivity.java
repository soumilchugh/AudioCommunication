package com.example.soumil.audio_playback;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AudioService.AudioServiceBinder mAudioServiceBinder;
    private ServiceStateReceiver mServiceStateReceiver;
    public final Messenger mMessenger = new Messenger(new IncomingHandler());
    public static final String TAG = MainActivity.class.getName();
    public boolean mServiceBounded;
    Button button1;
    Button button2;
    Button button3;
    public boolean mPermissionGranted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button1 =  findViewById(R.id.button1);
        button1.setOnClickListener(this);
        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(this);
        button3 = findViewById(R.id.button3);
        button3.setOnClickListener(this);
        if (savedInstanceState != null)
        {
            mPermissionGranted = savedInstanceState.getBoolean("Permission");
            mServiceBounded = savedInstanceState.getBoolean("Service Bounded");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.action.service");
        mServiceStateReceiver = new ServiceStateReceiver();
        registerReceiver(mServiceStateReceiver,filter);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
       super.onSaveInstanceState(savedInstanceState);
       savedInstanceState.putBoolean("Permission",mPermissionGranted);
       savedInstanceState.putBoolean("Service Bounded",mServiceBounded);

    }

    public void startNewthread()
    {
        bindService(new Intent(getApplicationContext(),AudioService.class),mServiceConnection,Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        String permissions[] = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO};
        if (!mPermissionGranted)
        {
            ActivityCompat.requestPermissions(this, permissions,1);
        }
        else {
            startNewthread();
        }
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, String permissions[], int[] grantResults)
    {
       switch(resultCode)
       {
           case 1:
               if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
               {
                   mPermissionGranted = true;
                   startNewthread();
                  //bindService(new Intent(this,AudioService.class),mServiceConnection,Context.BIND_AUTO_CREATE);

               }
               else
               {
                   mPermissionGranted = false;
               }
               break;
       }
       return;

    }
    @Override
    public void onPause()
    {
        super.onPause();
        if (mServiceBounded)
            unbindService(mServiceConnection);
        mServiceBounded = false;

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mServiceStateReceiver != null)
            unregisterReceiver(mServiceStateReceiver);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.button1:
                mAudioServiceBinder.start_Recording();
                break;
            case R.id.button2:
                mAudioServiceBinder.stop_Recording();
                break;
            case R.id.button3:
                mAudioServiceBinder.start_playback();
                break;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mAudioServiceBinder = (AudioService.AudioServiceBinder) iBinder;
            mAudioServiceBinder.registerActivityMessenger(mMessenger);
            mServiceBounded = true;
            Toast.makeText(getApplicationContext(),"Service is binded",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBounded = false;

        }
    } ;

    public class IncomingHandler extends Handler
    {
        @Override
        public void handleMessage(Message message)
        {
            int responsecode = message.what;
            switch(responsecode)
            {
                default:
                    super.handleMessage(message);
            }
        }

    }
    public class ServiceStateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.action.service"))
            {
                mServiceBounded = false;
                Toast.makeText(getApplicationContext(), "Service is unbinded",Toast.LENGTH_SHORT).show();

            }

        }
    }

}
