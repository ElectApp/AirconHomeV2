package com.apyeng.airconhomev2;



import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;

import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.applikeysolutions.cosmocalendar.dialog.CalendarDialog;
import com.applikeysolutions.cosmocalendar.dialog.OnDaysSelectionListener;
import com.applikeysolutions.cosmocalendar.model.Day;
import com.github.lzyzsd.randomcolor.RandomColor;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.StackedValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.whiteelephant.monthpicker.MonthPickerDialog;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class LoggingFragment extends Fragment {

    //About home
    private int groupId;
    private Date registeredDate;
    private List<AirItem> airItemList;
    private MqttAndroidClient mqtt;
    private SparseIntArray deviceArray;
    private int countSubscribed;
    //Real time
    private TextView consumptionTxt, savingTxt;
    private SparseIntArray acSumPower, pvSumPower;
    private static final String DEVICE_KEY[] = new String[]{ Constant.AC, Constant.PV };
    //History
    private int cTab, cYear, yDecimal;
    private String currentMonth, yUnit;
    private Calendar today;
    private TextView timeSelectedTxt;
    private BarChart chart;
    private List<ExpandItem> summaryItem;
    private ExpandableListViewAdapter adapter;
    private View chartView[];
    private HomeManager homeManager;
    private MonthPickerDialog.Builder monthPicker, yearPicker;
    private SimpleDateFormat monthFormat1 = new SimpleDateFormat("MMMM, yyyy", Locale.US);
    private int chartColors[]; //Index match with deviceArray
    private static final int LOAD_GRAPH = 0, SHOW_GRAPH = 1, FAILED_GRAPH = 2;
    //Other
    private boolean disconnectFlag;
    private TextView faultTxt;
    private LockableNestedScrollView scrollView;
    private View sectionView[];
    private Context context;
    private Activity activity;
    public static final String CHART_TAG = "MyChart";
    public static final String TAG = "LoggingFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get data
        groupId = getArguments().getInt(Constant.GROUP_ID);
        String register = getArguments().getString(Constant.REGISTERED);

        Log.w(CHART_TAG, "Registered time: "+register);

        try {
            registeredDate = Constant.REGISTER_FORMAT.parse(register);
        } catch (ParseException e) {
            e.printStackTrace();
            registeredDate = null;
        }

        //Initial MQTT
        final String id = MqttClient.generateClientId();
        mqtt = new MqttAndroidClient(context, Constant.MQTT_URL, id);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "MQTT reconnect..."+reconnect);
                if (reconnect){
                    //Hide
                    faultTxt.setVisibility(View.GONE);
                    //Subscribe
                    subscribeGroup();
                    subscribeDevice();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Show no internet bar
                //Show no internet
                if (!disconnectFlag){ faultTxt.setVisibility(View.VISIBLE); }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Update power status
                if (message!=null){
                    setAirItem(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //Set view
        View view = inflater.inflate(R.layout.fragment_logging, container, false);

        scrollView = view.findViewById(R.id.contain_sv);
        faultTxt = view.findViewById(R.id.no_internet_txt);

        savingTxt = view.findViewById(R.id.saving_value);
        consumptionTxt = view.findViewById(R.id.consumption_value);

        TabLayout tabLayout = view.findViewById(R.id.filter_tab);
        timeSelectedTxt = view.findViewById(R.id.time_selected_txt);
        chart = view.findViewById(R.id.bar_chart);
        NonScrollExpandableListView logExpLV = view.findViewById(R.id.expanded_menu);
        //Chart section
        chartView = new View[3];
        chartView[0] = view.findViewById(R.id.chart_loading);
        chartView[1] = view.findViewById(R.id.chart_result_lay);
        chartView[2] = view.findViewById(R.id.graph_failed_txt);

        //Parent Section
        sectionView = new View[2];
        sectionView[0] = scrollView;
        sectionView[1] = view.findViewById(R.id.circle_progress);

        //================ Time Select Dialog =========//
        //Get register time
        Calendar register = Calendar.getInstance();
        register.setTime(registeredDate!=null? registeredDate : new Date());
        int rMonth = 0; //register.get(Calendar.MONTH);
        int rYear = register.get(Calendar.YEAR);
        //Current time
        today = Calendar.getInstance();
        int cMonth = 11; //today.get(Calendar.MONTH);
        cYear = today.get(Calendar.YEAR);
        currentMonth = Constant.MONTH_FORMAT.format(today.getTime());
        //Month Picker Dialog
        monthPicker = new MonthPickerDialog.Builder(context, new MonthPickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(int selectedMonth, int selectedYear) {
                Log.w(CHART_TAG, "Select Month: "+selectedMonth+", Year: "+selectedYear);
                //Convert to Calendar
                Calendar cal = Calendar.getInstance();
                cal.set(selectedYear, selectedMonth, 1);
                //Set X axis Max
                //chart.getXAxis().setAxisMaximum(cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                //Read log data
                readMonthLogData(cal);
            }
        }, cYear, cMonth);
        //Limit rang from register time
        monthPicker.setMonthAndYearRange(rMonth, cMonth, rYear, cYear);
        //Year Picker Dialog
        yearPicker = new MonthPickerDialog.Builder(context, new MonthPickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(int selectedMonth, int selectedYear) {
                Log.w(CHART_TAG, "Select Year: "+selectedYear);
                //Read log data
                readYearLogData(selectedYear);
            }
        }, cYear, cMonth);
        //Limit year rang and show only year
        yearPicker.setYearRange(rYear, cYear).showYearOnly();
        //Pick button
        view.findViewById(R.id.time_selected_lay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                switch (cTab){
                    case 0: //Show month dialog picker
                        monthPicker.build().show();
                        break;
                    case 1: //Show year dialog picker
                        yearPicker.build().show();
                        break;
                }

            }
        });

        //=============== Initial Tap layout ===================//
        String []title = getStringArray(R.array.log_tab_title);
        //Set to tab
        for (String t : title){
            tabLayout.addTab(tabLayout.newTab().setText(t.toUpperCase()));
        }
        //Add listener
        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.w(TAG, "Select Tab..."+tab.getText());
                //Save tab
                cTab = tab.getPosition();
                //Read
                checkCurrentTabToReadLogData();

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        //==================== Initial Chart ========================//
        //Set property
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);
        chart.setHighlightFullBarEnabled(false);
        //If more than 5 entries are displayed in the chart, no values will be draw
        chart.setMaxVisibleValueCount(5);
        //Scaling can now only be done on x- and y-axis separately
        chart.setPinchZoom(false);
        //Y left Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //Not show right axis
        chart.getAxisRight().setEnabled(false);
        //X Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day
        xAxis.setLabelCount(7);
        //Modify the legend ...
        Legend l = chart.getLegend();
        l.setEnabled(false); //Not show Legend
        /*
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setFormSize(8f);
        l.setFormToTextSpace(4f);
        l.setXEntrySpace(6f);
        */

        chart.setTouchEnabled(true);
        chart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                Log.w(CHART_TAG, "Start touch...");
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                Log.w(CHART_TAG, "End touch...");

                ViewPortHandler port = chart.getViewPortHandler();
                float y = port.getScaleY();
                //Lock scroll when scaleY active
                scrollView.setScrollable(y<1.2f);
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

                Log.w(CHART_TAG, "ScaleY..."+scaleY);

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

                Log.w(CHART_TAG, "Translate...Y: "+dY);

            }
        });

        //==================== Initial Expand Adapter ====================//
        summaryItem = new ArrayList<>();
        //Set adapter
        adapter = new ExpandableListViewAdapter(context, summaryItem);
        logExpLV.setAdapter(adapter);
        //Add click group listener
        logExpLV.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int i) {
                Log.w(TAG, "Group expand no."+i);
            }
        });

        //Initial
        scrollView.setScrollable(true); //Unlock scroll
        showContent(false); //hide content
        homeManager = new HomeManager(context);
        airItemList = new ArrayList<>();
        deviceArray = new SparseIntArray();
        chartColors = new int[0];
        acSumPower = new SparseIntArray();
        pvSumPower = new SparseIntArray();

        //Add touch
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                // MotionEvent object holds X-Y values
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    //String text = "You click at x = " + event.getX() + " and y = " + event.getY();
                    //Function.showToast(context, text);
                    //Enable scroll when user touch out of graph area
                    scrollView.setScrollable(true);
                }
                return false;
            }
        });

        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Read device detail
        tryReadDeviceDetail();
    }




    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Should save context at here, due to it first work before onCreated()
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //Should save activity at here, due to it first work before onCreated()
        this.activity = activity;
    }

    @Override
    public void onDestroyView() {
        // Should disconnect MQTT on here due to after onDetach() work,
        // the context from onAttach() is auto set to null.
        // So, MQTT may bug!
        if (mqtt.isConnected()){ disconnectMQTTServer(); }
        // DestroyView
        super.onDestroyView();
    }

    private String[] getStringArray(int resId) {
        return context.getResources().getStringArray(resId);
    }

    private void setAirItem(@NonNull String topic, @NonNull MqttMessage message){
        Log.w(TAG, "New Message ["+topic+"] "+message.toString());
        //Check topic
        String s[] = topic.split("/");
        String v[] = message.toString().split(",");
        int group = Function.getInt(s[0].trim());
        if (group!=groupId){
            return;
        }
        Log.w(TAG, "Update...");
        //Update device topic: <group-id>/<device-id>/<key>
        if (s.length==3){
            //Check id match
            int deviceId = Function.getInt(s[1]);
            int p = deviceArray.indexOfValue(deviceId);
            if (p>-1 && v.length>=3){
                int value = Function.getInt(v[2].trim());
                switch (s[2]){
                    case Constant.AC: //Update AC power
                        setConsumptionTxt(deviceId, value);
                        break;
                    case Constant.PV: //Update PV power
                        setSavingTxt(deviceId, value);
                        break;
                }
            }
        }
        //Update number of list
        if (s.length==2){
            if (v.length<1){ return; }
            int deviceId = Function.getInt(v[0]);
            switch (s[1]){
                case Constant.DEVICE_CHANGED:
                    if (v.length>=2){
                        //Must recheck algorithm at updateDeviceArrayAndChartColors()
                        int p = deviceArray.indexOfValue(deviceId);
                        if (p>-1 && p<airItemList.size()){
                            Log.w(TAG, "Update item ID: "+deviceId);
                            airItemList.get(p).setNickname(v[1]);
                        }
                    }
                    break;
                case Constant.DEVICE_ADDED:
                    if (v.length>=3){
                        int p = deviceArray.indexOfValue(deviceId);
                        if (p<0){
                            Log.w(TAG, "Add item ID: "+deviceId);
                            airItemList.add(new AirItem(deviceId, v[1], v[2], null));
                            //Subscribe
                            subscribeDevice(deviceId, true);
                            //Update deviceArray
                            updateDeviceArrayAndChartColors();
                        }
                    }
                    break;
                case Constant.DEVICE_DELETED:
                    int p = deviceArray.indexOfValue(deviceId);
                    if (p>-1 && p<airItemList.size()){
                        Log.w(TAG, "Delete item ID: "+deviceId);
                        //Delete from list
                        airItemList.remove(p);
                        //Unsubscribe
                        subscribeDevice(deviceId, false);
                        //Update device array
                        updateDeviceArrayAndChartColors();
                    }

                    break;
            }
        }

    }

    private void setConsumptionTxt(int deviceId, int modbusValue){
        //Update array
        acSumPower.put(deviceId, modbusValue);
        //Update value
        String txt = Function.getPowerAndUnit(getSumOf(acSumPower)*10.0f);
        consumptionTxt.setText(txt);
    }

    private void setSavingTxt(int deviceId, int modbusValue){
        //Update array
        pvSumPower.put(deviceId, modbusValue);
        //Update value
        String txt = Function.getPowerAndUnit(getSumOf(pvSumPower)/10.0f);
        savingTxt.setText(txt);
    }


    private int getSumOf(SparseIntArray array){
        int v = 0;
        for (AirItem item : airItemList){
            v += array.get(item.getDeviceId(), 0);
        }
        return v;
    }

    private void tryReadDeviceDetail(){
        //Check internet connected
        if (Function.internetConnected(context)){
            homeManager.readFullDeviceDetail(groupId, new HomeManager.OnReadFullDeviceDetailListener() {
                @Override
                public void onSuccess(List<AirItem> airItems) {
                    Log.w(TAG, "OnSuccess...");
                    //Save list
                    if (airItems!=null && airItems.size()>0){
                        airItemList.clear();
                        airItemList.addAll(airItems);
                    }
                    //Update device array
                    updateDeviceArrayAndChartColors();
                    //Connect MQTT
                    connectMQTTServer();
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showNoResultDialog(activity, errorListener);
                }
            });
        }else{
            Function.showNoInternetDialog(activity, errorListener);
        }
    }

    private void connectMQTTServer(){
        disconnectFlag = false;
        //Set option
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(Constant.MQTT_USERNAME);
        options.setPassword(Constant.MQTT_PASSWORD.toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        try {
            Log.w(TAG, "Try to connect MQTT server...");
            IMqttToken token = mqtt.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Connect success");
                    //Hide dialog
                    Function.dismissAppErrorDialog(activity);
                    //Show content
                    showContent(true);
                    //Read chart
                    if (airItemList.size()>0){
                        checkCurrentTabToReadLogData();
                        //Subscribe device
                        subscribeDevice();
                    }else {
                        showChart(FAILED_GRAPH);
                    }

                    //Subscribe group
                    subscribeGroup();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed...");
                    Function.showNoResultDialog(activity, errorListener);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Log.e(TAG, "Connect error..."+e.getMessage());
        }

    }

    private void disconnectMQTTServer(){
        disconnectFlag = true;
        Log.w(TAG, "Try disconnect MQTT server...");
        try {
            IMqttToken token = mqtt.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Disconnect success...");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Disconnect failed...");
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void subscribeDevice(){
        //Subscribe
        for (AirItem item : airItemList){
            subscribeDevice(item.getDeviceId(), true);
        }
    }

    private void subscribeDevice(int deviceId, final boolean subscribe){
        //Subscribe device: <group-id>/<device-id>/<key>
        String head = String.valueOf(groupId)+"/" + String.valueOf(deviceId) + "/";
        for (String key : DEVICE_KEY){
            String topic = head + key;
            setSubscribe(topic, subscribe);
        }

    }

    private void subscribeGroup(){
        //Subscribe group topic: <group-id>/<key>
        String head = String.valueOf(groupId)+"/";
        for (String g : Constant.GROUP_KEY){
            String topic = head + g;
            setSubscribe(topic, true);
        }
    }

    private void setSubscribe(String topic, final boolean subscribe){
        try {
            //Set
            IMqttToken token;
            if(subscribe){
                token = mqtt.subscribe(topic, 1);
            }else {
                token = mqtt.unsubscribe(topic);
            }
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (subscribe){
                        countSubscribed++;
                        Log.w(TAG, "Subscribe success..."+countSubscribed);
                    }else {
                        countSubscribed--;
                        Log.w(TAG, "Unsubscribe success..."+countSubscribed);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            switch (titleId){
                case R.string.no_result:
                case R.string.no_internet:
                    tryReadDeviceDetail();
                    break;
            }

        }
    };

    private void showContent(boolean show){
        int n = show? 0:1;
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(n==i? View.VISIBLE : View.GONE);
        }
    }


    private void setTimeSelectedTxt(String time){
        timeSelectedTxt.setText(time);
    }

    private void setTimeSelectedTxt(Date date){
        String d = monthFormat1.format(date);
        timeSelectedTxt.setText(d);
    }

    private void showChart(int n){
        for (int i=0; i<chartView.length; i++){
            chartView[i].setVisibility(i==n? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void checkCurrentTabToReadLogData(){
        //Check update chart
        switch (cTab){
            case 0: //Read monthly log data
                readMonthLogData(today);
                break;
            case 1: //Read yearly log data
                readYearLogData(cYear);
                break;
        }
    }

    private void readMonthLogData(Calendar calendar){
        //Show loading
        showChart(LOAD_GRAPH);
        //Save picker last selected
        monthPicker.setActivatedMonth(calendar.get(Calendar.MONTH))
                .setActivatedYear(calendar.get(Calendar.YEAR));
        //Update selected time
        Date date = calendar.getTime();
        setTimeSelectedTxt(date);
        //Get date
        String startDate = Function.getStartDateInMonth(calendar);
        String endDate = Function.getEndDateInMonth(calendar);
        //Try
        homeManager.readDateRangLogData(groupId, deviceArray, startDate, endDate, new HomeManager.OnLogDataListener() {
            @Override
            public void onSuccess(float sumEachId[], float total, float maxValueAtTime, List<ChartItem> chartItems) {
                int len = chartItems.size();
                Log.w(CHART_TAG, "Read daily log success..."+len+" maxY: "+maxValueAtTime);
                if (len>0){
                    //Set x Axis
                    chart.getXAxis().setValueFormatter(dayFormatter);
                    //Plot chart
                    plotMonthBarChart(chartItems, getDividerAndSetYAxis(maxValueAtTime));
                    //Update summary list
                    updateChartSummaryList(sumEachId, total);
                }else {
                    //No chart data
                    showChart(FAILED_GRAPH);
                }
            }

            @Override
            public void onFailed(String error) {
                Log.e(CHART_TAG, "Read daily log failed: "+error);
                showChart(FAILED_GRAPH);
            }
        });
    }

    private void readYearLogData(int year){
        String y = String.valueOf(year);
        //Show loading
        showChart(LOAD_GRAPH);
        //Update selected time
        setTimeSelectedTxt(y);
        //Try read by Month Keep format: yyyy-mm
        homeManager.readMonthRangLogData(groupId, deviceArray, y+"-01", currentMonth,
                new HomeManager.OnLogDataListener() {
            @Override
            public void onSuccess(float[] sumEachId, float total, float maxValueAtTime, List<ChartItem> chartItems) {
                int len = chartItems.size();
                Log.w(CHART_TAG, "Read month log success..."+len+" maxY: "+maxValueAtTime);
                if (len>0){
                    //Set x Axis
                    chart.getXAxis().setValueFormatter(monthFormatter);
                    //Plot chart
                    plotYearBarChart(chartItems, getDividerAndSetYAxis(maxValueAtTime));
                    //Update summary list
                    updateChartSummaryList(sumEachId, total);
                }else {
                    //No chart data
                    showChart(FAILED_GRAPH);
                }

            }

            @Override
            public void onFailed(String error) {
                Log.e(CHART_TAG, "Read month log failed: "+error);
                showChart(FAILED_GRAPH);
            }
        });

    }

    private float getDividerAndSetYAxis(float maxValue){
        //Default
        String t = " Wh"; yDecimal = 0; float divider = 1f;
        //Check value
        if (maxValue>1000f){
            divider = 1000.0f;
            yDecimal = 2;
            t = " kWh";
        }
        //Set Left Axis
        chart.getAxisLeft().setValueFormatter(new MyValueFormatter(t, yDecimal));
        //Return divider
        return divider;
    }

    private ValueFormatter monthFormatter = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            String month[] = context.getResources().getStringArray(R.array.short_month);
            int m = (int)(value-1.0f);
            if (m>=0 && m<month.length){
                return month[m];
            }
            return super.getFormattedValue(value);
        }
    };

    private ValueFormatter dayFormatter = new ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {

            int dayOfMonth = (int)value;

            String appendix = "th";

            switch (dayOfMonth) {
                case 1:
                    appendix = "st";
                    break;
                case 2:
                    appendix = "nd";
                    break;
                case 3:
                    appendix = "rd";
                    break;
                case 21:
                    appendix = "st";
                    break;
                case 22:
                    appendix = "nd";
                    break;
                case 23:
                    appendix = "rd";
                    break;
                case 31:
                    appendix = "st";
                    break;
            }

            return dayOfMonth == 0 ? "" : dayOfMonth + appendix;

        }
    };


    //Update this array when device changing in group
    private void updateDeviceArrayAndChartColors(){
        //Clear
        deviceArray.clear();
        //Update device array
        int i=0;
        for (AirItem item : airItemList){
            deviceArray.put(i, item.getDeviceId());
            i++;
        }
        //Update chart colors array
        //updateChartColors();
        setChartColors();
    }

    //Written by Somsak Elect, 28/06/2019
    //Due to found RandomColor library (https://github.com/lzyzsd/AndroidRandomColor)
    //Sometimes giving similar colors, So this function will random different color always time
    private void setChartColors(){
        //Clear old color
        chartColors = new int[deviceArray.size()];
        //Random
        //Thank equation: int random = new Random().nextInt((max - min) + 1) + min;
        //For set rang for random
        Random random = new Random();
        int ran[] = new int[3];
        int last[] = new int[3];
        int n = 0;
        //Initial
        ran[1] = 25; ran[2] = 100;
        for (int i=0; i<3; i++){
            //Check to out of loop
            if (n>=chartColors.length){ break; }
            //Random color //Keep different 100 up
            int color;
            ran[i] = random.nextInt(211)+25; //25-235: This is setting for brightness zone
            while (Math.abs(ran[i]-last[i])<100){ //This is key for getting so different color
                ran[i] = random.nextInt(211)+25; //25-235
                Log.w(CHART_TAG, "New random...");
            }
            last[i] = ran[i];
            switch (i){
                case 0:
                    color = Color.argb(255, ran[i], ran[1], ran[2]);
                    Log.w(CHART_TAG, "R: "+ran[i]+" G: "+ran[1]+" B: "+ran[2]);
                    break;
                case 1:
                    color = Color.argb(255, ran[0], ran[i], ran[2]);
                    Log.w(CHART_TAG, "R: "+ran[0]+" G: "+ran[i]+" B: "+ran[2]);
                    break;
                default:
                    color = Color.argb(255, ran[0], ran[1], ran[i]);
                    Log.w(CHART_TAG, "R: "+ran[0]+" G: "+ran[1]+" B: "+ran[i]);
            }

            //Set color to array
            if (n<chartColors.length){
                chartColors[n] = color;
                n++;
            }
        }

    }


    //Keep format: YYYY-MM-dd
    private int getDay(String date){
        String d[] = date.split("-");
        if (d.length==3){
            return Function.getInt(d[2]);
        }
        return 0;
    }

    //Keep format: YYYY-MM
    private int getMonth(String monthYear){
        String d[] = monthYear.split("-");
        if (d.length==2){
            return Function.getInt(d[1]);
        }
        return 0;
    }

    private float[] divideBy(float values[], float divider){
        for (int i=0; i<values.length; i++){
            values[i] /= divider;
        }
        return values;
    }

    private void plotMonthBarChart(List<ChartItem> chartItems, float divider){
        //Set data
        ArrayList<BarEntry> val = new ArrayList<>();
        for (ChartItem item : chartItems){
            int d = getDay(item.time); //Convert time to day
            if (d>0){
                BarEntry entry = new BarEntry(d, item.values);
                //Edit y Value
                if (divider!=1f){
                    entry.setVals(divideBy(item.values, divider));
                }
                val.add(entry); //Add value to list
            }
        }

        /*
        int count = 0;
        ChartItem init = chartItems.get(0);
        float values[] = init.values;
        int d = getDay(init.time);
        float zero[] = new float[values.length];
        Log.w(CHART_TAG, "End day: "+plotEndDay);
        //Set data if match day in chartItems
        for (int n=1; n<plotEndDay+1; n++){
            //Check day match
            if (n==d){
                Log.w(CHART_TAG, "Set value to day: "+n);
                val.add(new BarEntry(n, values)); //Set point
                count++; //Next
                if (count<chartItems.size()){
                    ChartItem next = chartItems.get(count);
                    values = next.values;
                    d = getDay(next.time); //Get compare next day
                }
            }else {
                Log.w(CHART_TAG, "Set Zero to day: "+n);
                val.add(new BarEntry(n, zero)); //Set point is zero
            }
        }
        */

        //Plot
        plotChart(val);

    }

    private void plotYearBarChart(List<ChartItem> chartItems, float divider){
        //Set data
        ArrayList<BarEntry> val = new ArrayList<>();
        for (ChartItem item : chartItems){
            int d = getMonth(item.time); //Convert time to day
            if (d>0){
                BarEntry entry = new BarEntry(d, item.values);
                //Edit y Value
                if (divider!=1f){
                    entry.setVals(divideBy(item.values, divider));
                }
                val.add(entry); //Add value to list
            }
        }
        //Plot
        plotChart(val);
    }

    private void plotChart(ArrayList<BarEntry> val){

        BarDataSet set1 = new BarDataSet(val, "");
        set1.setDrawIcons(false);
        //Bar color
        set1.setColors(chartColors); //Length of array must match Y array at BarEntry()
        //Prepare data set
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        //Prepare bar data
        YAxis yAxis = chart.getAxisLeft();
        int d = yAxis.mDecimals;
        Log.w(CHART_TAG, "Decimal: "+d);
        BarData data = new BarData(dataSets);//Demical is update from getDividerAndSetYAxis()
        data.setValueFormatter(new StackedValueFormatter(false, "", yDecimal));
        data.setValueTextColor(Color.WHITE);
        //Set data
        chart.setData(data);
        //Fit bar
        chart.setFitBars(true);
        //Re-drawn
        chart.invalidate();
        //Reset zoom
        chart.fitScreen();
        //Animation
        //chart.animateXY(1000, 1000);
        chart.animateY(1000);
        //Show
        showChart(SHOW_GRAPH);
        //Unlock scroll
        scrollView.setScrollable(false);

    }

    //Index on sumEachId[] must match with getDeviceArray
    private void updateChartSummaryList(float sumEachId[], float total){
        //Clear
        summaryItem.clear();
        //Set child item
        List<LogItem> childItems = new ArrayList<>();
        for (int i=0; i<airItemList.size(); i++){
            AirItem item = airItemList.get(i);
            //Check index match
            int p = deviceArray.indexOfValue(item.getDeviceId());
            if (p>-1 && p<chartColors.length && p<sumEachId.length){
                //Set child value
                childItems.add(new LogItem(item.getNickname(),
                        getEnergyValueAndUnit(sumEachId[p]), chartColors[p]));
            }
        }
        //Group item
        LogItem group = new LogItem(context.getString(R.string.total), getEnergyValueAndUnit(total));
        //Update list
        summaryItem.add(new ExpandItem(group, childItems));
        adapter.notifyDataSetChanged();
    }

    private String getEnergyValueAndUnit(float value){
        //Define format
        DecimalFormat format = new DecimalFormat("###,###,###,##0");
        String unit = " Wh";
        if (value>999.9f){
            format = new DecimalFormat("###,###,###,##0.00");
            unit = " kWh";
            value /= 1000.0f;
        }
        return format.format(value) + unit;
    }






}
