package com.anshul.a240dc;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private List<VideoItem> videoList;

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
        setContentView(R.layout.activity_video_list);
        changeStatusBarColor("#000000"); // Changed to black for dark mode aesthetic

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        videoList = new ArrayList<>();
        loadVideosFromStorage();

        adapter = new VideoAdapter(this, videoList);
        recyclerView.setAdapter(adapter);

        // --- NEW: Staggered Slide-in Animation ---
        LayoutAnimationController controller = new LayoutAnimationController(
                AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        controller.setDelay(0.15f); // Delay between each item appearing
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        recyclerView.setLayoutAnimation(controller);
    }

    private void loadVideosFromStorage() {
        File dcimFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File videoFolder = new File(dcimFolder, "ProCamera");

        if (!videoFolder.exists() || !videoFolder.isDirectory()) return;

        File[] files = videoFolder.listFiles();
        if (files == null) return;

        SharedPreferences prefs = getSharedPreferences("VideoMetadata", MODE_PRIVATE);

        for (File file : files) {
            if (file.getName().endsWith(".mp4")) {
                String fileName = file.getName();
                String metadata = prefs.getString(fileName, null);

                if (metadata != null) {
                    String[] data = metadata.split(",");
                    if (data.length == 4) {
                        String duration = data[0];
                        int fps = Integer.parseInt(data[1]);
                        int iso = Integer.parseInt(data[2]);
                        String shutter = data[3];

                        videoList.add(new VideoItem(fileName, duration, fps, iso, shutter, file.getAbsolutePath()));
                    }
                }
            }
        }
    }
}