package com.apyeng.airconhomev2;

public class SheetItem {

    private int iconId, itemId;
    private String name;

    public SheetItem(int iconId, String name){
        this.iconId = iconId;
        this.name = name;
    }

    public SheetItem(int iconId, String name, int itemId){
        this.iconId = iconId;
        this.name = name;
        this.itemId = itemId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getIconId() {
        return iconId;
    }

    public String getName() {
        return name;
    }

    public int getItemId() {
        return itemId;
    }
}
