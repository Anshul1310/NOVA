package com.anshul.a240dc;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<VideoItem> videoList;

    public VideoAdapter(List<VideoItem> videoList) {
        this.videoList = videoList;
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

        // Set thumbnail (Use a placeholder image from your drawable folder)
        // In a real app: Glide.with(holder.itemView.getContext()).load(video.getUrl()).into(holder.imgThumbnail);
        if (video.getThumbnailResId() != 0) {
            holder.imgThumbnail.setImageResource(video.getThumbnailResId());
        }
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