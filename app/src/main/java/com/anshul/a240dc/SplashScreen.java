package com.anshul.a240dc;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.util.Range;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashScreen extends AppCompatActivity {

    private void changeStatusBarColor(String colorHex) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(colorHex));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        changeStatusBarColor("#241515");

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottie_splash);

        // Listen for when the animation finishes
        lottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Check hardware support when the animation finishes
                if (supportsHighSpeedVideo(120)) {
                    // Device is capable, proceed to the main camera
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
                } else {
                    // Device is not capable, show the unsupported screen
                    startActivity(new Intent(SplashScreen.this, UnsupportedDeviceActivity.class));
                }
                finish(); // Close SplashActivity so user can't go back to it
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    /**
     * Checks if the device's main back camera supports constrained high-speed video
     * at the required frame rate without needing camera permissions yet.
     */
    private boolean supportsHighSpeedVideo(int targetFps) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);

                // Skip front cameras, usually only the back sensor supports high speed
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) continue;

                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                if (capabilities != null) {
                    for (int cap : capabilities) {
                        if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO) {
                            StreamConfigurationMap map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                            if (map != null) {
                                // Check if the required FPS is supported in the high-speed ranges
                                for (Range<Integer> range : map.getHighSpeedVideoFpsRanges()) {
                                    if (range.getUpper() >= targetFps) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}