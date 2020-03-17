package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.ViewHolder> {

    private Context context;
    private List<LabelItem> items;
    private OnLabelClickListener labelClickListener;
    private Drawable labelIcon, addLabelIcon;
    private int bgSelected, bgNormal;
    private SparseIntArray iconColor, textColor, btnColor;
    private boolean autoSetSelected = true;
    private static final String TAG = "LabelAdapter";

    public LabelAdapter(Context context, List<LabelItem> items){
        this.context = context;
        this.items = items;
        labelIcon = context.getDrawable(R.drawable.label_icon);
        addLabelIcon = context.getDrawable(R.drawable.add_label_icon);
        bgSelected = context.getResources().getColor(R.color.colorPrimary);
        bgNormal = context.getResources().getColor(R.color.colorGray2);
        iconColor = new SparseIntArray();
        textColor = new SparseIntArray();
        btnColor = new SparseIntArray();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.label_layout, viewGroup, false);

        Log.w(TAG, "Create view holder: "+viewGroup.getLayoutDirection());

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabelAdapter.ViewHolder holder, int i) {
        //Get data
        LabelItem data = items.get(i);
        //Set on item layout
        String text = data.getText();
        holder.widget.setText(text!=null? text : "");
        Log.w(TAG, "Position: "+i+", "+data.isSelected());

        if (data.isSelected()){
            holder.setLabelColor(Color.WHITE, Color.WHITE, bgSelected);
        }else {
            holder.setLabelColor(bgSelected, bgSelected, bgNormal);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private ImageTextButtonWidget widget;

        public ViewHolder(@NonNull final View v) {
            super(v);

            widget = v.findViewById(R.id.labels_btn);
            //Set listener
            widget.setOnWidgetClickListener(new ImageTextButtonWidget.OnWidgetClickListener() {
                @Override
                public void onClick(View view) {
                    Log.w(TAG, "Click on widget: "+getAdapterPosition());

                    if (labelClickListener!=null){
                        int p = getAdapterPosition();
                        labelClickListener.onClick(view, items.get(p), p);

                        if (autoSetSelected){
                            boolean state = items.get(p).isSelected();

                            setLabelSelected(p, !state);
                        }
                    }

                }
            });


        }

        private void setLabelColor(int icon, int text, int bg){
            //Clear color icon before set new color
            widget.clearIconColor();
            //Set new color
            widget.setIconColor(icon);
            widget.setTextColor(text);
            widget.setButtonColor(bg);
        }

        private void setLabelSelected(int p, boolean selected){
            //Get old state
            LabelItem item = items.get(p);
            item.setSelected(selected);
            items.set(p, item);
            //Update
            notifyDataSetChanged();
        }

    }




    public void clearSelected(int exceptPosition){
        //Set selected to false
        for (int i=0; i<items.size(); i++){
            if (i!=exceptPosition){
                items.get(i).setSelected(false);
            }
        }
        //Update
        notifyDataSetChanged();
    }

    public LabelItem getLabel(int p){
        return items.get(p);
    }

    public void addLabels(List<LabelItem> labelItems){
        int start = items.size();
        items.addAll(labelItems);
        //Update adapter
        notifyItemRangeInserted(start, labelItems.size());
    }

    public List<LabelItem> getItems() {
        return items;
    }

    public void setLabelClickListener(OnLabelClickListener listener){
        labelClickListener = listener;
    }

    interface OnLabelClickListener{
        void onClick(View view, LabelItem labelItem, int position);
    }


}
