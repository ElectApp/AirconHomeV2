package com.apyeng.airconhomev2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

//Modified by Somsak Elect, 21/06/2019

public class MyNestScrollView extends NestedScrollView implements View.OnTouchListener{

    private GestureDetector gestureDetector;
    private OnSwipeListener swipeListener;

    public MyNestScrollView(@NonNull Context context) {
        super(context);

        init(context);
    }

    public MyNestScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public MyNestScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //For swipe
        return gestureDetector.onTouchEvent(motionEvent);
    }


    private void init(Context context){
        //Create GestureDetector object
        gestureDetector = new GestureDetector(context, new GestureListener());
        //Add touch listener
        setOnTouchListener(this);
    }

    //Thank: https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures/19506010#19506010
    class GestureListener extends GestureDetector.SimpleOnGestureListener{

        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            super.onFling(e1, e2, velocityX, velocityY);

            if (swipeListener==null){ return false; }

            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();
            if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0){
                    swipeListener.toRight();
                } else{
                    swipeListener.toLeft();
                }
                return true;
            }
            return false;

        }

    }

    public void addSwipeListener(OnSwipeListener swipeListener){
        this.swipeListener = swipeListener;
    }

    interface OnSwipeListener{
        void toLeft();
        void toRight();
    }

}
