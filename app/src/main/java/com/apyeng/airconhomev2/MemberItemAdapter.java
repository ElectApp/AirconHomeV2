package com.apyeng.airconhomev2;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemberItemAdapter extends RecyclerView.Adapter<MemberItemAdapter.ViewHolder>{

    private AsyncTaskImageLoader imageLoader;
    private List<MemberItem> items;
    private SparseArray<Bitmap> colorBitMap; //Use when no picture or download failed
    private Context context;
    private int lastIndex;
    private int rColor[];
    private OnClickItemListener clickItemListener;
    private static final String TAG = "MemberItemAdapter";

    public MemberItemAdapter(Context context, List<MemberItem> items){
        this.context = context;
        this.items = items;
        //Set color list resource
        //rColor = context.getResources().getIntArray(R.array.colorRandom);
        setColorList();
        //Initial color bitmap array
        colorBitMap = new SparseArray<>();
        //Create downloader object
        imageLoader = new AsyncTaskImageLoader(context);

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //View view = View.inflate(context, R.layout.member_item_layout, viewGroup);
        View view = LayoutInflater.from(context)
                .inflate(R.layout.member_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
        //Get data
        MemberItem item = items.get(i);
        //Set image
        String fileName = item.getImageName();
        if (fileName!=null && !fileName.isEmpty()){
            Bitmap img = item.getImageDownloaded();
            if (img!=null){
                holder.icon.setImageBitmap(img);
            }else {
                holder.loadImage(i, fileName);
            }
        }else {
            //Random color
            holder.setIconRandomColor(i);
        }
        //Set name
        holder.username.setText(item.getUsername());
        //Set time
        holder.setTime(item.getActiveTime());
        //holder.time.setText(item.getActiveTime());

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void setColorList(){
        TypedArray ta = context.getResources().obtainTypedArray(R.array.colorRandom);
        rColor = new int[ta.length()];
        for (int i = 0; i < ta.length(); i++) {
            rColor[i] = ta.getColor(i, 0);
        }
        ta.recycle();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        private ImageView icon;
        private TextView username;
        private TextView time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.member_icon);
            username = itemView.findViewById(R.id.member_name);
            time = itemView.findViewById(R.id.registered_time);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (clickItemListener!=null){
                        MemberItem item = items.get(getAdapterPosition());
                        clickItemListener.onClick(view, item);
                    }
                }
            });

        }

        private void loadImage(int p, String fileName){
            //Download
            imageLoader.load(p, Function.getUserImageUrl(fileName),
                    new AsyncTaskImageLoader.OnDownloadListener() {
                @Override
                public void onSuccess(LoaderItem item) {
                    //Update list
                    int p = item.getId();
                    MemberItem previously = items.get(p);
                    previously.setImageDownloaded(item.getBitmap());
                    //Update adapter
                    notifyItemChanged(p);
                }

                @Override
                public void onFailed(LoaderItem item, String error) {
                    //Update list
                    int p = item.getId();
                    MemberItem previously = items.get(p);
                    previously.setImageName(null);
                    //Update adapter
                    notifyItemChanged(p);
                }
            });
        }

        private void setIconRandomColor(int p){
            //Get olo color bitmap
            Bitmap bm = colorBitMap.get(p, null);
            //Check before add bitmap to old array list
            if (bm==null){
                //Get next color
                lastIndex++; //Count index
                if (lastIndex>=rColor.length){
                    lastIndex = 0;
                }
                //Save color
                int oc = rColor[lastIndex];
                bm = Function.createImage(100, 100, oc);
                colorBitMap.put(p, bm);
            }
            //Set
            icon.setImageBitmap(bm);
            //Can't use
            //icon.setImageResource(oc);
        }


        private void setTime(String dateTime){
            //Input: YYYY-DD-MM HH:mm:ss
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            try {
                //Today
                Calendar calendar0 = Calendar.getInstance();
                int tYear = calendar0.get(Calendar.YEAR);
                int tM = calendar0.get(Calendar.MONTH);
                int tD = calendar0.get(Calendar.DAY_OF_MONTH);
                //Compare date
                Date date = format.parse(dateTime);
                Calendar calendar1 = dateToCalendar(date);
                int cYear = calendar1.get(Calendar.YEAR);
                //Raw data
                String raw[] = dateTime.split(" ");
                String d[] = raw[0].split("-");
                String t[] = raw[1].split(":");
                if (cYear==tYear){
                    int cM = calendar1.get(Calendar.MONTH);
                    int cD = calendar1.get(Calendar.DAY_OF_MONTH);
                    String txtDay = d[2]+"/"+d[1];
                    String txtTime = t[0]+":"+t[1];
                    if(cM==tM){
                        switch (tD - cD){
                            case 0: //Today
                                txtDay = context.getString(R.string.today);
                                break;
                            case 1: //Yesterday
                                txtDay = context.getString(R.string.yesterday);
                                break;
                        }
                    }
                    String txt = txtDay+", "+txtTime;
                    time.setText(txt);
                }else {
                    //Show full detail
                    SimpleDateFormat format2 = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.US);
                    String txt = format2.format(date);
                    time.setText(txt);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                //Set from dateTime
                time.setText(dateTime);
            }

        }


        //Convert Date to Calendar
        private Calendar dateToCalendar(Date date) {

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;

        }

        //Convert Calendar to Date
        private Date calendarToDate(Calendar calendar) {
            return calendar.getTime();
        }

    }

    public Context getContext() {
        return context;
    }

    public void addList(List<MemberItem> addItemList){
        int p = items.size();
        items.addAll(addItemList);
        notifyItemRangeInserted(p, addItemList.size());
    }

    public void setClickItemListener(OnClickItemListener clickItemListener) {
        this.clickItemListener = clickItemListener;
    }

    interface OnClickItemListener{
        void onClick(View view, MemberItem memberItem);
    }

}
