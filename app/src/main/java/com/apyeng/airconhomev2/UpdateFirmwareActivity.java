package com.apyeng.airconhomev2;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

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
import java.util.Arrays;

public class UpdateFirmwareActivity extends AppCompatActivity {

    //Object in Layout
    private ImageView backImg;
    private View layView[];
    private RoundButtonWidget actionBtn;
    //MQTT
    private MqttAndroidClient mqtt;
    private String topicUpdated, topicUpdating;
    private boolean disconnectFlag;
    //Other
    private String filename, nowVersion, newVersion;
    private final static int CHECKING = 0, AVAILABLE = 1, UPDATING = 2, UPDATED = 3, FAILED = 4;
    private final static String TAG = "UpdateFirmwareActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_firmware);

        //Get data
        nowVersion = getIntent().getStringExtra(Constant.VERSION);
        String deviceHead = getIntent().getStringExtra(Constant.DEVICE_HEAD);
        if (nowVersion==null || deviceHead==null){
            throw new IllegalArgumentException("Must pass version code and device head...");
        }

        //Toolbar
        TextView titleTool = findViewById(R.id.title_toolbar);
        backImg = findViewById(R.id.back_icon);
        //Title
        titleTool.setText(R.string.update_title);

        //Add layout to array
        layView = new View[5];
        layView[0] = findViewById(R.id.lay_checking);
        layView[1] = findViewById(R.id.lay_available);
        layView[2] = findViewById(R.id.lay_updating);
        layView[3] = findViewById(R.id.lay_updated);
        layView[4] = findViewById(R.id.lay_failed);

        //Action button
        actionBtn = findViewById(R.id.action_btn);
        actionBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Connect MQTT and request to update firmware
                connectMQTTServer();
            }
        });

        //Back
        backImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Disconnect MQTT
                disconnectMQTTServer();
                //Back
                finish();
            }
        });

        //Set topic
        topicUpdated = deviceHead+Constant.FIRMWARE_UPDATED;
        topicUpdating = deviceHead+Constant.FIRMWARE_UPDATING;

        //Initial MQTT
        final String id = MqttClient.generateClientId();
        mqtt = new MqttAndroidClient(this, Constant.MQTT_URL, id);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "MQTT reconnect..."+reconnect);
                if (reconnect){
                    subscribeUpdated();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Show fault
                if (!disconnectFlag){
                    Function.showNoInternetDialog(UpdateFirmwareActivity.this, errorListener);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Update about items
                if (topic!=null && message!=null){
                    String m = message.toString();
                    Log.w(TAG, "["+topic+"] "+m);
                    //Check updated?
                    if (m.equals(newVersion)){
                        //Updated
                        showLayout(UPDATED);
                    }else {
                        //Failed
                        showLayout(FAILED);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String value = token.getResponse().getKey();
                Log.w(TAG, "Publish success..."+value);

            }
        });

        //Initial
        checkDeviceFirmware();


    }

    @Override
    public void onBackPressed() {
        //Allow back by navigator bar only when back icon is enable.
        if (backImg.isEnabled()) {
            //Disconnect MQTT
            disconnectMQTTServer();
            //Back
            super.onBackPressed();
        }
    }

    private void showLayout(int n){
        //Set main contain visible
        for (int i=0; i<layView.length; i++){
            layView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
        //Set action button visible
        actionBtn.setVisibility(n==AVAILABLE? View.VISIBLE : View.GONE);
        //Back button enable
        backImg.setEnabled(n!=UPDATING);
    }

    private void checkDeviceFirmware(){
        //Show Checking
        showLayout(CHECKING);
        //Read data from device_firmware table
        Ion.with(this)
                .load(Constant.DEVICE_FIRMWARE)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        //Check result
                        Log.e(TAG, "Error: "+e);
                        Log.w(TAG, "Result: "+result);
                        if (result!=null && !result.isJsonNull()){
                            if (!result.has(Constant.ERROR)){
                                filename = result.get(Constant.FILENAME).getAsString();
                                String details = result.get(Constant.DETAILS).getAsString();
                                String md5 = result.get(Constant.MD5).getAsString();
                                //Get version
                                String b[] = filename.split(".bin");
                                if (b.length>0){
                                    String v[] = b[0].split("v");
                                    if (v.length>0){
                                        newVersion = v[1];
                                        if (!newVersion.equals(nowVersion)){
                                            //Set new version
                                            TextView tvV = findViewById(R.id.tv_version);
                                            String vv = "v"+newVersion+" | "+md5;
                                            tvV.setText(vv);
                                            //Set detail
                                            TextView tvD = findViewById(R.id.tv_details);
                                            tvD.setText(details);
                                            //Show available layout
                                            showLayout(AVAILABLE);
                                        }else {
                                            showLayout(UPDATED);
                                        }
                                    }else {
                                        showLayout(UPDATED);
                                    }
                                }else{
                                    showLayout(UPDATED);
                                }
                            }else{
                                showLayout(UPDATED);
                            }
                        }else {
                            //Show no result
                            showErrorAndDelayBacking(getString(R.string.no_result));
                        }
                    }
                });
    }

    private void showErrorAndDelayBacking(String error){
        Function.showToast(UpdateFirmwareActivity.this, error);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);
    }

    private void connectMQTTServer(){
        //Layout
        showLayout(UPDATING);
        //Save flag
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
                    //Subscribe
                    subscribeUpdated();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed...");
                    Function.showNoResultDialog(UpdateFirmwareActivity.this, errorListener);
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

    private void subscribeUpdated(){
        try {
            //Set
            IMqttToken token = mqtt.subscribe(topicUpdated, 1);
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Subscribed topic..."
                            + Arrays.toString(asyncActionToken.getTopics()));
                    //Request updating
                    requestUpdating();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "Subscribe failed..."
                            +Arrays.toString(asyncActionToken.getTopics()));
                    Function.showNoResultDialog(UpdateFirmwareActivity.this, errorListener);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Function.showNoResultDialog(UpdateFirmwareActivity.this, errorListener);
        }
    }

    private void requestUpdating(){
        try {
            byte[] encodedPayload = filename.getBytes("UTF-8"); //Pass filename
            MqttMessage message = new MqttMessage(encodedPayload);
            mqtt.publish(topicUpdating, message);
        } catch (UnsupportedEncodingException | MqttException e) {
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
                    connectMQTTServer();
                    break;
            }

        }
    };




}
