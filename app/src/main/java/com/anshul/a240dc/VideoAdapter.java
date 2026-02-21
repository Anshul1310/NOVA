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
import android.widget.Toast;

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
        holder.tvDuration.setText("⏱ " + video.getDuration()); // Keep as per your layout overlay

        // Format the technical specs cleanly
        String specs = String.format("%d FPS  •  ISO %d  •  %s",
                video.getFps(), video.getIso(), video.getShutterSpeed());
        holder.tvSpecs.setText(specs);

        // 1. Set a default placeholder while loading
        holder.imgThumbnail.setImageResource(android.R.drawable.ic_media_play);

        // 2. Tag the ImageView with the path to avoid mixing images when scrolling
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

        // --- 4. Launch video player on card click ---
        holder.itemView.setOnClickListener(v -> {
            File videoFile = new File(video.getPath());
            if (!videoFile.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(videoFile);
            intent.setDataAndType(uri, "video/mp4");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        });

        // --- 5. Share Video + Metadata Post Button ---
        holder.btnShare.setOnClickListener(v -> {
            File videoFile = new File(video.getPath());
            if (!videoFile.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = Uri.fromFile(videoFile);

            // Build the caption/post text using the video metadata
            String postCaption = "🎥\n\n" +
                    "⚙️ Settings: " + video.getFps() + " FPS | ISO " + video.getIso() + " | " + video.getShutterSpeed() + "\n" +
                    "⏱ Duration: " + video.getDuration() + "\n\n";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/mp4");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);        // Attaches the actual video file
            shareIntent.putExtra(Intent.EXTRA_TEXT, postCaption);  // Attaches the text as a caption/post body
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Share Video & Details via"));
        });

        // --- 6. Delete Video Button ---
        holder.btnDelete.setOnClickListener(v -> {
            File videoFile = new File(video.getPath());

            // Delete the actual physical file from the device
            if (videoFile.exists() && videoFile.delete()) {

                // Remove the metadata from SharedPreferences
                context.getSharedPreferences("VideoMetadata", Context.MODE_PRIVATE)
                        .edit()
                        .remove(video.getName())
                        .apply();

                // Remove from the RecyclerView list and update the UI
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    videoList.remove(currentPosition);
                    notifyItemRemoved(currentPosition);
                    notifyItemRangeChanged(currentPosition, videoList.size());
                    Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to delete video", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail, btnShare, btnDelete;
        TextView tvName, tvDuration, tvSpecs;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvName = itemView.findViewById(R.id.tv_video_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvSpecs = itemView.findViewById(R.id.tv_specs);

            // Bind the Share and Delete buttons
            btnShare = itemView.findViewById(R.id.btn_share);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}