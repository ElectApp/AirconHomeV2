package com.apyeng.airconhomev2;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class LabelItem implements Parcelable {

    private String text, details;
    private boolean selected;
    private int id, totalDevices;

    public LabelItem(int id, String text, int totalDevices, boolean selected){
        this.id = id;
        this.text = text;
        this.totalDevices = totalDevices;
        this.selected = selected;
        this.details = null;
    }

    public LabelItem(int id, String text, String details, boolean selected){
        this.id = id;
        this.text = text;
        this.details = details;
        this.selected = selected;
        this.totalDevices = 0;
    }

    protected LabelItem(Parcel in) {
        text = in.readString();
        details = in.readString();
        selected = in.readByte() != 0;
        id = in.readInt();
        totalDevices = in.readInt();
    }

    public static final Creator<LabelItem> CREATOR = new Creator<LabelItem>() {
        @Override
        public LabelItem createFromParcel(Parcel in) {
            return new LabelItem(in);
        }

        @Override
        public LabelItem[] newArray(int size) {
            return new LabelItem[size];
        }
    };

    public void setId(int id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTotalDevices(int totalDevices) {
        this.totalDevices = totalDevices;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getText() {
        return text;
    }

    public int getTotalDevices() {
        return totalDevices;
    }

    public boolean isSelected() {
        return selected;
    }

    public int getId() {
        return id;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(details);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeInt(id);
        dest.writeInt(totalDevices);
    }
}
