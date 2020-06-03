package com.apyeng.airconhomev2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class NetworkItemAdapter extends RecyclerView.Adapter<NetworkItemAdapter.ViewHolder>{

    private ArrayList<NetworkItem> items;
    private Context context;
    private OnClickItemListener onClickItemListener;
    private OnHoldPressItemListener onHoldPressItemListener;

    public NetworkItemAdapter(Context context, ArrayList<NetworkItem> items){
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_network, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder v, int i) {
        //Get data
        NetworkItem item = items.get(i);
        //Set data
        v.tvSSID.setText(item.ssid);
        v.tvBSSID.setText(item.status!=null? item.status:item.bssid);
        v.setRSSI(item.rssi);
        String t = "";
        if (item.auth!=-1){
            t += item.auth>0? "Secured":"Open";
            t += "\n";
        }
        t += item.rssi+" dBm";
        v.tvRSSI.setText(t);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvSSID, tvBSSID, tvRSSI;
        ImageView iRSSI;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSSID = itemView.findViewById(R.id.tv_ssid);
            tvBSSID = itemView.findViewById(R.id.tv_bssid);
            tvRSSI = itemView.findViewById(R.id.tv_rssi);
            iRSSI = itemView.findViewById(R.id.i_rssi);

            if (onClickItemListener!=null){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int p = getAdapterPosition();
                        onClickItemListener.onClick(p, items.get(p));
                    }
                });
            }

            if(onHoldPressItemListener!=null){
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int p = getAdapterPosition();
                        onHoldPressItemListener.onPress(p, items.get(p));
                        return false;
                    }
                });
            }
        }

        private void setRSSI(int rssi){
            //RSSI
            String dbm = rssi + " dBm";
            tvRSSI.setText(dbm);
            //Image
            iRSSI.setImageResource(Function.getResWiFiLevelIcon(rssi));
        }

    }

    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {
        this.onClickItemListener = onClickItemListener;
    }

    public void setOnHoldPressItemListener(OnHoldPressItemListener onHoldPressItemListener) {
        this.onHoldPressItemListener = onHoldPressItemListener;
    }

    public interface OnClickItemListener{
        void onClick(int position, NetworkItem item);
    }

    public interface OnHoldPressItemListener{
        void onPress(int position, NetworkItem item);
    }
}
