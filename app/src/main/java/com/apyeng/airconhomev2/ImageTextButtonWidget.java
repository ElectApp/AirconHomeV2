package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ImageTextButtonWidget extends CardView implements View.OnClickListener{

    private CardView rootView;
    private ImageView imageView;
    private TextView textView;
    private float initEv;
    private int iconSize;
    public static final int HIDE_NONE = 0;
    public static final int HIDE_ICON = 1;
    public static final int HIDE_TEXT = 2;
    public static final int TEXT_BOLD = 0;
    public static final int TEXT_NORMAL = 1;
    private OnWidgetClickListener clickListener;

    public ImageTextButtonWidget(Context context) {
        super(context);
        init(context, null);
    }

    public ImageTextButtonWidget(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ImageTextButtonWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs){
        //Insert layout and declare View
        inflate(context, R.layout.widget_image_text_button, this);

        rootView = findViewById(R.id.root_view);
        imageView = findViewById(R.id.image_view);
        textView = findViewById(R.id.text_view);
        //Check attribute
        if (attrs!=null){
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.ImageTextButtonWidget, 0, 0);
            //Must set textView style before set textSize !!!!
            int style = a.getInteger(R.styleable.ImageTextButtonWidget_img_btn_textStyle, TEXT_BOLD);
            setTextStyle(style);
            String text = a.getString(R.styleable.ImageTextButtonWidget_img_btn_text);
            setText(text);
            float ts = a.getDimension(R.styleable.ImageTextButtonWidget_img_btn_textSize, textView.getTextSize());
            setTextSize(ts);
            int tc = a.getColor(R.styleable.ImageTextButtonWidget_img_btn_textColor, textView.getCurrentTextColor());
            setTextColor(tc);
            boolean tac = a.getBoolean(R.styleable.ImageTextButtonWidget_img_btn_textAllCap, false);
            setTextAllCap(tac);
            iconSize = (int)a.getDimension(R.styleable.ImageTextButtonWidget_img_btn_iconSize, getDimen(R.dimen.g_ic_s));
            setIconSize(iconSize);
            int i = a.getResourceId(R.styleable.ImageTextButtonWidget_img_btn_icon, R.drawable.app_icon_white);
            setIcon(getDrawable(i));
            int ip = (int)a.getDimension(R.styleable.ImageTextButtonWidget_img_btn_iconPadding, imageView.getPaddingStart());
            setIconPadding(ip);
            int ic = a.getColor(R.styleable.ImageTextButtonWidget_img_btn_backgroundColor, getColor(R.color.colorAccent));
            setButtonColor(ic);
            float ics = a.getDimension(R.styleable.ImageTextButtonWidget_img_btn_conner, rootView.getRadius());
            setButtonConner(ics);
            int iconColor = a.getColor(R.styleable.ImageTextButtonWidget_img_btn_iconColor, Color.WHITE);
            setIconColor(iconColor);
            float bElev = a.getDimension(R.styleable.ImageTextButtonWidget_img_btn_elevation, getDimen(R.dimen.g_eva));
            setButtonElevation(bElev);
            initEv = bElev;
            int hideFlag = a.getInteger(R.styleable.ImageTextButtonWidget_img_btn_hide, HIDE_NONE);
            setButtonHide(hideFlag);
        }
        //Click
        rootView.setOnClickListener(this);

    }

    private int getColor(int colorId){
        return getResources().getColor(colorId);
    }

    private float getDimen(int dimenId){ return getResources().getDimension(dimenId); }

    private Drawable getDrawable(int drawableId){ return getResources().getDrawable(drawableId); }

    public void setText(String text){
        textView.setText(text);
    }

    public void setText(int textId){
        textView.setText(textId);
    }

    public void setTextSize(float size){
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(int color){
        textView.setTextColor(color);
    }

    public int getTextColor(){
        return textView.getCurrentTextColor();
    }

    public void setTextAllCap(boolean allCap){
        textView.setAllCaps(allCap);
    }

    //Thank: https://stackoverflow.com/questions/3144940/set-imageview-width-and-height-programmatically
    public void setIconSize(int size){
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.height = size;
        params.width = size;
        imageView.requestLayout();
        iconSize = size;
    }

    public void setIcon(Drawable drawable){
        imageView.setImageDrawable(drawable);
    }

    public void setIconPadding(int padding){
        imageView.setPadding(padding, padding, padding, padding);
    }


    public void setIconColor(int color){
        imageView.setColorFilter(color);
    }

    public void clearIconColor(){
        imageView.clearColorFilter();
    }

    public void setButtonColor(int color){
        rootView.setCardBackgroundColor(color);
        setCardBackgroundColor(color);
        if (color==0){
            rootView.setCardElevation(0);
            setCardElevation(0);
        }else {
            setButtonElevation(initEv);
        }
    }

    public void setButtonElevation(float elevation){
        rootView.setCardElevation(elevation);
        setCardElevation(elevation);
        initEv = elevation;
    }

    public void setButtonConner(float conner){
        rootView.setRadius(conner);
        rootView.setContentPadding(imageView.getPaddingLeft(), 0, (int) conner, 0);
        setRadius(conner);
        //setContentPadding(imageView.getPaddingLeft(), 0, (int) conner, 0);
    }

    public void setButtonHide(int flag){
        switch (flag){
            case HIDE_ICON:
                    imageView.setVisibility(GONE);
                    //Set textView height
                    ViewGroup.LayoutParams params = textView.getLayoutParams();
                    params.height = iconSize;
                    textView.requestLayout();
                    //Set padding
                    int p = (int)textView.getTextSize();
                    rootView.setContentPadding(p, 0, p, 0);
                    break;
            case HIDE_TEXT:
                textView.setVisibility(GONE);
                int a = imageView.getPaddingLeft();
                rootView.setContentPadding(a, 0, a, 0);
                break;
            default:    imageView.setVisibility(VISIBLE);
                        textView.setVisibility(VISIBLE);
        }
    }

    public void setTextStyle(int flag){
        switch (flag){
            case TEXT_BOLD:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setTextAppearance(R.style.AppTheme_TextBold);
                }
                break;
            case TEXT_NORMAL:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setTextAppearance(R.style.AppTheme_TextNormal);
                }
                break;
        }
    }



    public void setOnWidgetClickListener(OnWidgetClickListener clickListener){
        this.clickListener = clickListener;
    }

    @Override
    public void onClick(View view) {
        if (clickListener!=null){
            clickListener.onClick(view);
        }
    }

    interface OnWidgetClickListener{
        void onClick(View view);
    }

}
