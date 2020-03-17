package com.apyeng.airconhomev2;

public class SheetItem {

    private int iconId;
    private String name;

    public SheetItem(int iconId, String name){
        this.iconId = iconId;
        this.name = name;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIconId() {
        return iconId;
    }

    public String getName() {
        return name;
    }

}
