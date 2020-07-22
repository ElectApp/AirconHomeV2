package com.apyeng.airconhomev2.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.apyeng.airconhomev2.LabelItem;
import com.apyeng.airconhomev2.R;

import java.util.List;

public class LabelDetailAdapter extends RecyclerView.Adapter<LabelDetailAdapter.ViewHolder>{

    private Context context;
    private List<LabelItem> items;
    private boolean editMode;
    private OnLabelClickListener onLabelClickListener;
    private OnLabelLongClickListener onLabelLongClickListener;

    public LabelDetailAdapter(Context context, List<LabelItem> items){
        this.context = context;
        this.items = items;
        editMode = false;
    }

    @NonNull
    @Override
    public LabelDetailAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.label_details_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabelDetailAdapter.ViewHolder vh, int i) {
        LabelItem item = items.get(i);
        String d;
        if (item.getDetails()!=null){
            d = item.getDetails();
        }else {
            d = String.valueOf(item.getTotalDevices())+" "+context.getResources().getString(R.string.devices);
        }

        vh.titleTv.setText(item.getText());
        vh.detailTv.setText(d);
        vh.checkedImg.setImageResource(item.isSelected()? R.drawable.check_icon:R.drawable.ic_check2);
        vh.checkedImg.setVisibility(editMode? View.VISIBLE:View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setOnLabelClickListener(OnLabelClickListener onLabelClickListener) {
        this.onLabelClickListener = onLabelClickListener;
    }

    public void setOnLabelLongClickListener(OnLabelLongClickListener onLabelLongClickListener) {
        this.onLabelLongClickListener = onLabelLongClickListener;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView titleTv, detailTv;
        private ImageView checkedImg;

        public ViewHolder(@NonNull final View v) {
            super(v);

            titleTv = v.findViewById(R.id.title_tv);
            detailTv = v.findViewById(R.id.detail_tv);
            checkedImg = v.findViewById(R.id.checked_icon);

            //Short click
            if (onLabelClickListener!=null){
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Callback
                        int p = getAdapterPosition();
                        onLabelClickListener.onClick(v, items.get(p), p);
                    }
                });
            }

            //Hold press
            if (onLabelLongClickListener!=null){
                v.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //Show check icon
                        int p = getAdapterPosition();
                        onLabelLongClickListener.onClick(v, items.get(p), p);
                        return false;
                    }
                });
            }

        }

    }

    public interface OnLabelClickListener {
        void onClick(View view, LabelItem labelItem, int position);
    }

    public interface OnLabelLongClickListener {
        void onClick(View view, LabelItem labelItem, int position);
    }


}
