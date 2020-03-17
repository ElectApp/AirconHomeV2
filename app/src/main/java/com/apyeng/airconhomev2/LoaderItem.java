package com.apyeng.airconhomev2;

import android.graphics.Bitmap;

public class LoaderItem {

    private String url;
    private int id;
    private Bitmap bitmap;


    public LoaderItem(int id, String url, Bitmap bitmap){
        this.url = url;
        this.id = id;
        this.bitmap = bitmap;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
