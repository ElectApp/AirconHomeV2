package com.apyeng.airconhomev2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AboutItemAdapter extends RecyclerView.Adapter<AboutItemAdapter.ViewHolder>{

    private Context context;
    private List<AboutItem> items;
    private OnAboutItemClickListener clickListener;

    public AboutItemAdapter(Context context, List<AboutItem> items){
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public AboutItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.about_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AboutItemAdapter.ViewHolder holder, int i) {
        //Get data
        AboutItem item = items.get(i);
        //Set view
        //Title
        holder.titleTv.setText(item.getTitle());
        //Detail
        String d = item.getDetail();
        if (d!=null){
            holder.detailTv.setText(d);
            holder.detailTv.setVisibility(View.VISIBLE);
            holder.nextImg.setVisibility(View.GONE);
        }else {
            holder.detailTv.setVisibility(View.GONE);
            holder.nextImg.setVisibility(View.VISIBLE);
        }
        //Divider
        if (item.isGroupDivider()){
            holder.groupDividerV.setVisibility(View.VISIBLE);
            holder.childDividerV.setVisibility(View.GONE);
        }else {
            holder.groupDividerV.setVisibility(View.GONE);
            holder.childDividerV.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView titleTv, detailTv;
        private ImageView nextImg;
        private View childDividerV, groupDividerV;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTv = itemView.findViewById(R.id.tv_title);
            detailTv = itemView.findViewById(R.id.tv_detail);
            nextImg = itemView.findViewById(R.id.img_next);
            childDividerV = itemView.findViewById(R.id.child_divider);
            groupDividerV = itemView.findViewById(R.id.group_divider);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (clickListener!=null){
                        int i = getAdapterPosition();
                        AboutItem item = items.get(i);
                        clickListener.onClick(view, i, item);
                    }
                }
            });

        }



    }

    public void setAboutItemClickListener(OnAboutItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    interface OnAboutItemClickListener{
        void onClick(View view, int position, AboutItem item);
    }


}
