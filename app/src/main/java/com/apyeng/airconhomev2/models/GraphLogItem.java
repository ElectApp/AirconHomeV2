package com.apyeng.airconhomev2.models;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class GraphLogItem {

    public int iconId;
    public String title, details, unit, format;
    public ArrayList<String> graphLabel;
    public ArrayList<Entry> graphData;
    public boolean checked;

    public GraphLogItem(boolean checked, int iconId, String title, String details, String unit, ArrayList<String> graphLabel, ArrayList<Entry> graphData){
        this.checked = checked;
        this.iconId = iconId;
        this.title = title;
        this.details = details;
        this.unit = unit;
        this.graphLabel = graphLabel;
        this.graphData = graphData;
    }

}
