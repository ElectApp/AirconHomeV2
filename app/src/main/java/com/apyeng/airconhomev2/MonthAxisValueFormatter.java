package com.apyeng.airconhomev2;

import android.content.Context;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class MonthAxisValueFormatter extends ValueFormatter {

    private String months[];

    public MonthAxisValueFormatter(Context context){
        this.months = context.getResources().getStringArray(R.array.short_month);
    }

    @Override
    public String getFormattedValue(float value) {
        //return super.getFormattedValue(value);
        int m = (int)(value-1f);
        if (m>=0 && m<months.length){
            return months[m];
        }
        return super.getFormattedValue(value);
    }


}
