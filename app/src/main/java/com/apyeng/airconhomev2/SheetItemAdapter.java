package com.apyeng.airconhomev2;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class SheetItemAdapter extends RecyclerView.Adapter<SheetItemAdapter.ViewHolder> {

    private List<SheetItem> mItems;
    private ItemListener mListener;
    private int id;

    SheetItemAdapter(int id, List<SheetItem> mItems, ItemListener mListener){
        this.id = id;
        this.mItems = mItems;
        this.mListener = mListener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bottom_sheet_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        SheetItem item = mItems.get(i);
        //Set icon
        int icon = item.getIconId();
        if (icon>0){
            holder.icon.setImageResource(icon);
            holder.icon.setVisibility(View.VISIBLE);
        }else {
            holder.icon.setVisibility(View.GONE);
        }
        //Set detail
        holder.textView.setText(item.getName());

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private ImageView icon;
        private TextView textView;

        ViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.menu_icon);
            textView = itemView.findViewById(R.id.text_view);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (mListener!=null){
                        int p = getAdapterPosition();
                        mListener.onItemClick(id, p, mItems.get(p));
                    }
                }
            });
        }


    }

    public void setItemListener(ItemListener listener){
        mListener = listener;
    }

    interface ItemListener {
        void onItemClick(int id, int numberSelected, SheetItem itemSelected);
    }


}
