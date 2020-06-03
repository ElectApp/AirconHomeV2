package com.apyeng.airconhomev2;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeListActivity extends AppCompatActivity {

    private int userId;
    private UserItem userItem;
    private HomeItemAdapter homeItemAdapter;
    private SelectableRoundedImageView userImg;
    private HomeManager homeManager;
    private RecyclerView homeRv;
    private FrameLayout welcomeLay;
    private MqttAndroidClient mqtt;
    private int countSubscribed;
    private boolean normalFlag;  //Set true when normal
    private SwipeRefreshLayout swipeRefresh;
    private static final int HOME_REQUEST = 1440, BUILD_REQUEST = 1444, JOIN_REQUEST = 1511,
            EDIT_USER_REQUEST = 1452, EDIT_HOME_REQUEST = 2034;
    private static final String DEVICE_KEY[] = new String[]{ Constant.AC, Constant.PV };
    private static final String TAG = "HomeListActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_list);

        //Get intent data
        userId = getIntent().getIntExtra(Constant.USER_ID, 0);
        if (userId<=0){
            throw new IllegalArgumentException("Must pass user id...");
        }

        //Declare object on layout
        userImg = findViewById(R.id.user_img);
        welcomeLay = findViewById(R.id.welcome_content);
        homeRv = findViewById(R.id.home_rv);
        NestedScrollView scrollView = findViewById(R.id.scroll_view);
        ImageTextButtonWidget joinBtn = findViewById(R.id.join_btn);
        ImageTextButtonWidget buildBtn = findViewById(R.id.build_btn);

        //Initial state
        showWelcome(false);

        //Initial list
        LinearLayoutManager rvManager = new LinearLayoutManager(this);
        homeItemAdapter = new HomeItemAdapter(this, new ArrayList<HomeItem>());
        homeItemAdapter.setClickItemListener(new HomeItemAdapter.OnClickHomeItemListener() {
            @Override
            public void onClick(View view, int position, HomeItem homeItem) {
                Log.w(TAG, "Click on "+position);
                //Disconnect MQTT
                disconnectMQTTServer();
                //Set data from intent
                Intent intent = new Intent(HomeListActivity.this, HomeActivity.class);
                intent.putExtra(Constant.USER_ID, userId);
                intent.putExtra(Constant.HOME_DATA, homeItem);
                //startActivity(intent);
                startActivityForResult(intent, HOME_REQUEST);

            }
        });
        homeItemAdapter.setClickSettingListener(new HomeItemAdapter.OnClickHomeItemListener() {
            @Override
            public void onClick(View view, int position, HomeItem homeItem) {
                Log.w(TAG, "Click Setting on "+position);
                //Show
                showMenu(homeItem);

            }
        });
        homeRv.setLayoutManager(rvManager);
        homeRv.setAdapter(homeItemAdapter);
        //Set RecyclerView smooth scrolling with NestScrollView
        //Thank: https://android.jlelse.eu/recyclerview-within-nestedscrollview-scrolling-issue-3180b5ad2542
        ViewCompat.setNestedScrollingEnabled(homeRv, false);
        //Add listener when scroll changed
        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                //Check scroll view state
                if (scrollY > oldScrollY) {
                    Log.w(TAG, "Scroll DOWN");
                }
                if (scrollY < oldScrollY) {
                    Log.w(TAG, "Scroll UP");
                }

                if (scrollY == 0) {
                    Log.w(TAG, "TOP SCROLL");
                }

                if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                    Log.w(TAG, "BOTTOM SCROLL");
                    //Read device id list when scroll end


                }
            }
        });

        //User img click
        userImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Go edit profile
                goEditUserProfile();
            }
        });

        //Join button
        joinBtn.setOnWidgetClickListener(new ImageTextButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Join Membership
                intentJoinActivity();
            }
        });

        //Build button
        buildBtn.setOnWidgetClickListener(new ImageTextButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Build new home
                intentAddDeviceActivity();
            }
        });

        //Set MQTT client
        //Thank: https://www.hivemq.com/blog/mqtt-client-library-enyclopedia-paho-android-service/
        final String id = MqttClient.generateClientId();
        mqtt = new MqttAndroidClient(this, Constant.MQTT_URL, id);
        //Add listener when MQTT active
        mqtt.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w(TAG, "Reconnect MQTT server: "+reconnect);
                //Re-subscribe
                if (reconnect){ subscribeGroup(homeItemAdapter.getItems(), true); }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.e(TAG, "Connect MQTT server lost...");
                //Show connection lost on toolbar below
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Update home item
                if (topic!=null && message!=null){ setHomeItem(topic, message); }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.w(TAG, "MQTT Sent complete...");
            }
        });

        //Refresh
        swipeRefresh = findViewById(R.id.refresh_container);
        //Enable
        swipeRefresh.setEnabled(true);
        // Configure the refreshing colors
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        //Listener
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Clear list
                homeItemAdapter.clearList();
                //Try login with id
                tryLoginWithUserId();
            }
        });

        //Try login with id
        tryLoginWithUserId();

    }

    @Override
    protected void onStop() {
        //disconnectMQTTServer();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.w(TAG, "onActivityResult: ["+requestCode+"] "+resultCode);
        switch (requestCode){
            case JOIN_REQUEST: //Check result
                //Update home list
                if (resultCode==RESULT_OK && data!=null){
                    HomeItem homeItem = data.getParcelableExtra(Constant.HOME_DATA);
                    if (homeItem!=null){
                        //Hide welcome
                        showWelcome(false);
                        //Add home list
                        homeItemAdapter.add(homeItem);
                        //Check connection
                        if (!mqtt.isConnected()){
                            //Connect and subscribe
                            connectMQTTServer(homeItemAdapter.getItems());
                        }else {
                            //Subscribe
                            subscribeGroup(homeItem, true);
                        }
                    }
                }
                break;
            case BUILD_REQUEST:
                //Update home list
                if (resultCode==RESULT_OK && data!=null){
                    int groupId = data.getIntExtra(Constant.GROUP_ID, 0);
                    if (groupId>0){
                        tryReadDeviceId(new int[]{ groupId }, false, true);
                    }
                }
                break;
            case EDIT_HOME_REQUEST:
                if (resultCode==RESULT_OK && data!=null){
                    HomeItem homeItem = data.getParcelableExtra(Constant.HOME_DATA);
                    if (homeItem!=null){
                        //Update home
                        int p = homeItemAdapter.getPositionOf(homeItem.getGroupId());
                        Bitmap bitmap = Function.getBitmap(data.getByteArrayExtra(Constant.PROFILE_IMG));
                        if (bitmap!=null){ homeItem.setImageDownloaded(bitmap); }
                        homeItemAdapter.setItems(p, homeItem);
                    }
                }
                break;
            case EDIT_USER_REQUEST:
                if (resultCode==RESULT_OK && data!=null){
                    UserItem item = data.getParcelableExtra(Constant.USER_DATA);
                    if (item!=null){
                        //Check image
                        Bitmap bitmap = Function.getBitmap(data.getByteArrayExtra(Constant.PROFILE_IMG));
                        if (bitmap!=null){
                            item.setImageDownloaded(bitmap); //Update item
                            userImg.setImageBitmap(bitmap); //Update image
                        }
                        //Save user data
                        userItem = item;
                    }
                }
                break;
            case HOME_REQUEST:
                //Re-connect
                if (!mqtt.isConnected()){ connectMQTTServer(homeItemAdapter.getItems()); }
                break;
        }

    }


    private void showMenu(final HomeItem homeItem){
        //Set data
        Bundle bundle = new Bundle();
        bundle.putInt(Constant.GROUP_ID, homeItem.getGroupId());
        //Create
        SheetDialogFragment dialogFragment = new SheetDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(getSupportFragmentManager(), SheetDialogFragment.TAG);
        //Add listener
        dialogFragment.setListener(new SheetItemAdapter.ItemListener() {
            @Override
            public void onItemClick(int id, int numberSelected, SheetItem itemSelected) {
                Log.w(TAG, "Selected..."+numberSelected);
                switch (numberSelected){
                    case 0: //Edit profile
                        goEditHomeProfile(homeItem);
                        //Log.w(TAG, "Registered time..."+homeItem.getRegisteredTime());
                        break;
                    case 1: //Leave Home
                        //Show confirm dialog
                        showConfirmLeaveDialog(homeItem.getGroupId());
                        break;
                }



            }
        });
    }

    private void showConfirmLeaveDialog(final int groupId){
        Function.showAlertDialog(this, R.string.leave_home, R.string.confirm_leave,
                R.string.confirm, new MyAlertDialog.OnButtonClickListener() {
                    @Override
                    public void onClose() {

                    }

                    @Override
                    public void onAction(String data, String password) {
                        Function.dismissDialogFragment(HomeListActivity.this, MyAlertDialog.TAG);
                        //Do
                        homeManager.leaveGroup(groupId, userId, new HomeManager.OnSetHomeListener() {
                            @Override
                            public void onSuccess(int groupId) {
                                //Remove adapter
                                int p = homeItemAdapter.getPositionOf(groupId);
                                homeItemAdapter.deleteItem(p);
                                //Show left
                                Function.showToast(getApplicationContext(), R.string.left);
                            }

                            @Override
                            public void onFailed(String error) {
                                //Show error
                                Function.showToast(getApplicationContext(), error);
                            }
                        });
                    }
                });

    }

    private void showWelcome(boolean show){
        if (show){
            //Show Welcome content
            welcomeLay.setVisibility(View.VISIBLE);
            homeRv.setVisibility(View.GONE);
            //Animation
            ImageView img = findViewById(R.id.welcome_img);
            img.setBackgroundResource(R.drawable.welcome_animation);
            AnimationDrawable anim = (AnimationDrawable) img.getBackground();
            anim.start();
            //Title
            TextView title = findViewById(R.id.welcome_title);
            String t = getString(R.string.welcome_title)+" "+userItem.getUsername();
            title.setText(t);
        }else {
            //Show list loading
            homeRv.setVisibility(View.VISIBLE);
            welcomeLay.setVisibility(View.GONE);
        }
    }


    private void tryLoginWithUserId(){
        Log.w(TAG, "Try login and read group id by "+userId);
        if (Function.internetConnected(this)){
            if (!normalFlag){ //Show loading dialog
                homeManager = new HomeManager(this, this);
            }else { //not show loading dialog
                homeManager = new HomeManager(this);
            }
            homeManager.signInWithUserId(userId, Constant.SIGN_IN_FLAG, new HomeManager.OnSignInListener() {
                @Override
                public void onSuccess(UserItem userData, int[] groupId) {
                    Log.w(TAG, "Success: "+ Arrays.toString(groupId));
                    //Set user data
                    userItem = userData;
                    //Update image
                    String img = userItem.getProfileImg();
                    if (img!=null && !img.isEmpty()){
                        downloadUserImg();
                    }else {
                        userImg.setImageResource(R.drawable.user_icon);
                    }
                    //check group id
                    if (groupId!=null){
                        //Continue read home detail
                        tryReadDeviceId(groupId, true, false);
                    }else {
                        //Hide refresh
                        if(swipeRefresh.isRefreshing()){ swipeRefresh.setRefreshing(false); }
                        //Show welcome content
                        showWelcome(true);
                        normalFlag = false;
                    }
                }

                @Override
                public void onFailed(String error) {
                    Log.e(TAG, "Failed: "+error);
                    Function.showNoResultDialog(HomeListActivity.this, errorButtonListener);
                    normalFlag = false;
                }
            });

        }else {
            Function.showNoInternetDialog(this, errorButtonListener);
        }
    }

    private void tryReadDeviceId(int groupId[], final boolean firstTime, final boolean insertTop){
        homeManager.readDeviceId(groupId, new HomeManager.OnReadDeviceIdListener() {
            @Override
            public void onSuccess(List<HomeItem> homeItems) {
                normalFlag = true;
                Log.w(TAG, "Found home total: "+homeItems.size());
                //Hide refresh icon
                if(swipeRefresh.isRefreshing()){ swipeRefresh.setRefreshing(false); }
                //Set home item
                if (homeItems.size()>0){
                    //Show list
                    showWelcome(false);
                    //Update home list
                    homeItemAdapter.addList(homeItems, insertTop);
                    //Connect MQTT server
                    if (!mqtt.isConnected()){
                        //Connect and subscribe
                        connectMQTTServer(homeItemAdapter.getItems());
                    }else {
                        //Subscribe
                        subscribeGroup(homeItems, true);
                    }
                }else {
                    //Show welcome content
                    showWelcome(firstTime);
                    normalFlag = false;
                }
            }

            @Override
            public void onFailed(String error) {
                normalFlag = false;
                Function.showNoResultDialog(HomeListActivity.this, errorButtonListener);
            }
        });
    }

    private AppErrorDialog.OnClickActionButtonListener
            errorButtonListener = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            Log.w(TAG, "Error Button Clicked "+titleId);
            if (buttonId==R.string.retry){
                Function.dismissAppErrorDialog(HomeListActivity.this);
                //Clear list
                homeItemAdapter.clearList();
                //Try again
                tryLoginWithUserId();
            }
        }
    };

    private void setHomeItem(@NonNull String topic, @NonNull MqttMessage message){
        Log.w(TAG, "MQTT Message arrived ["+topic+"] "+message.toString());
        String s[] = topic.split("/");
        String v[] = message.toString().split(",");
        //Device updated topic: <group-id>/<device-id>/<key>
        if (s.length==3) {
            int groupId = Function.getInt(s[0].trim());
            int deviceId = Function.getInt(s[1].trim());
            int p = homeItemAdapter.getPositionOf(groupId, deviceId);
            //Check device updated
            switch (s[2]) {
                case Constant.AC:
                    //Check member and length of array
                    if (v.length >= 3 && p > -1) {
                        //Get value: x10
                        int value = Function.getInt(v[2].trim());
                        //Get old item
                        HomeItem homeItem = homeItemAdapter.getItems().get(p);
                        SparseIntArray power = homeItem.getConsumptionEachId();
                        //Update list
                        power.put(deviceId, value > -1 ? value : 0);
                        homeItem.setConsumptionEachId(power);
                        //Update adapter
                        homeItemAdapter.notifyDataSetChanged();
                        Log.w(TAG, "Update AC Power " + value + " to " + homeItem.getName());
                    }
                    break;
                case Constant.PV:
                    //Check member and length of array
                    if (v.length >= 3 && p > -1) {
                        //Get value
                        int value = Function.getInt(v[2].trim());
                        //Get old item
                        HomeItem homeItem = homeItemAdapter.getItems().get(p);
                        SparseIntArray power = homeItem.getSavingEachId();
                        //Update list
                        power.put(deviceId, value > -1 ? value : 0);
                        homeItem.setSavingEachId(power);
                        //Update adapter
                        homeItemAdapter.notifyDataSetChanged();
                        Log.w(TAG, "Update PV Power " + value + " to " + homeItem.getName());
                    }
                    break;
            }

        }

        //Group updated topic: <group-id>/<key>
        if (s.length==2){
            //Check item list updated
            int groupId = Function.getInt(s[0].trim());
            int deviceId = Function.getInt(v[0].trim());
            int p = homeItemAdapter.getPositionOf(groupId, deviceId);
            //Check key
            switch (s[1]){
                case Constant.DEVICE_ADDED:
                    if (p<0){
                        //Add new device to item
                        int gP = homeItemAdapter.getPositionOf(groupId);
                        if (gP>-1){
                            //Get old value
                            HomeItem old = homeItemAdapter.getItems().get(gP);
                            int oldId[] = old.getDeviceId();
                            if (oldId==null){ oldId = new int[0]; }
                            int oldSize = oldId.length;
                            //Update
                            oldId = Arrays.copyOf(oldId, oldSize+1);
                            oldId[oldSize] = deviceId;
                            old.setDeviceId(oldId);
                            homeItemAdapter.setItems(gP, old);
                            Log.w(TAG, "Add deviceId "+deviceId+" to "+old.getName());
                            //Add subscribe
                            subscribeDevice(groupId, new int[]{deviceId}, true);
                        }
                    }
                    break;
                case Constant.DEVICE_DELETED:
                    if (p>-1){
                        Log.w(TAG, "Delete ID: "+deviceId);
                        //Get old value
                        HomeItem old = homeItemAdapter.getItems().get(p);
                        int oldId[] = old.getDeviceId();
                        for (int a=0; a<oldId.length; a++){
                            //Find index
                            if (oldId[a]==deviceId){
                                //Replace position of deviceId deleted by next Id
                                for (int i=a; i<oldId.length; i++){
                                    int next = i+1;
                                    if (next<oldId.length){ oldId[i] = oldId[next]; }
                                }
                                //Reduce size
                                oldId = Arrays.copyOf(oldId, oldId.length-1);
                                //Update array
                                old.setDeviceId(oldId);
                                //Remove key on Power Consumption
                                SparseIntArray power = old.getConsumptionEachId();
                                power.delete(a);
                                old.setConsumptionEachId(power);
                                //Remove key on Power Saving
                                power = old.getSavingEachId();
                                power.delete(a);
                                old.setSavingEachId(power);
                                //Update adapter
                                homeItemAdapter.setItems(p, old);
                                Log.w(TAG, "Delete deviceId "+deviceId+" from "+old.getName());
                                //Unsubscribe
                                subscribeDevice(groupId, new int[]{deviceId}, false);
                                break;
                            }
                        }
                    }
                    break;
                case Constant.GROUP_DELETED:
                    int gP = homeItemAdapter.getPositionOf(groupId);
                    if (gP>-1){
                        //Unsubscribe
                        HomeItem homeItem = homeItemAdapter.getItems().get(gP);
                        subscribeGroup(homeItem, false);
                        //Delete in list
                        homeItemAdapter.deleteItem(gP);
                        Log.w(TAG, "Delete groupId "+groupId+" from home list");
                    }
                    break;
            }
        }
    }

    private void connectMQTTServer(final List<HomeItem> homeItems){
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
                    subscribeGroup(homeItems, true);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Connection failed: "+exception.getMessage());
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }

    }

    private void subscribeGroup(@NonNull final List<HomeItem> homeItems, boolean subscribe){
        //Subscribe
        for (HomeItem item : homeItems){
            subscribeGroup(item, subscribe);
        }
    }

    private void subscribeGroup(HomeItem homeItem, boolean subscribe){
        int groupId = homeItem.getGroupId();
        //Subscribe group topic: <group-id>/<key>
        String head = String.valueOf(groupId)+"/";
        for (String g : Constant.GROUP_KEY){
            String topic = head + g;
            setSubscribe(topic, subscribe);
        }
        //Subscribe device
        subscribeDevice(groupId, homeItem.getDeviceId(), subscribe);
    }

    private void subscribeDevice(int groupId, int deviceId[], boolean subscribe){
        //Check device
        if (deviceId==null || deviceId.length<1){ return; }
        //Subscribe device: <group-id>/<device-id>/<key>
        String head = String.valueOf(groupId)+"/";
        for (String d : DEVICE_KEY){
            for (int id : deviceId){
                String topic = head + String.valueOf(id) + "/" + d;
                setSubscribe(topic, subscribe);
            }
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
                        if (countSubscribed==0){ disconnectMQTTServer(); }
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Subscribe or Unsubscribe failed!");
                }
            });
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    private void disconnectMQTTServer(){
        Log.w(TAG, "Try disconnect MQTT server..."+mqtt);
        if (mqtt!=null && mqtt.isConnected()){
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

    }


    private void intentAddDeviceActivity(){
        Intent intent = new Intent(this, AddDeviceActivity.class);
        intent.putExtra(Constant.USER_ID, userId);
        //startActivity(intent);
        startActivityForResult(intent, BUILD_REQUEST);
    }


    private void intentJoinActivity(){
        Intent intent = new Intent(this, JoinActivity.class);
        intent.putExtra(Constant.USER_ID, userId);
        intent.putExtra(Constant.GROUP_ID, getGroupId());
        startActivityForResult(intent, JOIN_REQUEST);
        //startActivity(intent);
    }

    private int[] getGroupId(){
        List<HomeItem> homeItems = homeItemAdapter.getItems();
        if (homeItems!=null && homeItems.size()>0){
            int id[] = new int[homeItems.size()];
            for (int i=0; i<id.length; i++){
                id[i] = homeItems.get(i).getGroupId();
            }
            return id;
        }
        return null;
    }

    private void downloadUserImg(){
        VolleyImageLoader loader = new VolleyImageLoader(this);
        loader.download(Function.getUserImageUrl(userItem.getProfileImg()), 0, 0,
                new VolleyImageLoader.OnLoadingListener() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        //Update image and save file
                        userImg.setImageBitmap(bitmap);
                        userItem.setImageDownloaded(bitmap);
                    }

                    @Override
                    public void onFailed(String error) {
                        Log.e(TAG, "Load image failed: "+error);
                        userImg.setImageResource(R.drawable.user_icon);
                        //userItem.setImageDownloaded(null);
                    }
                });
    }

    private void goEditHomeProfile(HomeItem homeItem){
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra(Constant.USER_ID, userId);
        intent.putExtra(Constant.HOME_DATA, homeItem);
        Bitmap bitmap = homeItem.getImageDownloaded();
        intent.putExtra(Constant.PROFILE_IMG, bitmap!=null? Function.getByteArray(bitmap) : null);
        startActivityForResult(intent, EDIT_HOME_REQUEST);
        //startActivity(intent);
    }

    private void goEditUserProfile(){
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra(Constant.USER_DATA, userItem);
        //Pass bitmap to another activity that should use this method
        //Due to sometime the Parcelable on UserItem render system bug!
        //Thank: https://jayrambhia.com/blog/pass-activity-bitmap
        Bitmap bitmap = userItem.getImageDownloaded();
        intent.putExtra(Constant.PROFILE_IMG, bitmap!=null? Function.getByteArray(bitmap) : null);
        startActivityForResult(intent, EDIT_USER_REQUEST);
        //startActivity(intent);
    }



}
