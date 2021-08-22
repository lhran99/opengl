package com.example.audioexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "AUDIO_DEMO";
    private Button record;
    private Button play;
    private Button stop;
    private AudioRecord recorder;
    private AudioTrack tracker;
    private HandlerThread handlerThread;
    private Handler handler;
    private volatile int currentState;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final String PCM_FILE = "/sdcard/pcm_file.pcm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        record = findViewById(R.id.record_btn);
        record.setOnClickListener(this);
        play = findViewById(R.id.track_btn);
        play.setOnClickListener(this);
        stop = findViewById(R.id.stop_btn);
        stop.setOnClickListener(this);
        handlerThread = new HandlerThread("audio_thread");
        handlerThread.start();
        handler = new MyHandler(handlerThread.getLooper());
        askForPermission();
    }

    private void askForPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record_btn:{
                startRecordMessage();
                break;
            }
            case R.id.track_btn: {
                startPlayMessage();
                break;
            }
            case R.id.stop_btn: {
                stopAudioMessage();
                break;
            }
            default:
                break;
        }
    }

    class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0: {
                    startRecord();
                    break;
                }
                case 1: {
                    stopAudio();
                    break;
                }
                case 2: {
                    startPlay();
                    break;
                }
            }
        }
    }

    private void stopAudio() {
        currentState = 1;
    }

    private void startRecord() {
        int sampleRate = 44100;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT, bufferSize);
        recorder.startRecording();
        currentState = 0;
        Log.d(TAG, "start record");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                byte byteBuffer [] = new byte[1024];
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(PCM_FILE);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                while (currentState != 1) {
                    int read = recorder.read(byteBuffer, 0, byteBuffer.length);
                    Log.d(TAG, "size is " + read);
                    try {
                        outputStream.write(byteBuffer, 0, read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recorder.stop();
            }
        });
    }



    private void startPlay() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build();
        AudioFormat audioFormat = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_8BIT).setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT);
        tracker = new AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        tracker.play();
        currentState = 2;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                byte[] data = new byte[1024]; // small buffer size to not overflow AudioTrack's internal buffer
                FileInputStream fileInputStream = null;
                try {
                    fileInputStream = new FileInputStream(new File(PCM_FILE));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                int readSize = 0;
                while (readSize != -1) {
                    try {
                        readSize = fileInputStream.read(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    Log.d(TAG, "start play read size is " + readSize);
                    if (readSize > 0) {
                        tracker.write(data, 0, readSize);
                    }
                }
                try {
                    fileInputStream.close();
                }
                catch (IOException e) {
                    // handle exception
                }

                tracker.stop();
                tracker.release();
            }
        });
    }

    enum AudioState {
        AUDIO_RECORD(0),
        AUDIO_STOP(1),
        AUDIO_PLAY(2);

        AudioState(int value) {
            this.value =value;
        }
        int value;
    }

    private void startRecordMessage() {
        handler.sendMessage(handler.obtainMessage(AudioState.AUDIO_RECORD.value));
    }

    private void startPlayMessage() {
        handler.sendMessage(handler.obtainMessage(AudioState.AUDIO_PLAY.value));
    }

    private void stopAudioMessage() {
        handler.sendMessage(handler.obtainMessage(AudioState.AUDIO_STOP.value));
    }
}
