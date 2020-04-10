package com.apyeng.airconhomev2;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ModbusItemAdapter extends RecyclerView.Adapter<ModbusItemAdapter.ViewHolder>{

    private List<ModbusItem> mItems;
    private ItemClickListener mListener;

    ModbusItemAdapter(List<ModbusItem> mItems){
        this.mItems = mItems;
    }

    @NonNull
    @Override
    public ModbusItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_mb_device, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ModbusItemAdapter.ViewHolder viewHolder, int i) {

        ModbusItem item = mItems.get(i);
        viewHolder.idTv.setText(item.getSlaveID());
        viewHolder.nameTv.setText(item.getDeviceName());

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView idTv, nameTv;

        ViewHolder(View itemView) {
            super(itemView);

            idTv = itemView.findViewById(R.id.id_txt);
            nameTv = itemView.findViewById(R.id.name_txt);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener!=null){
                        int p = getAdapterPosition();
                        mListener.onItemClick(p, mItems.get(p));
                    }
                }
            });
        }

    }

    public void setItemClickListener(ItemClickListener listener){
        mListener = listener;
    }

    interface ItemClickListener {
        void onItemClick(int p, ModbusItem item);
    }
}
