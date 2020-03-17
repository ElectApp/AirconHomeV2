package com.apyeng.airconhomev2;

public class LogItem {

    public int titleId;
    public String value;
    public String title;
    public int colorLabel;


    public LogItem(int titleId, String value){
        this.titleId = titleId;
        this.value = value;
    }

    public LogItem(String title, String value){
        this.title = title;
        this.value = value;
    }

    public LogItem(String title, String value, int colorLabel){
        this.title = title;
        this.value = value;
        this.colorLabel = colorLabel;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitleId(int titleId) {
        this.titleId = titleId;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getTitleId() {
        return titleId;
    }

    public String getValue() {
        return value;
    }

}
