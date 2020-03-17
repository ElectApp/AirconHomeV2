package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextDegreeWidget extends LinearLayout {

    private TextView numTxt, unitTxt;

    public TextDegreeWidget(Context context) {
        super(context);
        init(context, null);
    }

    public TextDegreeWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        //Set layout and declare View
        inflate(context, R.layout.widget_text_degree, this);
        numTxt = findViewById(R.id.tv_value);
        unitTxt = findViewById(R.id.tv_unit);
        //Set attrs
        if (attrs!=null){
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.TextDegreeWidget, 0, 0);
            float ns = a.getDimension(R.styleable.TextDegreeWidget_txt_deg_numberSize, numTxt.getTextSize());
            String nt = a.getString(R.styleable.TextDegreeWidget_txt_deg_number);
            setNumber(nt, ns);
            float us = a.getDimension(R.styleable.TextDegreeWidget_txt_deg_unitSize, unitTxt.getTextSize());
            String ut = a.getString(R.styleable.TextDegreeWidget_txt_deg_unit);
            setUnit(ut, us);
            int color = a.getColor(R.styleable.TextDegreeWidget_txt_deg_textColor, numTxt.getCurrentTextColor());
            setTextColor(color);
        }

    }

    public void setNumber(String number){
        numTxt.setText(number);
    }

    public void setUnit(String unit){
        unitTxt.setText(unit);
    }

    public void setNumber(String number, float size){
        numTxt.setText(number);
        setNumberSize(size);
    }

    public void setUnit(String unit, float size){
        unitTxt.setText(unit);
        setUnitSize(size);
    }

    public void setNumberSize(float size){
        numTxt.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setUnitSize(float size){
        unitTxt.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(int color){
        numTxt.setTextColor(color);
        unitTxt.setTextColor(color);
    }

    public void setNumberVisibility(int visibility){
        numTxt.setVisibility(visibility);
    }

    public void setUnitVisibility(int visibility){
        unitTxt.setVisibility(visibility);
    }



}
