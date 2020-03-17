package com.apyeng.airconhomev2;

import android.graphics.Bitmap;

public class MemberItem {

    private int userId;
    private String username;
    private String imageName;
    private String activeTime;
    private Bitmap imageDownloaded;

    public MemberItem(int userId, String username, String imageName, String activeTime){
        this.userId = userId;
        this.username = username;
        this.imageName = imageName;
        this.activeTime = activeTime;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setActiveTime(String activeTime) {
        this.activeTime = activeTime;
    }

    public void setImageDownloaded(Bitmap imageDownloaded) {
        this.imageDownloaded = imageDownloaded;
    }

    public Bitmap getImageDownloaded() {
        return imageDownloaded;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getImageName() {
        return imageName;
    }

    public String getActiveTime() {
        return activeTime;
    }

}
