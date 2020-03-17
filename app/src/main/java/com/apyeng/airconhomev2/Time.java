package com.apyeng.airconhomev2;

import android.os.Parcel;
import android.os.Parcelable;

public class Time implements Parcelable {

    private int hour, minute;
    private String text, timerText;

    public Time(int hour, int minute){
        this.hour = hour;
        this.minute = minute;
        //Set text
        this.text = get2Digit(hour)+":"+get2Digit(minute);
        //Set timer text
        String h = hour>0? String.valueOf(hour)+" h. " : "";
        this.timerText = h+String.valueOf(minute)+" m.";
    }

    protected Time(Parcel in) {
        hour = in.readInt();
        minute = in.readInt();
        text = in.readString();
        timerText = in.readString();
    }

    public static final Creator<Time> CREATOR = new Creator<Time>() {
        @Override
        public Time createFromParcel(Parcel in) {
            return new Time(in);
        }

        @Override
        public Time[] newArray(int size) {
            return new Time[size];
        }
    };

    public int getHour(){
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String get2Digit(int value){
        if (value<10){
            return "0"+ String.valueOf(value);
        }
        return String.valueOf(value);
    }

    public String getText() {
        return text;
    }

    public String getTimerText() {
        return timerText;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(hour);
        parcel.writeInt(minute);
        parcel.writeString(text);
        parcel.writeString(timerText);
    }
}
