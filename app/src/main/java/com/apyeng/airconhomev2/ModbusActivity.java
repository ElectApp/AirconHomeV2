package com.apyeng.airconhomev2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class ModbusActivity extends AppCompatActivity {

    //Layout
    private Switch mbSw;
    private TextView conTxt, statusTxt;
    private View sectionView[];

    //MQTT
    private int groupId;

    //TCP server
    private String myIP;
    private MyTCPServer tcpServer;
    private ServerTaskIn taskIn;
    private static final long TCP_TIMEOUT = 10000;
    private static final int TCP_Port = 5020; //For mobile must in rang 1024 - 65535
    private static final int STARTED = 1, CONNECTED = 2, DISCONNECTED = 6, ERROR = -1;
    private static final int TCP_BUF_LEN = 512;
    //MB
    private static final int MB_TCP_HEAD = 6, MB_REQ_MIN = 6;
    private short transID;
    private byte[] mbResp;
    private boolean hasMbResp;
    private static final String MB_POLL_REQ = "modbus_poll/request/",
            MB_POLL_RESP = "modbus_poll/response/";

    //Other
    private int netType;
    private List<ModbusItem> items;
    private ModbusItemAdapter adapter;
    private static boolean first = false;
    private static final int PROGRESS = 0, CONTENT = 1, NO_AC = 2;
    private static final String TAG = "ModbusTCP";

    //Notification
    private static final String CHANNEL_ID = "AC-MB-TCP/IP";
    private int NOTIFY_CODE; //Must change every time that create a new notification


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modbus);

        //Get data
        groupId = getIntent().getIntExtra(Constant.GROUP_ID, 0);

        //Layout
        mbSw = findViewById(R.id.mb_sw);
        statusTxt = findViewById(R.id.status_txt);
        conTxt = findViewById(R.id.con_detail_txt);
        RecyclerView deviceRv = findViewById(R.id.device_rv);
        sectionView = new View[3];
        sectionView[0] = findViewById(R.id.circle_progress);
        sectionView[1] =  findViewById(R.id.container);
        sectionView[2] = findViewById(R.id.no_ac_content);

        //TCP server
        tcpServer = new MyTCPServer();

        //Back
        findViewById(R.id.back_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stop
                stopTCPServer();
                //Back
                finish();
            }
        });

        //ON/OFF
        mbSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //TCP server
                if (isChecked){
                    //Start
                    checkInternetType();
                }else{
                    //Stop
                    stopTCPServer();
                }
            }
        });

        //Task In
        taskIn = new ServerTaskIn(TCP_Port, TCP_TIMEOUT, new ArrayList<String>(), new ArrayList<String>());

        //Initial Device list
        items = new ArrayList<>();
        adapter = new ModbusItemAdapter(items);
        deviceRv.setLayoutManager(new LinearLayoutManager(this));
        deviceRv.setAdapter(adapter);

        //Read device
        tryReadDeviceDetail();

    }

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }
    }

    private void tryReadDeviceDetail(){
        //Loading
        showContent(PROGRESS);
        //Action
        if (Function.internetConnected(this)){
            HomeManager homeManager = new HomeManager(this);
            homeManager.readFullDeviceDetail(groupId, new HomeManager.OnReadFullDeviceDetailListener() {
                @Override
                public void onSuccess(List<AirItem> airItems) {
                    Log.w(TAG, "OnSuccess...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(ModbusActivity.this);
                    //Add list
                    items.clear();
                    String id, aN;
                    AirItem ac;
                    for (int i=0; i<airItems.size(); i++){
                        //Device list
                        id = String.valueOf(i+1);
                        ac = airItems.get(i);
                        aN = ac.getActualName();
                        items.add(new ModbusItem(id, ac.getNickname(), aN));
                        //MQTT topic
                        taskIn.getPubTopics().add(MB_POLL_REQ+aN);
                        taskIn.getSubTopics().add(MB_POLL_RESP+aN);
                        Log.w(TAG, "Found: ["+(i+1)+"] "+aN);
                    }
                    adapter.notifyDataSetChanged();
                    //Show
                    boolean ok = items.size()>0;
                    showContent(ok? CONTENT:NO_AC);
                    //Enable
                    mbSw.setEnabled(ok);
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showNoResultDialog(ModbusActivity.this, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(ModbusActivity.this, errorListener);
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            if(items.size()>0){
                checkInternetType();
            }else {
                tryReadDeviceDetail();
            }
        }
    };

    private void checkInternetType(){
        //Hide dialog
        Function.dismissAppErrorDialog(ModbusActivity.this);
        //Get type
        netType = Function.getConnectionType(this);
        Log.w(TAG, "Internet type: "+netType);
        switch (netType){
            case 1: //4G
                myIP = getWifiApIpAddress();
                if(myIP==null){
                    //Request turn ON hotspot
                    Function.showAppErrorDialog(this, R.string.req_hotspot, R.string.done, errorListener);
                }else {
                    //Start TCP server
                    startTCPServer();
                }
                break;
            case 2: //Wi-Fi
                myIP = getWifiIpAddress();
                if(myIP.equals("0.0.0.0")){
                    //Error
                    Function.showNoResultDialog(ModbusActivity.this, errorListener);
                }else {
                    //Start TCP server
                    startTCPServer();
                }
                break;
            default: //No internet
                Function.showNoInternetDialog(ModbusActivity.this, errorListener);
        }

    }

    //Thank: https://stackoverflow.com/a/15060411
    private String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            //Log.w(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.getMessage());
        }
        return null;
    }

    private String getWifiIpAddress(){
        WifiManager mWifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = mWifiManager.getConnectionInfo().getIpAddress();
        return String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress>>8 & 0xff), (ipAddress>>16 & 0xff), (ipAddress>>24 & 0xff));
    }

    private void startTCPServer(){
        Log.w(TAG, "Start TCP server: "+myIP+":"+TCP_Port);
        //TCP connection
        String con = "IP="+myIP+", Port="+TCP_Port;
        conTxt.setText(con);
        //Start server
        tcpServer = new MyTCPServer();
        tcpServer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, taskIn);
    }


    private void showRunningStatus(){
        statusTxt.setText(R.string.running);
        statusTxt.setBackgroundColor(getResources().getColor(R.color.colorGreen));
        showFaultBar(true);
        //Show notify
        showNotification(getString(R.string.running)+": "+conTxt.getText());
        //Show dialog only one time
        if(first){ return; }
        first = true;
        String detail = getString(R.string.mb_ready_detail);
        if(netType==1){
            //Hotspot
            detail += " "+getString(R.string.u_hotspot);
        }else {
            //Wi-Fi
            detail += " "+getString(R.string.u_wifi);
        }
        Function.showAppErrorDialog(this, R.string.mb_ready_title,
                detail, R.string.ok, new AppErrorDialog.OnClickActionButtonListener() {
            @Override
            public void onClick(View view, int titleId, int buttonId) {
                Function.dismissAppErrorDialog(ModbusActivity.this);
            }
        });
    }

    private void showRunningStatus(String clientIP){
        String t = "Connected "+clientIP;
        statusTxt.setText(t);
        statusTxt.setBackgroundColor(getResources().getColor(R.color.colorGreen));
        showFaultBar(true);
        //Show notify
        showNotification(t);
    }

    private void showFaultStatus(String txt){
        statusTxt.setText(txt);
        statusTxt.setBackgroundColor(getResources().getColor(R.color.colorRed));
        showFaultBar(true);
    }

    private void showFaultBar(boolean show){
        statusTxt.setVisibility(show? View.VISIBLE : View.GONE);
    }

    private void stopTCPServer(){
        //TCP
        if(tcpServer!=null && !tcpServer.isCancelled()){
            tcpServer.stop();
        }
    }

    private class MyTCPServer extends AsyncTask<ServerTaskIn, ServerTask, Void>{
        //TCP
        private ServerSocket serverSocket;
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        //MQTT
        private MqttAndroidClient mqtt;
        private boolean disconnectFlag;

        @Override
        protected Void doInBackground(ServerTaskIn... taskIns) {
            final ServerTaskIn taskIn = taskIns[0];
            //Initial MQTT
            final String id = MqttClient.generateClientId();
            mqtt = new MqttAndroidClient(ModbusActivity.this, Constant.MQTT_URL, id);
            mqtt.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.w(TAG, "MQTT reconnect..."+reconnect);
                    //Subscribe
                    for(String s : taskIn.getSubTopics()){
                        setSubscribe(s);
                    }
                    //Status
                    if(socket!=null && socket.isConnected()){
                        publishProgress(new ServerTask(CONNECTED, getIP(socket.getInetAddress()), null));
                    }else {
                        publishProgress(new ServerTask(STARTED, String.valueOf(taskIn.getTcpPort()), null));
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    //Show no internet
                    if (!disconnectFlag){
                        publishProgress(new ServerTask(ERROR, "MQTT Lost", null));
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    //Check
                    if (topic==null || message==null){ return;}
                    //Set slave ID
                    int slaveId = 0;
                    for(int x=0; x<taskIn.getSubTopics().size(); x++){
                        if(taskIn.getSubTopics().get(x).equals(topic)){
                            slaveId = x+1;
                        }
                    }
                    //Set response message
                    if (slaveId>0 && slaveId<254 && message.getPayload()!=null){
                        int i = MB_TCP_HEAD, len = 6;
                        mbResp = new byte[TCP_BUF_LEN];
                        //Log.w(TAG, "Slave Response: "+Arrays.toString(message.getPayload()));
                        //Copy
                        for(byte b : message.getPayload()){
                            mbResp[i++] = b;
                        }
                        //Set Head
                        mbResp[0]= (byte) (transID>>8); mbResp[1]= (byte) transID; mbResp[2]=0; mbResp[3]=0;
                        mbResp[4]=0; mbResp[5]= (byte) len; //Message Length (ignore 'MB_TCP_HEAD')
                        switch (mbResp[MB_TCP_HEAD+1]){
                            case 3:
                                len = mbResp[MB_TCP_HEAD+2]+3;
                                mbResp[5] = (byte) (len);        //Low
                                mbResp[4] = (byte) (len>>8);     //High
                                //Log.w(TAG, "LEN="+len+", H="+mbResp[4]+", L="+mbResp[5]);
                                break;
                        }
                        //Set slave ID
                        mbResp[MB_TCP_HEAD] = (byte) (slaveId);
                        //Resize
                        mbResp = Arrays.copyOf(mbResp, len+MB_TCP_HEAD);
                        Log.w(TAG, "TCP Prepare Write: "+Arrays.toString(mbResp));
                        hasMbResp = true;
                        //Note!! Can't write to client at here!!! because it error 'Socket is closed'
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
            //Connect MQTT
            connectMQTTServer();

            //Start TCP
            long startTime;
            byte[] buffer = new byte[TCP_BUF_LEN];
            String ip = "";
            transID = 1;
            hasMbResp = false;
            serverSocket = null;
            try {
                //Create server socket
                serverSocket = new ServerSocket(taskIn.getTcpPort());
                //Started
                publishProgress(new ServerTask(STARTED, String.valueOf(taskIn.getTcpPort()), null));
                Log.w(TAG, "TCP server is running..."+taskIn.getTcpPort());
                //Waiting client connect
                while (!serverSocket.isClosed()){
                    //Create socket and wait client connect
                    socket = serverSocket.accept();
                    //Get client IP
                    ip = getIP(socket.getInetAddress());
                    publishProgress(new ServerTask(CONNECTED, ip, null));
                    //Save time
                    startTime = System.currentTimeMillis();
                    Log.w(TAG, "TCP client connected..."+ip+" at "+startTime);
                    //Connecting loop
                    while (socket.isConnected() && !serverSocket.isClosed()){
                        int data_len1 = 0, data_len2, data_len = 0;
                        //Create object for receiving data request from client.
                        inputStream = socket.getInputStream();
                        //Log.w(TAG, "Waiting data incoming..."+hasMbResp);

                        //Check timeout or client disconnect
                        if((System.currentTimeMillis()-startTime)>taskIn.getTcpTimeout()){
                            Log.w(TAG, "TCP Timeout!");
                            serverSocket.close();
                            break;
                        }

                        //Check response data from slave that set by MQTT subscribe
                        //Must call below 'socket.getInputStream()'
                        if(mbResp!=null && hasMbResp){
                            //Write to client
                            outputStream = socket.getOutputStream();
                            outputStream.write(mbResp);
                            outputStream.flush();
                            Log.w(TAG, "TCP Written..."+transID);
                            //Clear flag
                            hasMbResp = false;
                        }

                        //Check TCP data incoming
                        while (inputStream.available()>0){
                            //Update time
                            startTime = System.currentTimeMillis();
                            //Save input data
                            data_len2 = inputStream.available();
                            data_len += data_len2;
                            //Log.w(TAG, "IN: "+data_len2);
                            if (data_len>TCP_BUF_LEN){
                                data_len = 0; //No response and read for clear buffer
                                data_len1 = inputStream.read(buffer, 0, TCP_BUF_LEN);
                            }else {
                                data_len1 = inputStream.read(buffer, data_len1, data_len);
                            }
                        }
                        //Publish to slave
                        if(data_len>0){
                            //Log.w(TAG, "Bytes IN: "+data_len);
                            //Is Modbus TCP Request?
                            if(data_len>=(MB_TCP_HEAD+MB_REQ_MIN) && buffer[MB_TCP_HEAD]>0 &&
                                    buffer[MB_TCP_HEAD]<=taskIn.getSubTopics().size()){
                                //Save TransID
                                transID = (short)(((buffer[0]<<8)&0xFF00) | (buffer[1]&0x00FF));
                                //Log.w(TAG, "TransID="+transID+", H="+((buffer[0]<<8)&0xFF00)+", L="+(buffer[1]&0x00FF));
                                Log.w(TAG, "TCP TransID="+transID);
                                //Topic
                                String topic = taskIn.getPubTopics().get(buffer[MB_TCP_HEAD]-1);
                                //Data
                                byte[] payload = Arrays.copyOfRange(buffer, MB_TCP_HEAD, data_len);
                                //Publish to device
                                setPublish(topic, payload);
                                //Clear flag
                                hasMbResp = false;
                            }
                        }
                    }
                }
                Log.w(TAG, "Client disconnect");
                publishProgress(new ServerTask(DISCONNECTED, ip, null));
                //Disconnect client
                if (socket!=null && socket.isConnected()){ socket.close(); }
                //Clear
                if (inputStream!=null){ inputStream.close(); }
                if (outputStream!=null){ outputStream.close(); }
            } catch (IOException e) {
                e.printStackTrace();
                String err = e.getMessage();
                Log.e(TAG, "TCP ERROR: "+err);
                //Disconnect
                disconnectMQTTServer();
                //Error
                publishProgress(new ServerTask(ERROR, err, null));
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(ServerTask... values) {
            super.onProgressUpdate(values);

            if (values!=null && values[0]!=null){
                //Get detail
                String detail = values[0].getStatusDetail();
                if(detail==null){ return; }
                //Check code
                switch (values[0].getStatusCode()){
                    case ERROR:
                        showFaultStatus(detail);
                        break;
                    case STARTED:
                        clearNotification(); //Clear
                        NOTIFY_CODE = (int) (System.currentTimeMillis()); //Get new id
                        showRunningStatus();
                        break;
                    case CONNECTED:
                        showRunningStatus(detail);
                        break;
                }
            }

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.w(TAG, "TCP server end!");
            //Update toolbar
            showFaultBar(false);
            mbSw.setChecked(false);
            //Remove notify
            clearNotification();
        }

        private void stop(){
            try {
                if (serverSocket!=null){  serverSocket.close(); }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getIP(InetAddress inetAddress){
            String ip = inetAddress.toString(); //EX. /192.168.137.1
            if(ip.contains("/")){ip = ip.replace("/", "");} //Remove "/"
            return ip;
        }

        //========================== START MQTT ===========================//

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

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Connection failed...");
                        publishProgress(new ServerTask(ERROR, "MQTT connection failed", null));
                    }
                });
            }catch (MqttException e){
                e.printStackTrace();
                Log.e(TAG, "Connect error..."+e.getMessage());
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
                        publishProgress(new ServerTask(ERROR, "MQTT Subscribe Error", null));
                    }
                });
            }catch (MqttException e){
                e.printStackTrace();
                Function.showNoResultDialog(ModbusActivity.this, errorListener);
            }
        }

        private void setPublish(String topic, byte[] payload){
            Log.w(TAG, "Publish: ["+topic+"] "+Arrays.toString(payload));
            try {
                MqttMessage message = new MqttMessage(payload);
                mqtt.publish(topic, message);
            } catch (MqttException e) {
                e.printStackTrace();
                Log.e(TAG, "Publish ERROR: "+e.getMessage());
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

        //=================== END MQTT =====================//

    }

    private void showNotification(@NonNull String detail){
        //App icon
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon_normal);
        //Create intent click without new create activity
        //Thank: https://stackoverflow.com/a/48443809
        Intent intent = new Intent(this, ModbusActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pi = PendingIntent.getActivity(this, NOTIFY_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
        //Set notification
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLights(Color.BLUE, 1000, 2000)
                .setSmallIcon(R.drawable.tcp_icon)
                        .setLargeIcon(bitmap)
                .setContentTitle(getString(R.string.modbus_tcp))
                .setContentText(detail)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .build();

        //Set NotificationManager instance
        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager!=null){
            //Check SDK for notification on Android 8.0 (Oreo)
            //Thank: https://stackoverflow.com/questions/43093260/notification-not-showing-in-oreo
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                //Create Channel
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
                manager.createNotificationChannel(channel);
            }
            //Clear previously
            manager.cancel(NOTIFY_CODE);
            //Show new Notification
            manager.notify(NOTIFY_CODE, n);
            Log.w(TAG, "Notify created..."+NOTIFY_CODE);
        }

    }

    private void clearNotification(){
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager!=null){ manager.cancelAll(); }
    }






}
