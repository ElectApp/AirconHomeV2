package com.apyeng.airconhomev2;

import android.util.SparseIntArray;

import java.util.List;

public class ExpandItem {

    public LogItem groupItem;
    public List<LogItem> childItems;

    public ExpandItem(LogItem groupItem, List<LogItem> childItems){
        this.groupItem = groupItem;
        this.childItems = childItems;
    }



    public void setGroupItem(LogItem groupItem) {
        this.groupItem = groupItem;
    }

    public void setChildItems(List<LogItem> childItems) {
        this.childItems = childItems;
    }

    public LogItem getGroupItem() {
        return groupItem;
    }

    public List<LogItem> getChildItems() {
        return childItems;
    }



}
