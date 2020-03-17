package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class AirItemAdapter extends RecyclerView.Adapter<AirItemAdapter.ViewHolder> {

    private Context context;
    private List<AirItem> items;
    private OnAirItemClickListener airItemClickListener;
    public static final int OFF_MODE = -1;
    private static final String TAG = "AirItemAdapter";

    public AirItemAdapter(Context context, List<AirItem> items){
        this.context = context;
        this.items = items;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.air_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AirItemAdapter.ViewHolder holder, int i) {
        //Get data
        AirItem data = items.get(i);
        //Set basic data
        String name = data.getNickname();
        holder.name.setText(name!=null? name : "");
        //Set indoor value to item
        Indoor indoor = data.getIndoor();
        if (indoor!=null){
            //Show room widget
            holder.roomWidget.setVisibility(View.VISIBLE);
            //Set power on or power off
            if (!indoor.onoff || indoor.tripCode!=0){
                //Invisible mode, speed icon, set point
                holder.setModeIcon(OFF_MODE);
                holder.setSpeedIcon(OFF_MODE);
                holder.setSetPoint(OFF_MODE);
            }else {
                //Show mode icon, speed icon, set point
                holder.setModeIcon(indoor.mode);
                holder.setSpeedIcon(indoor.fan);
                holder.setSetPoint(indoor.setPointTemp);
            }
            holder.warningIcon.setVisibility(indoor.tripCode!=0? View.VISIBLE : View.GONE);
            holder.setPowerLine(indoor.onoff);
            holder.setRoomWidget(indoor.roomTemp);
            //Update air item status
            holder.setOffline(i, false);
        }else {
            //Show item is offline
            holder.setOffline(i, true);
            //Invisible mode, speed icon, set point, warning
            holder.setModeIcon(OFF_MODE);
            holder.setSpeedIcon(OFF_MODE);
            holder.setSetPoint(OFF_MODE);
            holder.warningIcon.setVisibility(View.GONE);
            //Invisible room widget
            holder.roomWidget.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private LinearLayout setPointLay;
        private TextView setPointValue;
        private TextView name;
        private ImageView modeIcon, speedIcon, warningIcon;
        private ImageTextButtonWidget roomWidget;
        private View powerLine;

        public ViewHolder(@NonNull final View v) {
            super(v);

            setPointLay = v.findViewById(R.id.set_point_lay);
            setPointValue = v.findViewById(R.id.set_point_value);
            name = v.findViewById(R.id.air_name);
            modeIcon = v.findViewById(R.id.mode_icon);
            speedIcon = v.findViewById(R.id.speed_icon);
            warningIcon = v.findViewById(R.id.warning_icon);
            roomWidget = v.findViewById(R.id.room_temp);
            powerLine = v.findViewById(R.id.power_line);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (airItemClickListener!=null){
                        int p = getAdapterPosition();
                        AirItem item = items.get(p);
                        airItemClickListener.onClick(view, p, item);
                    }
                }
            });

        }


        private void setModeIcon(int mode){
            if (mode!=OFF_MODE){
                modeIcon.setVisibility(View.VISIBLE);
                switch (mode){
                    case 0: //Fan
                        modeIcon.setImageDrawable(getDrawable(R.drawable.fan_icon));
                        break;
                    case 2: //Dry
                        modeIcon.setImageDrawable(getDrawable(R.drawable.dry_icon));
                        break;
                    case 3: //Cool
                        modeIcon.setImageDrawable(getDrawable(R.drawable.cool_icon));
                        break;
                }
            }else {
                modeIcon.setVisibility(View.INVISIBLE);
            }

        }

        private void setSpeedIcon(int speed){
            int icon[] = new int[]{ R.drawable.fan_speed_1_icon, R.drawable.fan_speed_2_icon,
                    R.drawable.fan_speed_3_icon, R.drawable.auto_icon };
            if (speed!=OFF_MODE && speed<icon.length){
                speedIcon.setImageDrawable(getDrawable(icon[speed]));
                speedIcon.setVisibility(View.VISIBLE);
            }else {
                speedIcon.setVisibility(View.INVISIBLE);
            }
        }

        private Drawable getDrawable(int resId){
            return context.getResources().getDrawable(resId);
        }

        private int getColor(int resId){
            return context.getResources().getColor(resId);
        }

        private void setPowerLine(boolean powerOn){
            powerLine.setBackgroundColor(powerOn?
                    getColor(R.color.powerOn) : getColor(R.color.powerOff));
        }

        private void setOffline(int p, boolean offline){
            //Set power line to offline color
            if (offline){ powerLine.setBackgroundColor(getColor(R.color.colorOffline)); }
            //Update and Save
            AirItem item = items.get(p);
            item.setOnline(!offline);
            items.set(p, item);
        }


        private void setSetPoint(int value){
            if (value!=OFF_MODE){
                setPointLay.setVisibility(View.VISIBLE);
                setPointValue.setText(Function.get1Digit(value));
            }else {
                setPointLay.setVisibility(View.INVISIBLE);
            }
        }

        private void setRoomWidget(int value){
            float av = value/10;
            //Add degree to value
            String v = Function.get1Digit(av) + "\u00B0";
            roomWidget.setText(v);
            //Set color
            int level = Function.getLevel(18.0f, 30.0f, 3, av);
            int color[] = context.getResources().getIntArray(R.array.roomTemp);
            if (level<=color.length && level>=0){
                roomWidget.setButtonColor(color[level]);
            }
        }


    }

    public void clearAll(){
        int end = items.size();
        items.clear();
        //Can't clear item without updating adapter!!!!!
        //If not clear, it will bug!!!!. 09/06/2019
        notifyItemRangeRemoved(0, end);
    }

    public int getSize(){
        return items.size();
    }

    public AirItem getAirItem(int p){
        return items.get(p);
    }

    public List<AirItem> getItems() {
        return items;
    }

    public void addAirItems(List<AirItem> airItems){
        items.addAll(0, airItems);
        //Insert from top item
        notifyItemRangeInserted(0, airItems.size());
    }

    public void addAirItems(AirItem airItem){
        items.add(0, airItem);
        notifyItemInserted(0);
    }

    public void setItems(int p, AirItem airItem){
        if (p<items.size()){
            items.set(p, airItem);
            notifyDataSetChanged();
        }
    }

    public void deleteItemID(int deviceId){
        int p = getPositionOf(deviceId);
        if (p>-1){
            items.remove(p);
            notifyItemRemoved(p);
        }
    }

    public int getPositionOf(int deviceId){
        for (int i=0; i<items.size(); i++){
            int id = items.get(i).getDeviceId();
            if (id==deviceId){
                return i;
            }
        }
        return -1;  //Not found
    }

    public Context getContext() {
        return context;
    }

    public int getOnline(){
        int online = 0;
        for (AirItem item : items){
            if (item.isOnline()){ online++; }
        }
        return online;
    }

    public void setAirItemClickListener(OnAirItemClickListener airItemClickListener) {
        this.airItemClickListener = airItemClickListener;
    }

    interface OnAirItemClickListener{
        void onClick(View view, int position, AirItem airItem);
    }


}
