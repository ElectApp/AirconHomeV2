package com.apyeng.airconhomev2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

//Use for show ExpandableListView in NestScrollView
//Thank: http://vjscrazzy.blogspot.com/2017/08/fixed-andorid-expandablelistview-issue.html
public class NonScrollExpandableListView extends ExpandableListView {

    public NonScrollExpandableListView(Context context) {
        super(context);
    }

    public NonScrollExpandableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NonScrollExpandableListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMeasureSpec_custom = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec_custom);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = getMeasuredHeight();
    }
}
