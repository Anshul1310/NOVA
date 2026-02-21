package com.anshul.a240dc;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        changeStatusBarColor("#ffffff");
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Generate dummy data
        videoList = new ArrayList<>();

        // Note: Replace android.R.drawable.ic_media_play with your own drawable like R.drawable.my_thumbnail
        videoList.add(new VideoItem("Cinematic B-Roll", "02:45", 60, 400, "1/120s", android.R.drawable.ic_media_play));
        videoList.add(new VideoItem("Night Sky Timelapse", "01:12", 24, 3200, "10s", android.R.drawable.ic_media_play));
        videoList.add(new VideoItem("Interview Footage", "15:30", 24, 100, "1/50s", android.R.drawable.ic_media_play));
        videoList.add(new VideoItem("Sports Slow Motion", "00:58", 120, 800, "1/240s", android.R.drawable.ic_media_play));

        adapter = new VideoAdapter(videoList);
        recyclerView.setAdapter(adapter);
    }
}