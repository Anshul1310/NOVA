package com.anshul.a240dc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private Context context;
    private List<VideoItem> videoList;

    // Background thread executor so thumbnail loading doesn't freeze the scroll
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    public VideoAdapter(Context context, List<VideoItem> videoList) {
        this.context = context;
        this.videoList = videoList;
        this.executorService = Executors.newFixedThreadPool(4); // 4 background threads
        this.mainThreadHandler = new Handler(Looper.getMainLooper()); // Posts back to UI
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem video = videoList.get(position);

        holder.tvName.setText(video.getName());
        holder.tvDuration.setText("⏱ " + video.getDuration());

        // Format the technical specs cleanly
        String specs = String.format("%d FPS  •  ISO %d  •  %s",
                video.getFps(), video.getIso(), video.getShutterSpeed());
        holder.tvSpecs.setText(specs);

        // 1. Set a default placeholder while loading
        holder.imgThumbnail.setImageResource(android.R.drawable.ic_media_play);

        // 2. Tag the ImageView with the path. (Crucial for RecyclerViews so images
        // don't get mixed up when scrolling fast)
        holder.imgThumbnail.setTag(video.getPath());

        // 3. Load the video frame asynchronously
        executorService.execute(() -> {
            Bitmap bitmap = null;
            try {
                // Android's native ThumbnailUtils grabs a representative frame of the video
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = ThumbnailUtils.createVideoThumbnail(new File(video.getPath()), new Size(512, 512), null);
                } else {
                    bitmap = ThumbnailUtils.createVideoThumbnail(video.getPath(), MediaStore.Video.Thumbnails.MINI_KIND);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (bitmap != null) {
                final Bitmap finalBitmap = bitmap;
                // Post the generated frame back to the main UI thread
                mainThreadHandler.post(() -> {
                    // Check if the view is still showing the same video (wasn't recycled)
                    if (video.getPath().equals(holder.imgThumbnail.getTag())) {
                        holder.imgThumbnail.setImageBitmap(finalBitmap);
                        holder.imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });
            }
        });

        // --- Launch video player on click ---
        holder.itemView.setOnClickListener(v -> {
            File videoFile = new File(video.getPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(videoFile);

            // Set data type to video so Android knows to open media players
            intent.setDataAndType(uri, "video/mp4");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView tvName, tvDuration, tvSpecs;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvName = itemView.findViewById(R.id.tv_video_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSpecs = itemView.findViewById(R.id.tv_specs);
        }
    }
}