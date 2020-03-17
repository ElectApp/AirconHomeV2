package com.apyeng.airconhomev2;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class AboutActivity extends AppCompatActivity {

    //Layout
    private View viewLay[];
    private final static int LAY_LOADING = 0, LAY_LIST = 1;
    private List<AboutItem> aboutItems;
    private AboutItemAdapter adapter;
    //MQTT
    private MqttAndroidClient mqtt;
    private String requestTopic, responseTopic;
    private boolean disconnectFlag;
    //Other
    private final static int UPDATE_CODE = 1149;
    private final static String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //Get data
        final String deviceHead = getIntent().getStringExtra(Constant.DEVICE_HEAD);
        if(deviceHead==null){
            throw new IllegalArgumentException("Must pass device head data...");
        }
        //Update topic
        requestTopic = deviceHead + Constant.REQUEST;
        responseTopic = deviceHead + Constant.RESPONSE;

        TextView toolbarTitle = findViewById(R.id.title_toolbar);
        ProgressBar progressBar = findViewById(R.id.progress_lay);
        RecyclerView aboutRv = findViewById(R.id.rv_about);

        toolbarTitle.setText(getString(R.string.about));

        viewLay = new View[2];
        viewLay[0] = progressBar;
        viewLay[1] = aboutRv;

        //Back icon
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Disconnect
                disconnectMQTTServer();
                finish();
            }
        });

        //Initial List
        aboutItems = new ArrayList<>();
        LinearLayoutManager rvManager = new LinearLayoutManager(this);
        adapter = new AboutItemAdapter(this, aboutItems);
        adapter.setAboutItemClickListener(new AboutItemAdapter.OnAboutItemClickListener() {
            @Override
            public void onClick(View view, int position, AboutItem item) {
                //Function.showToast(AboutActivity.this, item.getTitle());
                if (position==0){
                    //Update firmware
                    Intent intent = new Intent(AboutActivity.this, UpdateFirmwareActivity.class);
                    intent.putExtra(Constant.DEVICE_HEAD, deviceHead); //Head for MQTT
                    intent.putExtra(Constant.VERSION, aboutItems.get(3).getDetail()); //Now version
                    startActivityForResult(intent, UPDATE_CODE);
                }
            }
        });
        aboutRv.setLayoutManager(rvManager);
        aboutRv.setAdapter(adapter);

        //Initial MQTT
        final String id = MqttClient.generateClientId();
        mqtt = new MqttAndroidClient(this, Constant.MQTT_URL, id);
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "MQTT reconnect..."+reconnect);
                if (reconnect){
                    subscribeResponseTopic();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Show fault
                if (!disconnectFlag){
                    Function.showToast(AboutActivity.this, R.string.no_internet);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Update about items
                if (topic!=null && message!=null){
                    setAboutItems(topic, message.toString());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String value = token.getResponse().getKey();
                Log.w(TAG, "Publish success..."+value);

            }
        });

        //Show loading
        showContain(LAY_LOADING);

        //Connect MQTT
        connectMQTTServer();

    }

    @Override
    public void onBackPressed() {
        disconnectMQTTServer(); //Disconnect
        super.onBackPressed();  //Back
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.w(TAG, "Result: ["+requestCode+"] "+resultCode);

    }

    private void showContain(int n){
        for (int i=0; i<viewLay.length; i++){
            viewLay[i].setVisibility(i==n? View.VISIBLE:View.GONE);
        }
    }

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
                    subscribeResponseTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed...");
                    Function.showNoResultDialog(AboutActivity.this, errorListener);
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

    private void subscribeResponseTopic(){
        try {
            //Set
            IMqttToken token = mqtt.subscribe(responseTopic, 1);
            //Check result
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Subscribed topic..."
                            + Arrays.toString(asyncActionToken.getTopics()));
                    //Request about data
                    publishRequestTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "Subscribe failed..."
                            +Arrays.toString(asyncActionToken.getTopics()));
                    Function.showNoResultDialog(AboutActivity.this, errorListener);
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
            Function.showNoResultDialog(AboutActivity.this, errorListener);
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

    private void publishRequestTopic(){
        String value = Indoor.ABOUT+",";
        try {
            byte[] encodedPayload = value.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            mqtt.publish(requestTopic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void setAboutItems(@NonNull String topic, @NonNull String message){
        Log.w(TAG, "New Message ["+topic+"] "+message);
        String m[] = message.split(",");
        Log.w(TAG, "L: "+m.length);
        //Clear
        aboutItems.clear();
        //Check tag
        if(m.length>0 && m[0].equals(Indoor.ABOUT)){
            //Title
            String titles[] = getResources().getStringArray(R.array.about_title_items);
            int len = titles.length-1;
            aboutItems.add(new AboutItem(titles[0], null, true));
            //Check message
            if(m.length>=8){
                //Set item
                for (int i=1; i<len; i++){
                    aboutItems.add(new AboutItem(titles[i], m[i], i==4||i==7));
                }
            }else{
                Log.e(TAG, "Message is out of rang.");
                //Set item
                for (int i=1; i<len; i++){
                    aboutItems.add(new AboutItem(titles[i], "-", i==4||i==7));
                }
            }
            aboutItems.add(new AboutItem(titles[len], "A.P.Y. Engineering Co.,Ltd.", true));
            //Update adapter
            adapter.notifyDataSetChanged();
            //Show
            showContain(LAY_LIST);
        }



    }



}
