package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private final int CAMERA_REQUEST_CODE = 1;
    private Camera mCamera = null;
    private SurfaceView mSurfaceView = null;
    private Button mStartBtn;
    private Button mStopBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = findViewById(R.id.start_capture);
        mStopBtn = findViewById(R.id.stop_capture);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        requestPermission();
    }

    private void setSurfaceView() {
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
        mSurfaceView.setVisibility(View.VISIBLE);
        try {
            mCamera.setPreviewDisplay(mSurfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    private void startCapture() {
        openCamera();
        setSurfaceView();
        mCamera.startPreview();
    }

    private void openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i =0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        int degree = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            degree = info.orientation % 360 - 180;
        } else {
            degree = (info.orientation + 360) % 360;
        }
        mCamera.setDisplayOrientation(degree);
        setPreviewFormat(ImageFormat.NV16);
        setWidthHeight(2560, 1280);
        setPreviewFps(15000,30000);
    }


    private void setPreviewFormat(int format) {
        Camera.Parameters params = mCamera.getParameters();
        List<Integer> supportedFormat = params.getSupportedPreviewFormats();
        if (supportedFormat.contains(format)) {
            params.setPreviewFormat(format);
        }
        mCamera.setParameters(params);
    }

    private void setWidthHeight(int width, int height) {
        int width_param = 0;
        int height_param = 0;
        double last_weight = Double.MAX_VALUE;
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            if (size.height == height && size.width == width) {
                width_param = size.width;
                height_param = size.height;
                break;
            }

            double weight = Math.pow(Math.abs(size.height - height),2) + Math.pow(Math.abs(size.width - width),2);
            if (weight < last_weight) {
                width_param = size.width;
                height_param = size.height;
            }
        }
        params.setPreviewSize(width_param, height_param);
        mCamera.setParameters(params);
    }

    private void setPreviewFps(int minFps, int maxFps) {
        int minFps_param = 0;
        int maxFps_param = 0;
        double last_weight = Double.MAX_VALUE;
        Camera.Parameters params = mCamera.getParameters();
        List<int[]> ranges = params.getSupportedPreviewFpsRange();
        for (int[] range : ranges) {
            if (range[0] <= minFps && range[1] >= maxFps) {
                minFps_param = range[0];
                maxFps_param = range[1];
                break;
            }

            double weight = Math.pow(Math.abs(range[0] - minFps),2) + Math.pow(Math.abs(range[1] - maxFps),2);
            if (weight < last_weight) {
                minFps_param = range[0];
                maxFps_param = range[1];
            }
        }
        params.setPreviewFpsRange(minFps_param, maxFps_param);
        mCamera.setParameters(params);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
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

    private void stopCapture() {
        mCamera.stopPreview();
        try {
            mCamera.setPreviewDisplay(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.release();
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