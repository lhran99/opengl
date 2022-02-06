package com.example.camera2;

import static android.hardware.camera2.CameraDevice.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int CAMERA_REQUEST_CODE = 1;
    private Button mStartBtn;
    private Button mStopBtn;
    private String mCameraId;
    private CameraDevice mCamera;

    private HandlerThread handlerThread = null;
    private Handler handler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private SurfaceView mSurfaceView;
    private CameraCaptureSession mCaptureSession;
    private  CaptureRequest.Builder mCaptureRequestBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = findViewById(R.id.start_capture);
        mStopBtn = findViewById(R.id.stop_capture);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        handlerThread = new HandlerThread("camera_capture");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        mSurfaceView = findViewById(R.id.surface_view);
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
        getCamera();
        openCamera();
    }

    private void getCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] ids = manager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                if (characteristics == null) {
                    continue;
                }
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraId = id;
                    break;
                }
            }

            if (mCameraId == null) {
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        if (mCameraId == null) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                return;
            }
            manager.openCamera(mCameraId, new StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    mCamera = camera;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    mCameraOpenCloseLock.release();
                    camera.close();
                    mCamera = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    mCameraOpenCloseLock.release();
                    camera.close();
                    mCamera = null;
                }}, handler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            mCaptureRequestBuilder = mCamera.createCaptureRequest(TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurfaceView.getHolder().getSurface());

            mCamera.createCaptureSession(Arrays.asList(mSurfaceView.getHolder().getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            mCaptureSession = cameraCaptureSession;
                            if (mCaptureSession != null) {
                                try {
                                    mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopCapture() {
        try {
            mCameraOpenCloseLock.acquire();
            mCaptureSession.stopRepeating();
            mCamera.close();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
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