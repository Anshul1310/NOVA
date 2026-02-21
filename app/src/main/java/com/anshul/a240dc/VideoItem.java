package com.anshul.a240dc;

public class VideoItem {
    private String name;
    private String duration;
    private int fps;
    private int iso;
    private String shutterSpeed;
    private int thumbnailResId; // Using a drawable resource for this example

    public VideoItem(String name, String duration, int fps, int iso, String shutterSpeed, int thumbnailResId) {
        this.name = name;
        this.duration = duration;
        this.fps = fps;
        this.iso = iso;
        this.shutterSpeed = shutterSpeed;
        this.thumbnailResId = thumbnailResId;
    }

    public String getName() { return name; }
    public String getDuration() { return duration; }
    public int getFps() { return fps; }
    public int getIso() { return iso; }
    public String getShutterSpeed() { return shutterSpeed; }
    public int getThumbnailResId() { return thumbnailResId; }
}