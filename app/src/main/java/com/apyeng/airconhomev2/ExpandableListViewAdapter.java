package com.apyeng.airconhomev2;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

public class ExpandableListViewAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<ExpandItem> items;
    private static final String TAG = "ExpandableAdapter";

    public ExpandableListViewAdapter(Context context, List<ExpandItem> items){
        this.context = context;
        this.items = items;
    }


    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return items.get(i).childItems.size();
    }

    @Override
    public Object getGroup(int i) {
        return items.get(i).groupItem;
    }

    @Override
    public Object getChild(int i, int i1) {
        return items.get(i).childItems.get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean isExpanded, View view, ViewGroup viewGroup) {

        View v = LayoutInflater.from(context).inflate(R.layout.log_group_item_layout, null);
        TextView titleTxt = v.findViewById(R.id.group_title);
        TextView valueTxt = v.findViewById(R.id.sum_value);

        //Get group item
        LogItem group = items.get(i).groupItem;

        //Set title
        int id = group.titleId;
        if (id>0){
            titleTxt.setText(id);
        }else {
            String t = group.title;
            titleTxt.setText(t!=null? t : "");
        }

        //Set value
        String d = group.value;
        valueTxt.setText(d!=null? d : "");

        /*
        //Show only item not expand
        if (d!=null && !isExpanded){
            valueTxt.setText(d);
        }else {
            valueTxt.setText("");
        }
        */

        return v;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {

        View v = LayoutInflater.from(context).inflate(R.layout.log_child_item_layout, null);

        View tagV = v.findViewById(R.id.label_color);
        TextView titleTxt = v.findViewById(R.id.child_title);
        TextView valueTxt = v.findViewById(R.id.value_txt);


        LogItem child = items.get(i).childItems.get(i1);

        //Set color
        int color = child.colorLabel;
        Log.w(TAG, "Update Child Color: "+color);
        if (color!=0){
            tagV.setBackgroundColor(color);
        }

        //Set title
        int id = child.titleId;
        if (id>0){
            titleTxt.setText(id);
        }else {
            String t = child.title;
            titleTxt.setText(t!=null? t : "");
        }

        //Set value
        String d = child.value;
        valueTxt.setText(d!=null? d : "");

        return v;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false; //Disable click on child item
    }
}
