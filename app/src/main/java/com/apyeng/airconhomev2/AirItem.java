package com.apyeng.airconhomev2;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;

public class AirItem implements Parcelable{

    private int deviceId;
    private String actualName;
    private String nickname;
    private boolean online;
    private Indoor indoor;
    private String error;

    public AirItem(){

    }

    public AirItem(int deviceId){
        this.deviceId = deviceId;
    }

    public AirItem(int deviceId, String actualName, String nickname, Indoor indoor){
        this.deviceId = deviceId;
        this.actualName = actualName;
        this.nickname = nickname;
        this.indoor = indoor;
    }

    protected AirItem(Parcel in) {
        deviceId = in.readInt();
        actualName = in.readString();
        nickname = in.readString();
        online = in.readByte() != 0;
        error = in.readString();
    }

    public static final Creator<AirItem> CREATOR = new Creator<AirItem>() {
        @Override
        public AirItem createFromParcel(Parcel in) {
            return new AirItem(in);
        }

        @Override
        public AirItem[] newArray(int size) {
            return new AirItem[size];
        }
    };

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public void setActualName(String actualName) {
        this.actualName = actualName;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setIndoor(Indoor indoor) {
        this.indoor = indoor;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public String getActualName() {
        return actualName;
    }

    public String getNickname() {
        return nickname;
    }

    public Indoor getIndoor() {
        return indoor;
    }

    public String getError() {
        return error;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(deviceId);
        parcel.writeString(actualName);
        parcel.writeString(nickname);
        parcel.writeByte((byte) (online ? 1 : 0));
        parcel.writeString(error);
    }
}
