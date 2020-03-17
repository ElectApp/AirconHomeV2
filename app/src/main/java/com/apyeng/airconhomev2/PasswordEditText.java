package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PasswordEditText extends AppCompatEditText implements View.OnTouchListener{

    private Drawable showIcon, invisibleIcon, visibleIcon;
    private OnTouchListener mOnTouchListener;
    private boolean show = false;

    public PasswordEditText(Context context) {
        super(context);
        init(context);
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        //super.setOnTouchListener(l);
        mOnTouchListener = l;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int x = (int) event.getX();
        if (showIcon.isVisible() && x > getWidth() - getPaddingRight() - showIcon.getIntrinsicWidth()) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                //Thank: https://wajahatkarim.com/2018/11/show/hide-password-in-edittext-in-android/
                if (!show){
                    // Show Password
                    showPassword(true);
                }else {
                    // Hide Password
                    showPassword(false);
                }

            }
            return true;
        }
        return mOnTouchListener != null && mOnTouchListener.onTouch(v, event);
    }


    private void init(final Context context) {
        
        final Drawable drawable1 = ContextCompat.getDrawable(context, R.drawable.ic_visibility_off);
        final Drawable drawable2 = ContextCompat.getDrawable(context, R.drawable.ic_visibility);
        
        if (drawable1!=null && drawable2!=null){
            //Set visible icon
            final Drawable wrappedDrawable1 = DrawableCompat.wrap(drawable1); //Wrap the drawable so that it can be tinted pre Lollipop
            DrawableCompat.setTint(wrappedDrawable1, getCurrentHintTextColor());
            invisibleIcon = wrappedDrawable1;
            invisibleIcon.setBounds(0, 0, invisibleIcon.getIntrinsicHeight(), invisibleIcon.getIntrinsicHeight());
            //Set invisible icon
            final Drawable wrappedDrawable2 = DrawableCompat.wrap(drawable2); //Wrap the drawable so that it can be tinted pre Lollipop
            DrawableCompat.setTint(wrappedDrawable2, getCurrentHintTextColor());
            visibleIcon = wrappedDrawable2;
            visibleIcon.setBounds(0, 0, visibleIcon.getIntrinsicHeight(), visibleIcon.getIntrinsicHeight());

            //Initial hide password
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            //Initial showIcon to invisible icon
            setShowIcon(false);
            //Add touchListener
            super.setOnTouchListener(this);
        }

    }

    private void setShowIcon(final boolean visible) {
        if (visible){
            showIcon = visibleIcon; //Set icon
            show = true; //Set flag
        }else {
            showIcon = invisibleIcon; //Set icon
            show = false; //Set flag
        }
        final Drawable[] compoundDrawables = getCompoundDrawables();
        setCompoundDrawables(
                compoundDrawables[0],
                compoundDrawables[1],
                visible ? visibleIcon : invisibleIcon,
                compoundDrawables[3]);
        //Move cursor to end text on EditText
        setSelection(length());
    }


    public void showPassword(boolean show){
        if (show){
            // Show Password
            setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            //Change icon to visible
            setShowIcon(true);
        }else {
            // Hide Password
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            //Change icon to invisible
            setShowIcon(false);
        }
    }


}
