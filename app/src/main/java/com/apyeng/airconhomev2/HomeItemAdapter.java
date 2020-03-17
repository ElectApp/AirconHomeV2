package com.apyeng.airconhomev2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HomeItemAdapter extends RecyclerView.Adapter<HomeItemAdapter.ViewHolder> {

    private Context context;
    private List<HomeItem> items;
    private SparseIntArray oldColorArray;
    private OnClickHomeItemListener clickHomeItemListener, clickSettingListener;
    private int rColor[];
    private int lastIndex;
    private AsyncTaskImageLoader taskImageLoader;
    private static final String TAG = "HomeItemAdapter";


    public HomeItemAdapter(Context context, List<HomeItem> items){
        this.context = context;
        this.items = items;
        //Set color list resource
        rColor = context.getResources().getIntArray(R.array.colorRandom);
        //Initial old color array list
        oldColorArray = new SparseIntArray();
        //Create downloader object
        taskImageLoader = new AsyncTaskImageLoader(context);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.home_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeItemAdapter.ViewHolder holder, int i) {
        HomeItem item = items.get(i);
        holder.homeName.setText(item.getName());
        //Set detail
        int deviceId[] = item.getDeviceId();
        if (deviceId!=null && deviceId.length>0 && deviceId[0]!=0){
            String num = context.getString(R.string.installed)+" "+item.getDeviceId().length+" "
                    +context.getString(R.string.devices);
            holder.installed.setText(num);
            String sav = context.getString(R.string.saving)
                    +" "+Function.getPowerAndUnit(item.getSaving()/10.0f);
            holder.saving.setText(sav); //Show 1 decimal
            String con = context.getString(R.string.consumption)
                    +" "+Function.getPowerAndUnit(item.getConsumption()*10.0f);
            holder.consumption.setText(con);
        }else {
            holder.installed.setText(R.string.no_ac);
            holder.consumption.setText("");
            holder.saving.setText("");
        }
        //Set image
        String fileName = item.getProfileImg();
        if (fileName!=null){
            Bitmap img = item.getImageDownloaded();
            if (img!=null){
                Log.w(TAG, "User old file: "+i);
                holder.setLoadFrame(false);
                //Set from image download finish
                holder.homeImg.setImageBitmap(img);
            }else {
                Log.w(TAG, "Waiting download: "+i);
                //Show loading
                holder.setLoadFrame(true);
                //Load
                holder.loadImage(i, fileName);
            }
        }else {
            Log.w(TAG, "No picture: "+i);
            //Hide loading frame
            holder.setLoadFrame(false);
            //Random color
            holder.setBackgroundRandomColor(i);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView homeName;
        private TextView installed;
        private TextView consumption;
        private TextView saving;
        private SelectableRoundedImageView homeImg;
        private ShimmerFrameLayout loadFrame;
        private ConstraintLayout contentLay;

        public ViewHolder(@NonNull final View v) {
            super(v);

            homeName = v.findViewById(R.id.home_name);
            installed = v.findViewById(R.id.num_devices);
            consumption = v.findViewById(R.id.power_consumption);
            saving = v.findViewById(R.id.power_save);
            homeImg = v.findViewById(R.id.home_img);
            loadFrame = v.findViewById(R.id.loading_frame);
            contentLay = v.findViewById(R.id.content);

            //Item click listener
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickHomeItemListener!=null){
                        int p = getAdapterPosition();
                        clickHomeItemListener.onClick(view, p, items.get(p));
                    }
                }
            });

            //Home setting listener
            v.findViewById(R.id.setting_icon).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickSettingListener!=null){
                        int p = getAdapterPosition();
                        clickSettingListener.onClick(view, p, items.get(p));
                    }
                }
            });


        }

        private void setBackgroundRandomColor(int p){
            //Get old color
            int oc = oldColorArray.get(p, 0);
            //Check before add color to old array list
            if (oc==0){
                lastIndex++; //Count index
                if (lastIndex>=rColor.length){
                    lastIndex = 0;
                }
                //Save color
                oc = rColor[lastIndex];
                oldColorArray.put(p, oc);
            }

            //Clear
            homeImg.setImageBitmap(null);
            //Set color
            homeImg.setBackgroundColor(oc);
            Log.w(TAG, "Set random color to "+p+" by "+oc);
        }

        private void loadImage(int id, String fileName){
            //Set url to download
            String url = Constant.DOWNLOAD_URL+"?path="+Constant.GROUP_IMG_DIR+"/"+fileName+Constant.JPEG;
            //Start download
            taskImageLoader.load(id, url, new AsyncTaskImageLoader.OnDownloadListener() {
                @Override
                public void onSuccess(LoaderItem item) {
                    //Update list
                    HomeItem previously = items.get(item.getId());
                    previously.setImageDownloaded(item.getBitmap());
                    items.set(item.getId(), previously);
                    //Update adapter
                    notifyDataSetChanged();
                }

                @Override
                public void onFailed(LoaderItem item, String error) {
                    //Update list
                    HomeItem previously = items.get(item.getId());
                    previously.setProfileImg(null);
                    items.set(item.getId(), previously);
                    //Update adapter
                    notifyDataSetChanged();
                }
            });
        }


        private void setLoadFrame(boolean show){
            if (show){
                contentLay.setVisibility(View.GONE);
                loadFrame.setVisibility(View.VISIBLE);
                loadFrame.startShimmerAnimation();
            }else {
                loadFrame.setVisibility(View.GONE);
                loadFrame.stopShimmerAnimation();
                contentLay.setVisibility(View.VISIBLE);
            }
        }

    }

    public int getPositionOf(int groupId){
        for (int i=0; i<items.size(); i++){
            int id = items.get(i).getGroupId();
            if (id==groupId){
                return i;
            }
        }
        return -1;  //Not found
    }

    public int getPositionOf(int groupId, int consumptionId){
        for (int i=0; i<items.size(); i++){
            int g = items.get(i).getGroupId();
            if (g==groupId){
                for (int d : items.get(i).getDeviceId()){
                    if (d==consumptionId){
                        return i;
                    }
                }
            }
        }
        return -1;  //Not found
    }

    public List<HomeItem> getItems(){
        return items;
    }

    public void clearList(){
        items.clear();
        notifyDataSetChanged();
    }

    //Recommend add each 5 items -> download fast
    public void addList(@NonNull List<HomeItem> list, boolean insertTop){
        //Check flag
        if (insertTop){
            for (HomeItem item : list){
                items.add(0, item);
                notifyItemInserted(0);
            }
        }else {
            int a = items.size();
            items.addAll(list);
            notifyItemRangeInserted(a, list.size());
        }

    }

    public void add(@NonNull HomeItem homeItem){
        int p = getPositionOf(homeItem.getGroupId());
        if (p==-1){
            items.add(0, homeItem);
            notifyItemInserted(0);
        }
    }

    public void deleteItem(int p){
        if (p<items.size()){
            items.remove(p);
            notifyItemRemoved(p);
        }
    }

    public void setItems(int p, HomeItem homeItem){
        if (p<items.size()){
            items.set(p, homeItem);
            //notifyItemChanged(p);      //Update by adapter blink
            notifyDataSetChanged();     //Update by adapter not blink
        }
    }

    public void setClickItemListener(OnClickHomeItemListener homeItemClickListener) {
        this.clickHomeItemListener = homeItemClickListener;
    }

    public void setClickSettingListener(OnClickHomeItemListener homeItemClickListener){
        this.clickSettingListener = homeItemClickListener;
    }

    interface OnClickHomeItemListener{
        void onClick(View view, int position, HomeItem airItem);
    }



}
