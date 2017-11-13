package com.example.soumil.audio_playback;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.telephony.ServiceState;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by soumi on 10-11-2017.
 */

public class AudioService extends Service {

    public enum AudioState {IDLE,PLAYING,RECORDING};
    public AudioState mAudioState;
    private final IBinder mBinder = new AudioServiceBinder();
    private Messenger mMessenger;
    public static boolean isServiceBinded;
    private static final int RECORDING_RATE = 44100;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int PLAYBACK_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PLAYBACK_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 1024;
    private AudioManager mAudioManager;
    FileWriter writer;
    private AudioRecord mAudioRecord = null;
    private static final String mOutputFileName = "Recording";
    private boolean stopped = false;
    private static final String TAG = AudioService.class.getName();
    File root;
    File dir;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        isServiceBinded = true;
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        super.onUnbind(intent);
        isServiceBinded = false;
        //Toast.makeText(getApplicationContext(),"Service is unbinded",Toast.LENGTH_LONG).show();
        // Communicate with the activity that the service is unbinded.
        Intent intent1 = new Intent("android.action.service").putExtra("unBind","Service Unbinded");

        sendBroadcast(intent1);
        return true;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        mAudioState = AudioState.IDLE;

    }

    @Override
    public int onStartCommand(final Intent intent, int id, int flags )
    {
        super.onStartCommand(intent,id,flags);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

    }
    public static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }

    public void startrecording()
    {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
        BufferedOutputStream bufferedOutputStream = null;
        root = android.os.Environment.getExternalStorageDirectory();
        dir = new File(root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir,"Data.txt");
        //File file1 = new File(dir,"Data2.txt");

        try {
            OutputStream out = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(out);
          //  writer = new FileWriter(file1);

            byte[] buffer = new byte[BUFFER_SIZE];
            mAudioRecord.startRecording();
            mAudioState = AudioState.RECORDING;
            while (!stopped) {
                int read = mAudioRecord.read(buffer, 0, buffer.length);
             //   short[] pcmAsFloats = shortMe(buffer);
             //   for (int i = 0; i < pcmAsFloats.length;i++)
             //   {
              //      writer.write(String.valueOf(pcmAsFloats[i]));
             //       writer.write(", ");
             //       Log.d(TAG,String.valueOf(pcmAsFloats[i]));
             //   }
             //   writer.write("\n");
                bufferedOutputStream.write(buffer, 0, read);

            }
        } catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Failed to record data: " + e);
        } finally {
            if (bufferedOutputStream != null) {
                try {
                  //  writer.close();
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            mAudioRecord.release();
            mAudioRecord = null;
            mAudioState = AudioState.IDLE;
            stopped = false;
        }
    }


    public void stoprecording()
    {
        if (mAudioState == AudioState.RECORDING) {
            stopped = true;
            Log.d(TAG, "Stopping the recording ...");
            mAudioState = AudioState.IDLE;
        } else {
            Log.w(TAG, "Requesting to stop recording while state was not RECORDING");
        }

    }

    public void startplayback()
    {
        if (mAudioState != AudioState.IDLE)
        {
            Log.d(TAG,"Cannot play");
            return;
        }
        if (! new File(dir,"Data.txt").exists())
        {
            Log.d(TAG,"No file to play");
            return;
        }
        /*if (! new File(getApplicationContext().getFilesDir(), mOutputFileName).exists())
        {
            Log.d(TAG,"No file to play");
            return;
        }*/
        final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE,PLAYBACK_CHANNEL,PLAYBACK_AUDIO_ENCODING);
        AudioTrack mAudioTrack;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
        mAudioState = AudioState.PLAYING;
        try {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE, PLAYBACK_CHANNEL, PLAYBACK_AUDIO_ENCODING, intSize, AudioTrack.MODE_STREAM);
            byte[] buffer = new byte[intSize * 2];
            FileInputStream in = null;
            BufferedInputStream bufferedInputStream = null;
            mAudioTrack.setVolume(AudioTrack.getMaxVolume());
            mAudioTrack.play();
            try {
                String filename = "Data.txt";
                String path = dir.getPath() + "/" + filename;
                File file = new File(path);
                in = new FileInputStream(file);
               // in = getApplicationContext().openFileInput(mOutputFileName);
                bufferedInputStream = new BufferedInputStream(in);
                int read;
                while ((read = bufferedInputStream.read(buffer, 0, buffer.length)) > 0) {
                    mAudioTrack.write(buffer, 0, read);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read file");
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (bufferedInputStream != null) {

                    }
                } catch (IOException e) {

                }
                mAudioTrack.release();
                mAudioState = AudioState.IDLE;
            }
        }
        catch (IllegalStateException e) {
            Log.e(TAG, "Falied to playback");

        }

    }

    public class AudioServiceBinder extends Binder
    {
        public void registerActivityMessenger(Messenger messenger)
        {
            mMessenger = messenger;
        }

        public AudioState getState()
        {
            return mAudioState;
        }

        public void start_Recording()
        {
            Thread t = new Thread()
            {
                public void run() {
                    startrecording();
                }
            };
            t.start();

        }

        public void stop_Recording()
        {
            stoprecording();

        }
        public void start_playback()
        {
            startplayback();

        }


    }

}
