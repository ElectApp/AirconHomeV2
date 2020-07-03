package com.apyeng.airconhomev2.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.apyeng.airconhomev2.Constant;
import com.apyeng.airconhomev2.Function;
import com.apyeng.airconhomev2.LockableNestedScrollView;
import com.apyeng.airconhomev2.MyValueFormatter;
import com.apyeng.airconhomev2.R;
import com.apyeng.airconhomev2.models.GraphLogItem;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;

public class GraphLogItemAdapter extends RecyclerView.Adapter<GraphLogItemAdapter.ViewHolder>{

    private Context context;
    private List<GraphLogItem> items;
    private OnItemClickListener onItemClickListener = null;

    public GraphLogItemAdapter(Context context, List<GraphLogItem> items){
        this.context = context;
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.graph_log_item_layout, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        //Get data
        GraphLogItem logItem = items.get(i);
        //Set data
        viewHolder.setIconImg(logItem.checked? R.drawable.check_icon:logItem.iconId);
        viewHolder.titleTv.setText(logItem.title);
        viewHolder.detailsTv.setText(logItem.details);
        viewHolder.setChart(logItem.title, logItem.unit, logItem.graphLabel, logItem.graphData, logItem.min);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView iconImg;
        private TextView titleTv, detailsTv;
        private LineChart chart;
        private boolean isShowValue, isScaleX, isScaleY;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            iconImg = itemView.findViewById(R.id.log_img);
            titleTv = itemView.findViewById(R.id.log_title);
            detailsTv = itemView.findViewById(R.id.log_details);
            chart = itemView.findViewById(R.id.log_chart);

            if (onItemClickListener!=null){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int p = getAdapterPosition();
                        GraphLogItem item = items.get(p);
                        onItemClickListener.onClick(view, p, item.title, item.checked);
                    }
                });
            }
        }

        private void setIconImg(int id){
            //Icon
            iconImg.setImageResource(id);
            //Chart Visibility
            chart.setVisibility(id==R.drawable.check_icon? View.VISIBLE:View.GONE);
        }

        private void showChartDrawValue(boolean show){
            //Check flag
            if (isShowValue!=show){
                List<ILineDataSet> sets = chart.getData()
                        .getDataSets();
                for (ILineDataSet iSet : sets) {
                    LineDataSet set = (LineDataSet) iSet;
                    set.setDrawValues(show);
                }
                //Re-draw
                chart.invalidate();
            }
            //Save state
            isShowValue = show;
        }

        private void setChart(String chartTitle, String unit, final ArrayList<String> graphLabel,
                              final ArrayList<Entry> graphData, final float min){
            if (chartTitle==null || unit==null || graphLabel==null || graphData==null){ return; }
            //=================== Initial ====================//
            //Set property
            chart.setDrawGridBackground(false);
            Description description = chart.getDescription();
            description.setEnabled(false);
            chart.setDrawBorders(false);
            //Y left Axis
            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setEnabled(true);
            leftAxis.setDrawAxisLine(true);
            leftAxis.setDrawGridLines(true);
            leftAxis.enableGridDashedLine(10f, 10f, 10f);
            leftAxis.setValueFormatter(new MyValueFormatter(" "+unit, 0));
            leftAxis.setAxisMinimum(min<0f? min:0f); //Start point
            //Y right Axis
            YAxis rightAxis = chart.getAxisRight();
            rightAxis.setEnabled(false); //Not use
            //X Axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);
            xAxis.setPosition(XAxis.XAxisPosition.TOP);
            xAxis.enableGridDashedLine(10f, 10f, 10f);
            // enable touch gestures
            chart.setTouchEnabled(true);
            // enable scaling and dragging
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            // if disabled, scaling can be done on x- and y-axis separately
            chart.setPinchZoom(false);
            //Limit zoom max
            ViewPortHandler portHandler = chart.getViewPortHandler();
            portHandler.setMaximumScaleX(17.0f);
            portHandler.setMaximumScaleY(17.0f);
            //Auto scale
            chart.setAutoScaleMinMaxEnabled(true);
            //Add listener when scale or translate
            chart.setOnChartGestureListener(new OnChartGestureListener() {
                @Override
                public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                    //Lock scroll when zoom chart
                    //nestedScrollView.setScrollable(false);
                }

                @Override
                public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

                    //Get scale
                    ViewPortHandler port = chart.getViewPortHandler();
                    //This value is actual more than scaleX, Y at onChartScale()
                    float scaleX = port.getScaleX();
                    float scaleY = port.getScaleY();

                    Log.w("MyChart", "Actual Scale...X: "+scaleX+", Y: "+scaleY);
                    Log.w("MyChart", "Is scale...X: "+isScaleX+", Y: "+isScaleY);

                    if(isScaleX){
                        if (scaleX>10.0f){
                            showChartDrawValue(true);
                        }else if (scaleX<8.0f && scaleY<8.0f){
                            showChartDrawValue(false);
                        }
                    }else if (isScaleY){
                        if (scaleY>10.0f){
                            showChartDrawValue(true);
                        }else if (scaleX<8.0f && scaleY<8.0f){
                            showChartDrawValue(false);
                        }
                    }

                    //Lock scroll when scaleY active
                    //unlockScrollView(scaleY<1.2f);
                }

                @Override
                public void onChartLongPressed(MotionEvent me) {

                }

                @Override
                public void onChartDoubleTapped(MotionEvent me) {

                }

                @Override
                public void onChartSingleTapped(MotionEvent me) {

                }

                @Override
                public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

                }

                @Override
                public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                    Log.w("MyChart", "Scale...X: "+scaleX+", Y: "+scaleY);
                    //Save state
                    isScaleX = Math.abs(scaleX-1.0f)>0;
                    isScaleY = Math.abs(scaleY-1.0f)>0;

                }

                @Override
                public void onChartTranslate(MotionEvent me, float dX, float dY) {
                    //Log.w("MyChart", "Translate...X: "+dX+", Y: "+dY);
                }
            });
            // modify the legend ...
            Legend l = chart.getLegend();
            l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            l.setDrawInside(false);
            l.setForm(Legend.LegendForm.SQUARE);

            //====================== Show ========================//
            //Clear
            chart.resetTracking();
            //Set data
            LineDataSet d = new LineDataSet(graphData, chartTitle);
            //Set line style
            int c = Function.getRandomColor();
            d.setLineWidth(2.0f);
            d.setMode(LineDataSet.Mode.LINEAR); //Linear line
            d.setDrawCircles(false); //Not show dot circle
            d.setDrawFilled(true);   //Fill color
            d.setFillColor(c);
            d.setColor(c);
            d.setDrawValues(false); //Not show value on line
            //Add line
            LineData data = new LineData(d);
            chart.setData(data);
            //Set X label
            chart.getXAxis().setValueFormatter(new ValueFormatter() {

                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    //return super.getAxisLabel(value, axis);
                    //Thank: https://github.com/PhilJay/MPAndroidChart/issues/133
                    //Modify by Somsak Elect 25/06/2019
                    int n = (int)value%graphData.size();
                    float decimal = value-n;
                    Log.w("MyLabel", "X: "+value+", decimal: "+decimal+", N: "+n);
                    if (decimal>0.1f){
                        return ""; //Not show label
                    }else {
                        return graphLabel.get(n); //Show label
                    }
                }

            });
            //Re-drawn
            chart.invalidate();
            //Reset zoom
            chart.fitScreen();
            //Show animation
            chart.animateX(1000);
            //Unlock scroll
            //unlockScrollView(true);
        }
    }

    public interface OnItemClickListener{
        void onClick(View view, int p, String title, boolean checked);
    }


}
