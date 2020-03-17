package com.apyeng.airconhomev2;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class UserItem implements Parcelable{

    private int userId;
    private String username;
    private String email;
    private String profileImg;
    private String registeredTime;
    private Bitmap imageDownloaded;


    public UserItem(int userId, String username,
                    String email, String profileImg, String registeredTime){
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImg = profileImg;
        this.registeredTime = registeredTime;
    }

    protected UserItem(Parcel in) {
        userId = in.readInt();
        username = in.readString();
        email = in.readString();
        profileImg = in.readString();
        registeredTime = in.readString();
      //  imageDownloaded = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<UserItem> CREATOR = new Creator<UserItem>() {
        @Override
        public UserItem createFromParcel(Parcel in) {
            return new UserItem(in);
        }

        @Override
        public UserItem[] newArray(int size) {
            return new UserItem[size];
        }
    };

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRegisteredTime(String registeredTime) {
        this.registeredTime = registeredTime;
    }

    public void setImageDownloaded(Bitmap imageDownloaded) {
        this.imageDownloaded = imageDownloaded;
    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }

    public String getProfileImg() {
        return profileImg;
    }

    public String getEmail() {
        return email;
    }

    public Bitmap getImageDownloaded() {
        return imageDownloaded;
    }

    public String getRegisteredTime() {
        return registeredTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(userId);
        parcel.writeString(username);
        parcel.writeString(email);
        parcel.writeString(profileImg);
        parcel.writeString(registeredTime);
      //  parcel.writeParcelable(imageDownloaded, i);
        //Can't pass Bitmap
    }
}
