package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mStartBtn;
    private Button mStopBtn;
    private PreviewView mPreview;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private final int CAMERA_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = findViewById(R.id.start_capture);
        mStopBtn = findViewById(R.id.stop_capture);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mPreview = findViewById(R.id.preview);
        requestPermission();
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                break;
            }
            default:
                break;
        }
        Toast.makeText(this, "permission denied ", Toast.LENGTH_LONG).show();
    }

    private void startCapture() {
        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);
        mCameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();

                    Preview preview = new Preview.Builder().build();
                    ImageAnalysis imageAnalysis =
                            new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720)).build();
                    CameraSelector cameraSelector = CameraSelector.Builder.fromSelector(CameraSelector.DEFAULT_FRONT_CAMERA).build();

                    cameraProvider.unbind();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis, preview);
                    preview.setSurfaceProvider(mPreview.getSurfaceProvider());

                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("RestrictedApi")
    private void stopCapture() {
        try {
            mCameraProviderFuture.get().shutdown();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.start_capture:
                startCapture();
                break;
            case R.id.stop_capture:
                stopCapture();
                break;
            default:
                break;
        }
    }
}