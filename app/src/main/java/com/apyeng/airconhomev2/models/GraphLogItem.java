package com.apyeng.airconhomev2.models;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class GraphLogItem {

    public int iconId;
    public String title, details, unit, format, logKey;
    public ArrayList<String> graphLabel;
    public ArrayList<Entry> graphData;
    public boolean checked;
    public float min;

    public GraphLogItem(String logKey, boolean checked, int iconId, String title, String details, String format, String unit, ArrayList<String> graphLabel, ArrayList<Entry> graphData){
        this.logKey = logKey;
        this.checked = checked;
        this.iconId = iconId;
        this.title = title;
        this.details = details;
        this.format = format;
        this.unit = unit;
        this.graphLabel = graphLabel;
        this.graphData = graphData;
    }

}
