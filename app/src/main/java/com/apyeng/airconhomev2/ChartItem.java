package com.apyeng.airconhomev2;

import android.util.SparseArray;

import java.util.List;
import java.util.Map;

public class ChartItem {

    public String time;
    public float values[];


    public double acValue, pvValue;

    public ChartItem(String time, double acValue, double pvValue){
        this.time = time;
        this.acValue = acValue;
        this.pvValue = pvValue;
    }

    public ChartItem(String time, float values[]){
        this.time = time;
        this.values = values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }

    public float[] getValues() {
        return values;
    }


    public void setTime(String time) {
        this.time = time;
    }

    public void setAcValue(double acValue) {
        this.acValue = acValue;
    }

    public void setPvValue(double pvValue) {
        this.pvValue = pvValue;
    }

    public double getAcValue() {
        return acValue;
    }

    public double getPvValue() {
        return pvValue;
    }

    public String getTime() {
        return time;
    }

}
