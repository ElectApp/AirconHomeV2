package com.apyeng.airconhomev2;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

public class LogExpandableListViewAdapter extends BaseExpandableListAdapter{

    private Context context;
    private List<LogItem> expandableListTitle;
    private SparseArray<List<LogItem>> expandableListDetail;

    public LogExpandableListViewAdapter(Context context, List<LogItem> expandableListTitle, SparseArray<List<LogItem>> expandableListDetail){
        this.context = context;
        this.expandableListTitle = expandableListTitle;
        this.expandableListDetail = expandableListDetail;
    }

    @Override
    public int getGroupCount() {
        return expandableListTitle.size();
    }

    @Override
    public int getChildrenCount(int i) {
        List<LogItem> items = expandableListDetail.valueAt(i);
        return items.size();
    }

    @Override
    public Object getGroup(int i) {
        //Return LogItem
        return expandableListTitle.get(i);
    }

    @Override
    public Object getChild(int listPosition, int expandedListPosition) {
        //Return LogItem
        return expandableListDetail.valueAt(listPosition).get(expandedListPosition);
    }

    @Override
    public long getGroupId(int listPosition) {
        return listPosition;
    }

    @Override
    public long getChildId(int listPosition, int expandedListPosition) {
        return expandedListPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int listPosition, boolean isExpanded, View convertView , ViewGroup viewGroup) {

        View view = LayoutInflater.from(context).inflate(R.layout.log_group_item_layout, null);

        TextView titleTxt = view.findViewById(R.id.group_title);
        TextView valueTxt = view.findViewById(R.id.sum_value);

        LogItem logItem = expandableListTitle.get(listPosition);
        titleTxt.setText(logItem.getTitleId());
        valueTxt.setText(isExpanded? "":logItem.getValue()); //Show only item not expand


        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView , ViewGroup viewGroup) {

        View view = LayoutInflater.from(context).inflate(R.layout.log_child_item_layout, null);

        TextView titleTxt = view.findViewById(R.id.child_title);
        TextView valueTxt = view.findViewById(R.id.value_txt);

        LogItem logItem = expandableListDetail.valueAt(i).get(i1);
        titleTxt.setText(logItem.getTitleId());
        valueTxt.setText(logItem.getValue());

        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false; //Disable click
    }



}
