package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class RoundButtonWidget extends CardView implements View.OnClickListener{

    private TextView textView;
    private CardView cardView;
    private OnWidgetClickListener onClickListener;
    private int buttonColor;
    private static final String TAG = "RoundButtonWidget";

    public RoundButtonWidget(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public RoundButtonWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoundButtonWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        //Insert layout and declare View
        inflate(context, R.layout.widget_round_button, this);
        textView = findViewById(R.id.text_view);
        cardView = findViewById(R.id.card_view);
        //General
        //setRadius(cardView.getRadius());
        //setCardElevation(cardView.getCardElevation());
        //Check attribute
        if (attrs!=null){
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.RoundButtonWidget, 0, 0);
            String text = a.getString(R.styleable.RoundButtonWidget_text);
            setText(text);
            int c = a.getColor(R.styleable.RoundButtonWidget_textColor, textView.getCurrentTextColor());
            setTextColor(c);
            float s = a.getDimension(R.styleable.RoundButtonWidget_textSize, textView.getTextSize());
            setTextSize(s);
           // int f = a.getResourceId(R.styleable.RoundButtonWidget_textFont, R.font.muli_extra_bold);
            //Typeface typeface = ResourcesCompat.getFont(context, f);
            //setTextFont(typeface);
            boolean l = a.getBoolean(R.styleable.RoundButtonWidget_textAllCap, true);
            setTextAllCap(l);
            int bc = a.getColor(R.styleable.RoundButtonWidget_buttonColor, getColor(R.color.colorAccent));
            setButtonColor(bc);
            float conner = a.getDimension(R.styleable.RoundButtonWidget_conner, cardView.getRadius());
            setRadius(conner);
            float eva = a.getDimension(R.styleable.RoundButtonWidget_elevation, cardView.getCardElevation());
            setCardElevation(eva);
        }
        //Listener
        cardView.setOnClickListener(this);

    }



    private int getColor(int colorId){
        return getResources().getColor(colorId);
    }

    public void setText(String text){
        if (textView!=null && text!=null){
            textView.setText(text);
        }
    }

    public void setText(int textId){
        if (textView!=null){
            textView.setText(textId);
        }
    }

    public String getText(){
        return textView.getText().toString();
    }

    public void setTextAllCap(boolean allCap){
        if (textView!=null){
            textView.setAllCaps(allCap);
        }
    }

    //Thank: https://stackoverflow.com/questions/28071584/android-text-size-programmatically-too-big
    public void setTextSize(float size){
        if (textView!=null){
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        }
    }

    public void setTextFont(Typeface typeface){
        if (textView!=null){
            textView.setTypeface(typeface);
        }
    }

    public void setColor(int buttonColor, int textColor){
        setButtonColor(buttonColor);
        setTextColor(textColor);
    }


    public void setTextColor(int color){
        if (textView!=null){
            textView.setTextColor(color);
        }
    }

    public int getTextColor(){
        return textView.getCurrentTextColor();
    }

    public void setButtonColor(int color){
        buttonColor = color;
        cardView.setCardBackgroundColor(color);
        setCardBackgroundColor(color);
        if (color==0){
            cardView.setCardElevation(0);
            setCardElevation(0);
        }
    }

    public int getButtonColor(){
        return buttonColor;
    }

    public void setOnWidgetClickListener(OnWidgetClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public void onClick(View view) {
        if (onClickListener!=null){
            onClickListener.onClick(view);
        }
    }

    interface OnWidgetClickListener{
        void onClick(View view);
    }





}
