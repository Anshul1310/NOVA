package com.anshul.a240dc;

public class VideoItem {
    private String name;
    private String duration;
    private int fps;
    private int iso;
    private String shutterSpeed;
    private String path; // NEW: The absolute path to the .mp4 file

    public VideoItem(String name, String duration, int fps, int iso, String shutterSpeed, String path) {
        this.name = name;
        this.duration = duration;
        this.fps = fps;
        this.iso = iso;
        this.shutterSpeed = shutterSpeed;
        this.path = path;
    }

    public String getName() { return name; }
    public String getDuration() { return duration; }
    public int getFps() { return fps; }
    public int getIso() { return iso; }
    public String getShutterSpeed() { return shutterSpeed; }
    public String getPath() { return path; } // NEW
}