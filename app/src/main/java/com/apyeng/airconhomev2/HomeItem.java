package com.apyeng.airconhomev2;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;

public class HomeItem implements Parcelable{

    private int groupId;
    private String name;
    private String profileImg;      //File name
    private String registeredTime;
    private Bitmap imageDownloaded;           //Image download finish
    private int deviceId[];
    //Power value base on MODBUS value
    private int consumption;
    private int saving;
    private SparseIntArray consumptionEachId;
    private SparseIntArray savingEachId;

    public HomeItem(){

    }

    public HomeItem(int groupId, String name, String profileImg, int deviceId[], String registeredTime){
        this.groupId = groupId;
        this.name = name;
        this.profileImg = profileImg;
        this.deviceId = deviceId;
        this.registeredTime = registeredTime;
        consumptionEachId = new SparseIntArray();
        savingEachId = new SparseIntArray();

    }

    public HomeItem(int groupId, @NonNull String name, String profileImg, int deviceId[], String registeredTime, int consumption, int saving){
        this.groupId = groupId;
        this.name = name;
        this.profileImg = profileImg;
        this.deviceId = deviceId;
        this.consumption = consumption;
        this.saving = saving;
        this.registeredTime = registeredTime;
        consumptionEachId = new SparseIntArray();
        savingEachId = new SparseIntArray();
    }

    protected HomeItem(Parcel in) {
        groupId = in.readInt();
        name = in.readString();
        profileImg = in.readString();
        registeredTime = in.readString();
       // imageDownloaded = in.readParcelable(Bitmap.class.getClassLoader());
        deviceId = in.createIntArray();
        consumption = in.readInt();
        saving = in.readInt();
    }


    public static final Creator<HomeItem> CREATOR = new Creator<HomeItem>() {
        @Override
        public HomeItem createFromParcel(Parcel in) {
            return new HomeItem(in);
        }

        @Override
        public HomeItem[] newArray(int size) {
            return new HomeItem[size];
        }
    };

    public void setSavingEachId(@NonNull SparseIntArray savingEachId) {
        this.savingEachId = savingEachId;
        //Update saving
        saving = getSumOfPower(savingEachId);
    }

    public void setConsumptionEachId(@NonNull SparseIntArray consumptionEachId) {
        this.consumptionEachId = consumptionEachId;
        //Update consumption
        consumption = getSumOfPower(consumptionEachId);
    }

    private int getSumOfPower(@NonNull SparseIntArray sparseIntArray){

        if (deviceId!=null){
            int sum = 0;
            for (int id : deviceId){
                sum += sparseIntArray.get(id, 0);
            }
            return sum;
        }
        return 0;

    }

    public void setRegisteredTime(String registeredTime) {
        this.registeredTime = registeredTime;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public void setImageDownloaded(Bitmap imageDownloaded) {
        this.imageDownloaded = imageDownloaded;
    }

    public void setDeviceId(int[] deviceId) {
        this.deviceId = deviceId;
    }

    public void setConsumption(int consumption) {
        this.consumption = consumption;
    }

    public void setSaving(int saving) {
        this.saving = saving;
    }

    public Bitmap getImageDownloaded() {
        return imageDownloaded;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getProfileImg() {
        return profileImg;
    }

    public int[] getDeviceId() {
        return deviceId;
    }

    public int getConsumption() {
        return consumption;
    }

    public int getSaving() {
        return saving;
    }

    public String getRegisteredTime() {
        return registeredTime;
    }

    public SparseIntArray getConsumptionEachId() {
        return consumptionEachId;
    }

    public SparseIntArray getSavingEachId() {
        return savingEachId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(groupId);
        parcel.writeString(name);
        parcel.writeString(profileImg);
        parcel.writeString(registeredTime);
        //parcel.writeParcelable(imageDownloaded, i);
        //Sometime bitmap render system bug!
        parcel.writeIntArray(deviceId);
        parcel.writeInt(consumption);
        parcel.writeInt(saving);
    }
}
