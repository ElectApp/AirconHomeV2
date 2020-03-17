package com.apyeng.airconhomev2;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    private View rootView;
    //Energy
    private TextView savingTxt, consumptionTxt;
    //Room
    private TextView roomTempTxt;
    //Remote
    private TextView timeOnOffTxt, setPointTxt;
    private ImageView powerIcon, modeIcon, fanIcon, quietIcon,
            louverIcon, sleepIcon, timerIcon, ecoIcon, turboIcon;
    //Top panel
    private TextView faultTxt;
    private ImageView warningIcon;
    //Animation
    private ObjectAnimator faultTxtAnim;
    //State
    private Indoor lastIndoor;
    //MQTT
    private MqttAndroidClient mqtt;
    private String deviceHead;
    private static final String KEY[] = { Constant.AC, Constant.PV, Constant.STATE, Constant.RESPONSE };
    //Chart
    private TextView dateTxt[];
    private LineChart chart;
    private View graphSectionView[];
    private int[] dateTxtColor, lineChartColor, areaChartColor;
    private boolean isShowValue, isScaleX, isScaleY;
    private static final int LOAD_GRAPH = 0, SHOW_GRAPH = 1, FAILED_GRAPH = 2;
    //Other
    private LockableNestedScrollView nestedScrollView;
    private AirItem airItem;
    private int groupId;
    private boolean disconnectFlag, isActive;
    private int setPointBuffer, holdPressId;
    private static final String TAG_BTN = "HoldPress";
    private static final String TAG = "ControlActivity";

    @SuppressLint("ClickableViewAccessibility") //Not alert about onTouchListener
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        //Get intent data
        airItem = getIntent().getParcelableExtra(Constant.AC_DATA);
        Indoor indoor = getIntent().getParcelableExtra(Constant.INDOOR_DATA);
        Time timerOn = getIntent().getParcelableExtra(Constant.TIME_ON);
        Time timerOff = getIntent().getParcelableExtra(Constant.TIME_OFF);
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);

        if (airItem==null || indoor==null || groupId==0){
            throw new IllegalArgumentException("Must pass AC, Indoor, groupId data...");
        }

        Log.w(TAG, "AC ID: "+airItem.getDeviceId());

        //Declare object in layout
        rootView = findViewById(R.id.root_view);

        nestedScrollView = findViewById(R.id.container);

        warningIcon = findViewById(R.id.warning_icon);
        faultTxt = findViewById(R.id.fault_txt);

        savingTxt = findViewById(R.id.saving_value);
        consumptionTxt = findViewById(R.id.consumption_value);

        roomTempTxt = findViewById(R.id.room_temp_txt);
        timeOnOffTxt = findViewById(R.id.time_on_off_txt);

        powerIcon = findViewById(R.id.power_icon);
        ImageView increaseIcon = findViewById(R.id.ic_increase);
        ImageView decreaseIcon = findViewById(R.id.ic_decrease);
        setPointTxt = findViewById(R.id.setpoint_value);
        modeIcon = findViewById(R.id.mode_icon);
        fanIcon = findViewById(R.id.speed_level_icon);
        quietIcon = findViewById(R.id.quiet_icon);
        louverIcon = findViewById(R.id.louver_icon);
        sleepIcon = findViewById(R.id.sleep_icon);
        timerIcon = findViewById(R.id.timer_icon);
        ecoIcon = findViewById(R.id.eco_icon);
        turboIcon = findViewById(R.id.turbo_icon);

        //Set head on topic
        String groupHead = String.valueOf(groupId)+"/";
        deviceHead = groupHead+String.valueOf(airItem.getDeviceId())+"/";

        //Initial unlock scroll
        nestedScrollView.setScrollable(true);

        //======= About Chart =========//
        //Date Text
        dateTxt = new TextView[2];
        dateTxt[0] = findViewById(R.id.today_chart_txt);
        dateTxt[1] = findViewById(R.id.yesterday_chart_txt);
        dateTxt[0].setOnClickListener(dateClickListener);
        dateTxt[1].setOnClickListener(dateClickListener);
        dateTxtColor = getResources().getIntArray(R.array.textTabColor); //Color
        //Chart
        chart = findViewById(R.id.line_chart);
        graphSectionView = new View[3];
        graphSectionView[0] = findViewById(R.id.chart_loading);
        graphSectionView[1] = chart;
        graphSectionView[2] = findViewById(R.id.graph_failed_txt);
        //Set property
        chart.setDrawGridBackground(false);
        Description description = chart.getDescription();
        description.setEnabled(false);
        chart.setDrawBorders(false);
        //Color
        lineChartColor = getResources().getIntArray(R.array.chartColor);
        areaChartColor = getResources().getIntArray(R.array.chartAreaColor);
        //Y left Axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(true);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setDrawGridLines(true);
        leftAxis.enableGridDashedLine(10f, 10f, 10f);
        leftAxis.setValueFormatter(new MyValueFormatter(" W", 0));
        leftAxis.setAxisMinimum(0f); //Start from 0
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
                nestedScrollView.setScrollable(scaleY<1.2f);
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
        //Initial show today
        loadDataToPlotGraph(Function.getDate(0));

        //============ About Control and Other ============//
        //Set toolbar title
        TextView nameTxt = findViewById(R.id.air_name);
        nameTxt.setText(airItem.getNickname());

        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Disconnect
                disconnectMQTTServer();
                finish();
            }
        });

        findViewById(R.id.more_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Block show toast error
                isActive = false;
                //Start AboutActivity
                Intent intent = new Intent(ControlActivity.this, AboutActivity.class);
                intent.putExtra(Constant.DEVICE_HEAD, deviceHead);
                startActivity(intent);
            }
        });

        warningIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Display detail
                showControllerFaultDialog(lastIndoor.tripCode);
            }
        });


        //Add click listener to icon on remote layout
        powerIcon.setOnClickListener(remoteClickListener);
        modeIcon.setOnClickListener(remoteClickListener);
        fanIcon.setOnClickListener(remoteClickListener);
        quietIcon.setOnClickListener(remoteClickListener);
        louverIcon.setOnClickListener(remoteClickListener);
        sleepIcon.setOnClickListener(remoteClickListener);
        timerIcon.setOnClickListener(remoteClickListener);
        ecoIcon.setOnClickListener(remoteClickListener);
        turboIcon.setOnClickListener(remoteClickListener);

        //Hold press listener
        increaseIcon.setOnLongClickListener(remoteLongClickListener);
        decreaseIcon.setOnLongClickListener(remoteLongClickListener);
        increaseIcon.setOnTouchListener(remoteTouchListener);
        decreaseIcon.setOnTouchListener(remoteTouchListener);

        //Animation
        setFaultTxtAnim();

        //Initial other view
        lastIndoor = indoor;
        lastIndoor.onTime = timerOn;
        lastIndoor.offTime = timerOff;
        updateDisplay();

        //Initial MQTT
        final String id = MqttClient.generateClientId();
        mqtt = new MqttAndroidClient(this, Constant.MQTT_URL, id);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "MQTT reconnect..."+reconnect);
                if (reconnect){
                    //Hide fault bar
                    setFaultTxt(-1);
                    //Subscribe
                    subscribeDevice();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Show fault
                if (!disconnectFlag){ setFaultTxt(R.string.no_internet); }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Update air item
                if (topic!=null && message!=null){
                    setAirItem(topic, message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String value = token.getResponse().getKey();
                Log.w(TAG, "Publish success..."+value);

            }
        });



        //Connect
        connectMQTTServer();

    }

    //Thank: https://xjaphx.wordpress.com/2011/06/13/detect-xy-coordinates-when-clicking-or-touching-on-screen/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // MotionEvent object holds X-Y values
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //String text = "You click at x = " + event.getX() + " and y = " + event.getY();
            //Function.showToast(this, text);
            //Enable scroll when user touch out of graph area
            nestedScrollView.setScrollable(true);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        disconnectMQTTServer(); //Disconnect
        super.onBackPressed();  //Back
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Set flag for show toast error
        isActive = true;
    }

    private void setRootViewColor(int roomTemp){
        //Get top color
        float av = roomTemp/10; //Modbus value
        int tone[] = getResources().getIntArray(R.array.roomTemp);
        int level = Function.getLevel(18.0f, 30.0f, 3, av);
        int topColor = Color.parseColor("#2F80ED");
        if (level<=tone.length && level>=0){
            topColor = tone[level];
        }
        //Set gradient color to background
        int[] gradientColor = { topColor, Color.parseColor("#ffffff")};
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColor);
        gd.setCornerRadius(0f);
        rootView.setBackground(gd);
    }


    private View.OnClickListener remoteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //Check ID icon
            switch (view.getId()){
                case R.id.power_icon:
                    writeCommand(lastIndoor.setOnOff(!lastIndoor.onoff));
                    break;

                case R.id.mode_icon:
                    lastIndoor.mode++; //Count mode: 0, 2, 3
                    if (lastIndoor.mode==1){
                        lastIndoor.mode = 2;
                    }else if (lastIndoor.mode>3){
                        lastIndoor.mode = 0;
                    }
                    writeCommand(lastIndoor.setMode(lastIndoor.mode));
                    break;

                case R.id.speed_level_icon:
                    lastIndoor.fan++;
                    if(lastIndoor.fan>3){ lastIndoor.fan = 0; }
                    writeCommand(lastIndoor.setFan(lastIndoor.fan));
                    break;

                case R.id.quiet_icon:
                    writeCommand(lastIndoor.setQuiet(!lastIndoor.quiet));
                    break;

                case R.id.louver_icon:
                    lastIndoor.louver++;
                    if (lastIndoor.louver>5){ lastIndoor.louver = 1; }
                    writeCommand(lastIndoor.setLouver(lastIndoor.louver));
                    break;

                case R.id.sleep_icon:
                    writeCommand(lastIndoor.setSleep(!lastIndoor.sleep));
                    break;

                case R.id.timer_icon:
                    if(timerIcon.isSelected()){
                        //Show confirm dialog before disable timer
                        Function.showAlertDialog(ControlActivity.this,
                                R.string.disable_timer, R.string.disable_timer_confirm,
                                R.string.confirm, new MyAlertDialog.OnButtonClickListener() {
                            @Override
                            public void onClose() {

                            }

                            @Override
                            public void onAction(View view) {
                                //Hide
                                Function.dismissDialogFragment(ControlActivity.this, MyAlertDialog.TAG);
                                //Disable timer without checking Power status
                                writeModbus(Indoor.COMMAND_ADDR, lastIndoor.setDisableTimer());
                            }
                        });
                    }else {
                        //Show Time Picker that opposite current Power status
                        showTimerDialog(lastIndoor.onoff?
                                TimerPickerDialog.TIMER_OFF : TimerPickerDialog.TIMER_ON);
                    }
                    break;

                case R.id.eco_icon:
                    writeCommand(lastIndoor.setEco(!lastIndoor.eco));
                    break;

                case R.id.turbo_icon:
                    writeCommand(lastIndoor.setTurbo(!lastIndoor.turbo));
                    break;
            }


        }
    };

    private View.OnTouchListener remoteTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(final View view, MotionEvent event) {

            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    Log.w(TAG_BTN, "Press button...");
                    //Write Set Point from one click
                    if (holdPressId==0){
                        writeSetPoint(getNextSetPoint(view.getId()==R.id.ic_increase));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.w(TAG_BTN, "Release button...");
                    //Stop counter
                    countTimer.cancel();
                    //Write Set point
                    if (holdPressId!=0){ writeSetPoint(setPointBuffer); }
                    //Clear 
                    holdPressId = 0;
                    break;
            }


            return false;
        }
    };

    private View.OnLongClickListener remoteLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            Log.w(TAG_BTN, "OnLongClick..."+view.getId());
            //Save ID and start counter
            holdPressId = view.getId();
            countTimer.start();
            return false;
        }
    };

    //Auto
    private CountDownTimer countTimer = new CountDownTimer(3600000, 250) {
        @Override
        public void onTick(long l) {
            //Auto count up or down set point
            if (holdPressId!=0){
                setSetPointTxt(getNextSetPoint(holdPressId==R.id.ic_increase));
                Log.w(TAG_BTN, "NOW: "+setPointBuffer);
            }

        }

        @Override
        public void onFinish() {
            countTimer.start(); //Restart
        }
    };


    private void connectMQTTServer(){
        disconnectFlag = false; //Save flag
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
                    //Subscribe
                    subscribeDevice();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed...");
                    Function.showNoResultDialog(ControlActivity.this, errorListener);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Log.e(TAG, "Connect error..."+e.getMessage());
        }

    }

    private void subscribeDevice(){
        Log.w(TAG, "Try subscribe...");
        for (String k : KEY){
            String topic = deviceHead + k;
            setSubscribe(topic);
        }
    }

    private void setSubscribe(String topic){
        try {
            //Set
            IMqttToken token = mqtt.subscribe(topic, 1);
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Subscribed topic..."
                            +Arrays.toString(asyncActionToken.getTopics()));
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "Subscribe failed..."
                            +Arrays.toString(asyncActionToken.getTopics()));
                    Function.showNoResultDialog(ControlActivity.this, errorListener);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Function.showNoResultDialog(ControlActivity.this, errorListener);
        }
    }

    private void writeCommand(int command){
        //Check power icon is pressed, if AC is OFF, it will alert "Please power ON"
        if(BitOperation.readBit(0, 1, command)!=1 && !lastIndoor.onoff){
            setPowerIconBlink();
        }else {
            writeModbus(Indoor.COMMAND_ADDR, command);
        }
    }

    private int getNextSetPoint(boolean countUp){
        if (countUp){
            setPointBuffer += 5; //Step +0.5c
            if (setPointBuffer>300){ setPointBuffer = 300; }
        }else {
            setPointBuffer -= 5; //Step -0.5c
            if (setPointBuffer<150){ setPointBuffer = 150; }
        }
        return setPointBuffer;
    }


    private void writeSetPoint(int value){
        if (isAllowWrite()){
            writeModbus(Indoor.SET_POINT_ADDR, value);
        }
    }

    private boolean isAllowWrite(){
        //If AC is OFF, it will alert "Please power ON"
        if(!lastIndoor.onoff){
            setPowerIconBlink();
            return false;
        }
        return true;
    }

    private void writeModbus(String address, int value){
        //Don't forget set ',' to end message
        String topic = deviceHead+Constant.REQUEST;
        String message = Function.addComma(
                new String[]{ Indoor.MB_WRITE, address, String.valueOf(value)});
        setPublish(topic, message);
    }

    private void setPublish(String topic, String value){
        try {
            byte[] encodedPayload = value.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            mqtt.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnectMQTTServer(){
        disconnectFlag = true;
        Log.w(TAG, "Try disconnect MQTT server...");
        if (mqtt.isConnected()){
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
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else {
            Log.w(TAG, "Disconnect success...");
        }
    }

    private void setAirItem(@NonNull String topic, @NonNull MqttMessage message){
        Log.w(TAG, "New Message ["+topic+"] "+message.toString());
        //Check topic
        String s[] = topic.split("/");
        if (s.length!=3){ return; }
        //Check ID
        int group = Function.getInt(s[0].trim());
        int device = Function.getInt(s[1].trim());
        if (group!=groupId && device!=airItem.getDeviceId()){
            return;
        }
        //Update item
        String m[] = message.toString().split(",");
        switch (s[2]){
            case Constant.PV:
                if (m.length>=3){ setSavingTxt(Function.getInt(m[2])); }
                break;
            case Constant.AC: //x10
                if (m.length>=3){ setConsumptionTxt(Function.getInt(m[2])); }
                break;
            case Constant.STATE:
                if (m.length>=Indoor.DATA_SIZE(Indoor.MB_INFO)){
                    //Set item
                    int v[] = Function.getIntArray(message.toString());
                    Time on = new Time(v[2], v[3]);
                    Time off = new Time(v[4], v[5]);
                    lastIndoor = new Indoor(v[0], v[1], on, off, v[6], v[7]);
                    //Check fault
                    if (lastIndoor.tripCode==0){
                        //Hide error bar
                        setFaultTxt(-1);
                        warningIcon.setVisibility(View.GONE);
                        //Closed dialog
                        Function.dismissDialogFragment(this, ControllerFaultDialog.TAG);
                    }else {
                        //Warning icon
                        warningIcon.setVisibility(View.VISIBLE);
                        //Set status
                        if(lastIndoor.tripCode>100){
                            setFaultTxt(R.string.indoor_fault);
                        }else {
                            setFaultTxt(R.string.outdoor_fault);
                        }
                    }
                    //Update other view
                    updateDisplay();
                }else {
                    //Show errors bar
                    setFaultTxt(R.string.no_result);
                }
                break;
            case Constant.RESPONSE: //Check response after publish request
                //Check screen state
                if(!isActive){ return; }
                //Check min length
                if(m.length<1){
                    Function.showToast(ControlActivity.this, R.string.failed);
                    return;
                }
                //Check tag
                switch (m[0]){
                    case Indoor.MB_WRITE:
                        if(m.length>=4 && m[3].equals(Constant.SUCCESS)){
                            Log.w(TAG, "Write success...["+m[1]+"] "+m[2]);
                            switch (m[1]){
                                case Indoor.COMMAND_ADDR:
                                    lastIndoor.setCommand(Function.getInt(m[2]));
                                    break;
                                case Indoor.SET_POINT_ADDR:
                                    lastIndoor.setPointTemp = Function.getInt(m[2]);
                                    break;
                            }
                            //Update item
                            updateDisplay();
                        }else {
                            Function.showToast(ControlActivity.this, R.string.failed);
                        }
                        break;
                    case Indoor.SET_TIMER:
                        if(m.length>=5 && m[4].equals(Constant.SUCCESS)){
                            Log.w(TAG, "Set Timer success..."+m[2]+" : "+m[3]);
                            //Update command
                            lastIndoor.setCommand(Function.getInt(m[1]));
                            //Update timer
                            int h = Function.getInt(m[2]);
                            int min = Function.getInt(m[3]);
                            Time time = new Time(h, min);
                            if (lastIndoor.timerOn){
                                lastIndoor.onTime = time;
                            }else if (lastIndoor.timerOff){
                                lastIndoor.offTime = time;
                            }else {
                                lastIndoor.onTime = null;
                                lastIndoor.offTime = null;
                            }
                            //Display
                            updateDisplay();
                        }else {
                            Function.showToast(ControlActivity.this, R.string.failed);
                        }
                        break;
                }
                break;

        }

    }

    void updateDisplay(){
        setRoomTempTxt(lastIndoor.roomTemp);
        if (holdPressId==0){
            setPointBuffer = lastIndoor.setPointTemp;   //Save buffer
            setSetPointTxt(lastIndoor.setPointTemp);    //Update set point
        }
        setPowerIcon(lastIndoor.onoff);
        setModeIcon(lastIndoor.mode);
        setQuietIcon(lastIndoor.quiet);     //Must set quiet icon before fan speed
        setFanIcon(lastIndoor.fan);
        setLouverIcon(lastIndoor.louver-1);
        setSleepIcon(lastIndoor.sleep);
        setTimer(lastIndoor.timerOn, lastIndoor.onTime, lastIndoor.timerOff, lastIndoor.offTime);
        setEcoIcon(lastIndoor.eco);
        setTurboIcon(lastIndoor.turbo);

    }

    private void setSavingTxt(int value){
        String txt = Function.getPowerAndUnit(value/10.0f);
        savingTxt.setText(txt);
    }

    private void setConsumptionTxt(int value){
        String txt = Function.getPowerAndUnit(value*10.0f);
        consumptionTxt.setText(txt);
    }

    private void setRoomTempTxt(int value){
        //Set room temp txt
        String txt = getString(R.string.room)+" "+Function.get1Digit(value)
                +getString(R.string.celsius_degree);
        roomTempTxt.setText(txt);
        //Update background
        setRootViewColor(value);
    }


    private void setPowerIcon(boolean on){
        powerIcon.setImageDrawable(getResources().getDrawable(on? R.drawable.power_on_icon : R.drawable.power_off_icon));
    }

    private void setSetPointTxt(int value){
        String txt = Function.get1Digit(value)+getString(R.string.ring_above);
        setPointTxt.setText(txt);
    }

    private void setModeIcon(int value){
        //Check value
        if (value<0 || value==1 || value>3){
            modeIcon.setImageDrawable(getResources().getDrawable(R.drawable.cool_icon_unpress));
            modeIcon.setSelected(false);
            return;
        }
        //Set icon
        int iconId[] = { R.drawable.fan_icon_white, -1, R.drawable.dry_icon_white,
                R.drawable.cool_icon_white };
        for (int i=0; i<iconId.length; i++){
            if (i==value){
                modeIcon.setImageDrawable(getResources().getDrawable(iconId[i]));
                modeIcon.setSelected(true);
                break;
            }
        }
    }

    private void setFanIcon(int value){
        //Check value
        if (value<0 || value>3){
            fanIcon.setImageDrawable(getResources()
                    .getDrawable(R.drawable.fan_speed_1_icon_unpress));
            fanIcon.setSelected(false);
            return;
        }
        //Set to fan speed icon, Don't forget call setQuietIcon() before this function
        int icon1[] = { R.drawable.fan_speed_1_icon_press, R.drawable.fan_speed_2_icon_press,
                R.drawable.fan_speed_3_icon_press, R.drawable.auto_icon_white };
        int icon2[] = { R.drawable.fan_speed_1_icon_unpress, R.drawable.fan_speed_2_icon_unpress,
                R.drawable.fan_speed_3_icon_unpress, R.drawable.auto_icon_unpress };
        int icon[]; boolean active;
        if (quietIcon.isSelected()){
            icon = icon2;
            active = false;
        }else {
            icon = icon1;
            active = true;
        }
        for (int i=0; i<icon1.length; i++){
            if (i==value){
                fanIcon.setImageDrawable(getResources().getDrawable(icon[i]));
                fanIcon.setSelected(active);
            }
        }

    }

    private void setQuietIcon(boolean value){
        quietIcon.setImageDrawable(getResources().getDrawable(
                value? R.drawable.quiet_icon:R.drawable.quiet_icon_unactive));
        quietIcon.setSelected(value);
    }

    private void setLouverIcon(int index){
        int icon[] = { R.drawable.louver_level_4_icon, R.drawable.louver_level_3_icon,
                R.drawable.louver_level_2_icon, R.drawable.louver_level_1_icon,
                R.drawable.louver_swing_icon };
        if (index>=0 && index<icon.length){
            louverIcon.setImageDrawable(getResources().getDrawable(icon[index]));
            louverIcon.setSelected(true);
        }else {
            louverIcon.setSelected(false);
        }
    }

    private void setSleepIcon(boolean value){
        sleepIcon.setImageDrawable(getResources().getDrawable(
                value? R.drawable.sleep_icon_white:R.drawable.sleep_icon_unactive));
        sleepIcon.setSelected(value);
    }

    private void setTimerIcon(boolean value){
        timerIcon.setImageDrawable(getResources().getDrawable(
                value? R.drawable.timer_icon_white:R.drawable.timer_icon_unactive));
        timerIcon.setSelected(value);
        //Set timeOnOff layout
        timeOnOffTxt.setVisibility(value? View.VISIBLE : View.GONE);
    }

    private void setTimer(boolean timerOnFlag, Time timeOn, boolean timerOffFlag, Time timeOff){
        if (timerOnFlag && timeOn!=null){
            String t = getString(R.string.power_on_in) +" "+timeOn.getTimerText();
            timeOnOffTxt.setText(t);
            setTimerIcon(true);
        }else if (timerOffFlag && timeOff!=null){
            String t = getString(R.string.power_off_in) +" "+timeOff.getTimerText();
            timeOnOffTxt.setText(t);
            setTimerIcon(true);
        }else {
            setTimerIcon(false);
        }
    }

    private void setEcoIcon(boolean value){
        ecoIcon.setImageDrawable(getResources().getDrawable(
                value? R.drawable.eco_icon_active:R.drawable.eco_icon_unactive));
        ecoIcon.setSelected(value);
    }

    private void setTurboIcon(boolean value){
        turboIcon.setImageDrawable(getResources().getDrawable(
                value? R.drawable.turbo_icon:R.drawable.turbo_icon_unactive));
        turboIcon.setSelected(value);
    }


    //Thank: http://www.akexorcist.com/2014/07/android-code-object-animator.html
    private void setPowerIconBlink(){
        //Set blink
        ObjectAnimator animator = ObjectAnimator.ofFloat(powerIcon, View.ALPHA, 0f, 1f);
        animator.setDuration(250);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(2);
        animator.start();
        //Set vibrate
        Function.setOneShortVibrator(this);
        //Show text
        Function.showToast(this, R.string.alert_power_off);
    }


    private void setFaultTxtAnim(){
        faultTxtAnim = ObjectAnimator.ofFloat(faultTxt, View.ALPHA, 0f, 1f);
        faultTxtAnim.setDuration(1000);
        faultTxtAnim.setRepeatMode(ValueAnimator.REVERSE);
        faultTxtAnim.setRepeatCount(ValueAnimator.INFINITE);
    }

    private void setFaultTxt(int id){
        //Log.w(TAG, "Display Fault: "+id);
        if (id>0){
            //Text
            faultTxt.setText(id);
            faultTxt.setVisibility(View.VISIBLE);
            //Vibrate
            //Function.setOneShortVibrator(this);
            //Blink
            if (!faultTxtAnim.isStarted()){ faultTxtAnim.start(); }
        }else {
            //Gone
            faultTxt.setVisibility(View.GONE);
            //Reset blink
            if (faultTxtAnim.isRunning()){ faultTxtAnim.end(); }

        }
    }

    private void showControllerFaultDialog(int tripCode){
        //Pass data to DialogFragment
        //Thank: https://stackoverflow.com/questions/15459209/passing-argument-to-dialogfragment
        Bundle bundle = new Bundle();
        bundle.putInt(ControllerFaultDialog.CODE, tripCode);
        //Create dialog
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        ControllerFaultDialog dialog = new ControllerFaultDialog();
        dialog.setArguments(bundle);
        dialog.show(transaction, ControllerFaultDialog.TAG);
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            switch (titleId){
                case R.string.no_result:
                case R.string.no_internet:
                    connectMQTTServer();
                    break;
            }

        }
    };

    private View.OnClickListener dateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.today_chart_txt:  //Show today graph
                    setDateSelected(0);
                    loadDataToPlotGraph(Function.getDate(0));
                    break;
                case R.id.yesterday_chart_txt:  //Show yesterday graph
                    setDateSelected(1);
                    loadDataToPlotGraph(Function.getDate(-1));
                    break;
            }
        }
    };

    private void setDateSelected(int n){
        for (int i=0; i<dateTxt.length; i++){
            dateTxt[i].setTextColor(i==n?  dateTxtColor[0] : dateTxtColor[1]);
        }
    }

    private void showGraph(int n){
        for (int i=0; i<graphSectionView.length; i++){
            graphSectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    //Date must in format: YYYY-MM-DD
    private void loadDataToPlotGraph(String date){
        //Load
        showGraph(LOAD_GRAPH);
        //Try
        HomeManager homeManager = new HomeManager(this);
        homeManager.readDeviceLogData(groupId, airItem.getDeviceId(), date,
                new HomeManager.OnLogDataListener() {
            @Override
            public void onSuccess(float sumEachId[], float total, float maxValueAtTime, List<ChartItem> chartItems) {
                Log.w(TAG, "Download graph success...");
                //Prepare data to set chart
                ArrayList<Entry> savingData = new ArrayList<>();
                ArrayList<Entry> consumptionData = new ArrayList<>();
                ArrayList<String> timeData = new ArrayList<>();
                for (int i=0; i<chartItems.size(); i++){
                    ChartItem item = chartItems.get(i);
                    consumptionData.add(new Entry(i, item.values[0]));
                    savingData.add(new Entry(i, item.values[1]));
                    timeData.add(item.time);
                }
                //Plot
                plotChart(timeData, savingData, consumptionData);
                //Show
                showGraph(SHOW_GRAPH);
            }

            @Override
            public void onFailed(String error) {
                Log.e(TAG, "Download graph failed: "+error);
                showGraph(FAILED_GRAPH);
            }
        });
    }

    private final int[] chartTitle = new int[]{
            R.string.saving,
            R.string.consumption
    };

    //Thank: https://github.com/PhilJay/MPAndroidChart
    //At example: MultiLineChartActivity.java
    //At MPAndroid idChart Example app (Google Play) -> Line Charts -> Multiple
    private void plotChart(final ArrayList<String> xLabel, ArrayList<Entry> savingData,
                           final ArrayList<Entry> consumptionData){
        //Clear
        chart.resetTracking();
        //Add data to dataSets
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        SparseArray<ArrayList<Entry>> dataList = new SparseArray<>();
        dataList.put(0, savingData);
        dataList.put(1, consumptionData);
        for (int i=0; i<chartTitle.length; i++){
            //Set data
            String title = getString(chartTitle[i]);
            LineDataSet d = new LineDataSet(dataList.get(i), title);
            //Set line style
            d.setLineWidth(2.0f);
            d.setMode(LineDataSet.Mode.LINEAR); //Linear line
            d.setDrawCircles(false); //Not show dot circle
            d.setDrawFilled(true);   //Fill color
            d.setFillColor(areaChartColor[i]);
            d.setColor(lineChartColor[i]);
            d.setDrawValues(false); //Not show value on line
            //Add to dataSet
            dataSets.add(d);
        }
        //Add line
        LineData data = new LineData(dataSets);
        chart.setData(data);
        //Set X label
        chart.getXAxis().setValueFormatter(new ValueFormatter() {

            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                //return super.getAxisLabel(value, axis);
                //Thank: https://github.com/PhilJay/MPAndroidChart/issues/133
                //Modify by Somsak Elect 25/06/2019
                int n = (int)value%consumptionData.size();
                float decimal = value-n;
                Log.w("MyLabel", "X: "+value+", decimal: "+decimal+", N: "+n);
                if (decimal>0.1f){
                    return ""; //Not show label
                }else {
                    return xLabel.get(n); //Show label
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
        nestedScrollView.setScrollable(true);

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

    private void showTimerDialog(int type){
        //Pass data
        Bundle bundle = new Bundle();
        bundle.putInt(TimerPickerDialog.TYPE, type);
        //Create dialog
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        TimerPickerDialog dialog = new TimerPickerDialog();
        dialog.setArguments(bundle);
        dialog.addOnSaveTimeListener(new TimerPickerDialog.OnSaveTimeListener() {
            @Override
            public void onSave(int type, int hour, int minute) {
                Log.w(TAG, "Save time: "+type+" :: "+hour+":"+minute);
                //Set command
                int command = type==TimerPickerDialog.TIMER_ON?
                        lastIndoor.setTimerOn(true) : lastIndoor.setTimerOff(true);
                //Set message
                //Don't forget set ',' to end message
                String topic = deviceHead+Constant.REQUEST;
                String message = Function.addComma(
                        new String[]{Indoor.SET_TIMER,
                                String.valueOf(command), String.valueOf(hour), String.valueOf(minute)});
                //Publish
                setPublish(topic, message);
            }
        });
        //Show
        dialog.show(transaction, TimerPickerDialog.TAG);

    }


}
