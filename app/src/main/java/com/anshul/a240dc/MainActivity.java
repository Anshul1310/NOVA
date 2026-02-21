package com.anshul.a240dc;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Range;
import android.util.Size; // NEW IMPORT FOR SIZE FIX
import android.view.HapticFeedbackConstants;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String selectedFps = "120";
    private String selectedIso = "400";
    private String selectedShutter = "1/240";

    // UI Elements
    private Group groupSettings, groupRecording;
    private TextView tvTimer;
    private ImageView btnPauseResume, btnStop, btnRecord;
    private ObjectAnimator pulseAnimator;

    // Recording Info Overlays
    private TextView tvRecFps, tvRecIso, tvRecShutter;

    // Recording Variables
    private boolean isRecording = false;
    private boolean isPaused = false;
    private int secondsRecorded = 0;
    private Handler timerHandler = new Handler();

    // Camera & Media Variables
    private static final int REQUEST_PERMISSIONS = 200;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private String cameraId;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (!supportsHighSpeedVideo(120)) {
            startActivity(new Intent(this, UnsupportedDeviceActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        groupSettings = findViewById(R.id.group_settings);
        groupRecording = findViewById(R.id.group_recording);
        tvTimer = findViewById(R.id.tv_timer);
        btnRecord = findViewById(R.id.btn_record);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnStop = findViewById(R.id.btn_stop);

        tvRecFps = findViewById(R.id.tv_rec_fps);
        tvRecIso = findViewById(R.id.tv_rec_iso);
        tvRecShutter = findViewById(R.id.tv_rec_shutter);

        setupFpsToggle();
        setupIsoDial();
        setupShutterDial();

        btnRecord.setOnClickListener(v -> startRecording());
        btnPauseResume.setOnClickListener(v -> togglePauseResume());
        btnStop.setOnClickListener(v -> stopRecording());

        View btnVideoList = findViewById(R.id.btn_video_list);
        if (btnVideoList != null) {
            btnVideoList.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VideoList.class)));
        }

        // Setup Pulsing Animation for Recording Dot
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                findViewById(R.id.ic_recording_dot),
                PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.2f),
                PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.2f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0.4f)
        );
        pulseAnimator.setDuration(600);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
    }

    private void animateChipSelection(ChipGroup group, Chip selectedChip) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip c = (Chip) group.getChildAt(i);
            if (c == selectedChip) {
                c.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start();
            } else {
                c.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        openCamera();
    }

    @Override
    protected void onPause() {
        if (isRecording) stopRecording();
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
                return;
            }
            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { cameraDevice = camera; }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { cameraDevice.close(); }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void closeCamera() {
        if (captureSession != null) { captureSession.close(); captureSession = null; }
        if (cameraDevice != null) { cameraDevice.close(); cameraDevice = null; }
        if (mediaRecorder != null) { mediaRecorder.release(); mediaRecorder = null; }
    }

    private void startRecording() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Camera is still initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            setupMediaRecorder();

            Surface recorderSurface = mediaRecorder.getSurface();
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureBuilder.addTarget(recorderSurface);

            applyManualSettingsToCamera(captureBuilder);

            cameraDevice.createConstrainedHighSpeedCaptureSession(
                    Collections.singletonList(recorderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                CameraConstrainedHighSpeedCaptureSession highSpeedSession = (CameraConstrainedHighSpeedCaptureSession) session;
                                List<CaptureRequest> highSpeedRequests = highSpeedSession.createHighSpeedRequestList(captureBuilder.build());
                                session.setRepeatingBurst(highSpeedRequests, null, backgroundHandler);

                                runOnUiThread(() -> {
                                    try {
                                        mediaRecorder.start();
                                        isRecording = true;
                                        isPaused = false;
                                        secondsRecorded = 0;
                                        updateTimerText();

                                        tvRecFps.setText(selectedFps + " FPS");
                                        tvRecIso.setText("ISO " + selectedIso);
                                        tvRecShutter.setText(selectedShutter + "s");

                                        // Smooth Transition
                                        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new AutoTransition());
                                        groupSettings.setVisibility(View.GONE);
                                        groupRecording.setVisibility(View.VISIBLE);

                                        pulseAnimator.start();

                                        timerHandler.postDelayed(timerRunnable, 1000);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        isRecording = false;
                                        Toast.makeText(MainActivity.this, "Failed to start recorder.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (CameraAccessException e) { e.printStackTrace(); }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to configure high-speed camera. Unsupported resolution.", Toast.LENGTH_SHORT).show());
                        }
                    }, backgroundHandler);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupMediaRecorder() throws IOException {
        int fps = Integer.parseInt(selectedFps);
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File videoFolder = new File(dcimFolder, "ProCamera");
        if (!videoFolder.exists()) videoFolder.mkdirs();

        videoFile = new File(videoFolder, "VID_" + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());

        // ENCODERS
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        // --- FIX: DYNAMICALLY CHECK RESOLUTION ---
        Size bestSize = getBestHighSpeedSize(fps);
        mediaRecorder.setVideoSize(bestSize.getWidth(), bestSize.getHeight());

        // Dynamically adjust bitrate based on resolution (720p needs less data)
        if (bestSize.getWidth() == 1920) {
            mediaRecorder.setVideoEncodingBitRate(100_000_000); // 100 Mbps for 1080p
        } else {
            mediaRecorder.setVideoEncodingBitRate(50_000_000);  // 50 Mbps for 720p
        }

        mediaRecorder.setVideoFrameRate(fps);
        mediaRecorder.prepare();
    }

    private void togglePauseResume() {
        if (!isRecording) return;
        if (isPaused) {
            isPaused = false;
            btnPauseResume.setImageResource(android.R.drawable.ic_media_pause);
            pulseAnimator.resume();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) mediaRecorder.resume();
        } else {
            isPaused = true;
            btnPauseResume.setImageResource(android.R.drawable.ic_media_play);
            pulseAnimator.pause();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mediaRecorder != null) mediaRecorder.pause();
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        pulseAnimator.cancel();

        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.abortCaptures();
            }
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (videoFile != null) {
            String durationStr = String.format("%02d:%02d", secondsRecorded / 60, secondsRecorded % 60);
            String metadata = durationStr + "," + selectedFps + "," + selectedIso + "," + selectedShutter;

            // Saves metadata strictly tied to the filename
            getSharedPreferences("VideoMetadata", MODE_PRIVATE).edit().putString(videoFile.getName(), metadata).apply();
            Toast.makeText(this, "Saved to DCIM/ProCamera!\n" + videoFile.getName(), Toast.LENGTH_LONG).show();
        }

        // Smooth Transition
        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), new AutoTransition());
        groupSettings.setVisibility(View.VISIBLE);
        groupRecording.setVisibility(View.GONE);
        btnPauseResume.setImageResource(android.R.drawable.ic_media_pause);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPaused && isRecording) {
                secondsRecorded++;
                updateTimerText();
            }
            if (isRecording) timerHandler.postDelayed(this, 1000);
        }
    };

    private void updateTimerText() {
        tvTimer.setText(String.format("%02d:%02d", secondsRecorded / 60, secondsRecorded % 60));
    }

    public void applyManualSettingsToCamera(CaptureRequest.Builder builder) {
        int fps = Integer.parseInt(selectedFps);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.parseInt(selectedIso));
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, calculateShutterSpeedNanos(selectedShutter));
        long frameDurationNanos = 1_000_000_000L / fps;
        builder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDurationNanos);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(fps, fps));
    }

    private long calculateShutterSpeedNanos(String shutterString) {
        String[] parts = shutterString.split("/");
        if (parts.length == 2) return 1_000_000_000L / Integer.parseInt(parts[1]);
        return 1_000_000_000L / 120;
    }

    // --- NEW: Gets the best supported resolution to prevent Surface Size Crashes ---
    private Size getBestHighSpeedSize(int targetFps) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Size fallbackSize = new Size(1280, 720); // Default to 720p fallback

        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) continue;

                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                if (capabilities != null) {
                    for (int cap : capabilities) {
                        if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                            if (map != null) {
                                Size size1080p = new Size(1920, 1080);
                                Size[] sizes = map.getHighSpeedVideoSizes();

                                // 1. Check if 1080p is supported at the target FPS
                                for (Size s : sizes) {
                                    if (s.getWidth() == 1920 && s.getHeight() == 1080) {
                                        try {
                                            for (Range<Integer> range : map.getHighSpeedVideoFpsRangesFor(size1080p)) {
                                                if (range.getUpper() >= targetFps) return size1080p;
                                            }
                                        } catch (IllegalArgumentException e) { /* Ignored */ }
                                    }
                                }

                                // 2. If 1080p isn't supported, check if 720p is supported
                                Size size720p = new Size(1280, 720);
                                for (Size s : sizes) {
                                    if (s.getWidth() == 1280 && s.getHeight() == 720) {
                                        try {
                                            for (Range<Integer> range : map.getHighSpeedVideoFpsRangesFor(size720p)) {
                                                if (range.getUpper() >= targetFps) return size720p;
                                            }
                                        } catch (IllegalArgumentException e) { /* Ignored */ }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (CameraAccessException e) { e.printStackTrace(); }
        return fallbackSize;
    }

    // --- NEW: Strictly check 1080p at target FPS ---
    private boolean supportsHighSpeedAt1080p(int targetFps) {
        Size size = getBestHighSpeedSize(targetFps);
        return size.getWidth() == 1920 && size.getHeight() == 1080;
    }

    // Legacy general check just to boot the app
    private boolean supportsHighSpeedVideo(int targetFps) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) continue;

                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                if (capabilities != null) {
                    for (int cap : capabilities) {
                        if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                            return true; // As long as it supports SOME high speed video, let them in
                        }
                    }
                }
            }
        } catch (CameraAccessException e) { e.printStackTrace(); }
        return false;
    }

    private void setupFpsToggle() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggle_fps);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                group.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                if (checkedId == R.id.btn_120fps) {
                    selectedFps = "120";
                } else if (checkedId == R.id.btn_240fps) {
                    // --- ENFORCE STRICT 240 FPS @ 1080p RULE ---
                    if (!supportsHighSpeedAt1080p(240)) {
                        Toast.makeText(MainActivity.this, "Your device does not support 240 FPS at 1080p", Toast.LENGTH_LONG).show();
                        group.check(R.id.btn_120fps); // Snap back to 120
                        selectedFps = "120";
                    } else {
                        selectedFps = "240";
                    }
                }
            }
        });
    }

    private void setupIsoDial() {
        ChipGroup chipGroupIso = findViewById(R.id.chipGroup_iso);
        String[] isoValues = {"100", "200", "400", "800", "1600", "3200", "6400"};
        for (String iso : isoValues) {
            Chip chip = createProChip(iso);
            chipGroupIso.addView(chip);
            if (iso.equals(selectedIso)) {
                chip.setChecked(true);
                chip.setScaleX(1.15f);
                chip.setScaleY(1.15f);
            }
        }
        chipGroupIso.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedIso = chip.getText().toString();
                    chip.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    animateChipSelection(group, chip);
                }
            }
        });
    }

    private void setupShutterDial() {
        ChipGroup chipGroupShutter = findViewById(R.id.chipGroup_shutter);
        String[] shutterValues = {"1/60", "1/120", "1/240", "1/480", "1/1000", "1/2000", "1/4000"};
        for (String speed : shutterValues) {
            Chip chip = createProChip(speed);
            chipGroupShutter.addView(chip);
            if (speed.equals(selectedShutter)) {
                chip.setChecked(true);
                chip.setScaleX(1.15f);
                chip.setScaleY(1.15f);
            }
        }
        chipGroupShutter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    selectedShutter = chip.getText().toString();
                    chip.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    animateChipSelection(group, chip);
                }
            }
        });
    }

    private Chip createProChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColorResource(android.R.color.transparent);

        int colorUnselected = ContextCompat.getColor(this, R.color.white);
        int colorSelected = ContextCompat.getColor(this, R.color.pro_accent_yellow);
        int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, new int[] { -android.R.attr.state_checked } };
        int[] colors = new int[] { colorSelected, colorUnselected };

        ColorStateList colorStateList = new ColorStateList(states, colors);
        chip.setTextColor(colorStateList);
        chip.setChipStrokeColor(colorStateList);
        chip.setChipStrokeWidth(2f);
        return chip;
    }
}