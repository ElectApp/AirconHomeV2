package com.apyeng.airconhomev2;

import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class LabelItem {

    private String text;
    private boolean selected;
    private int id;

    public LabelItem(int id, String text, boolean selected){
        this.id = id;
        this.text = text;
        this.selected = selected;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }


    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getText() {
        return text;
    }


    public boolean isSelected() {
        return selected;
    }

    public int getId() {
        return id;
    }
}
