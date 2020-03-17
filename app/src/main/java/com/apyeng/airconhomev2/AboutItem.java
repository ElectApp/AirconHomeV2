package com.apyeng.airconhomev2;

public class AboutItem {

    private String title;
    private String detail;
    private boolean groupDivider;

    public AboutItem(String title, String detail, boolean groupDivider){
        this.title = title;
        this.detail = detail;
        this.groupDivider = groupDivider;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setGroupDivider(boolean groupDivider) {
        this.groupDivider = groupDivider;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isGroupDivider() {
        return groupDivider;
    }



}
