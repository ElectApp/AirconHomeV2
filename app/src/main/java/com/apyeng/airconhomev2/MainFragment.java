package com.apyeng.airconhomev2;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment {

    private int cLabelId;
    private TextView labelTxt;
    private ArrayList<SheetItem> labelItems;
    private AirItemAdapter airItemAdapter;
    private int groupId, userId;
    private MqttAndroidClient mqtt;
    private int countSubscribed;
    private View sectionView[];
    private TextView numAC;
    private Context context;
    private Activity activity;
    private TextView faultTxt;
    private boolean disconnectFlag;
    private static final int PROGRESS = 0, CONTENT = 1, NO_AC = 2;
    public static final int AIR_ITEM_WIDTH_MIN_DP = 228;   //Min width of air item layout (dp unit)
    private static final int RENAME_CODE = 1725, CHANGE_LABEL_CODE = 1842;
    public static final String TAG = "MainFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get data
        Bundle bundle = getArguments();
        groupId = bundle.getInt(Constant.GROUP_ID, 0);
        userId = bundle.getInt(Constant.GROUP_ID, 0);

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
                    //Re-subscribe
                    //subscribeGroup();
                    subscribeDevice(airItemAdapter.getItems());
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //Show no internet
                if (!disconnectFlag){ faultTxt.setVisibility(View.VISIBLE); }

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
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
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        numAC = view.findViewById(R.id.total_list);
        faultTxt = view.findViewById(R.id.no_internet_txt);
        RecyclerView airRv = view.findViewById(R.id.ac_rv);

        sectionView = new View[3];
        sectionView[0] = view.findViewById(R.id.circle_progress);
        sectionView[1] = view.findViewById(R.id.container);;
        sectionView[2] = view.findViewById(R.id.no_ac_content);

        //Initial hide content
        showContent(PROGRESS);

        //Label
        labelTxt = view.findViewById(R.id.tv_label);
        labelItems = new ArrayList<>();
        view.findViewById(R.id.label_lay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLabelList();
            }
        });

        //Spacer
        ItemOffsetDecoration decoration = new ItemOffsetDecoration(context, R.dimen.item_offset);
        //Initial air item
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context,
                Function.getGirdColumn(context, activity, AIR_ITEM_WIDTH_MIN_DP),
                GridLayoutManager.VERTICAL, false);
        airRv.setLayoutManager(gridLayoutManager);
        airItemAdapter = new AirItemAdapter(context, new ArrayList<AirItem>());
        airItemAdapter.setAirItemClickListener(new AirItemAdapter.OnAirItemClickListener() {
            @Override
            public void onClick(View view, int position, AirItem airItem) {
                Log.w(TAG, "Click AC ["+position+"] "+airItem.isOnline());
                Indoor indoor = airItem.getIndoor();
                Log.w(TAG, "Indoor data: "+indoor);

                //Set intent data and start ControlActivity
                if (airItem.isOnline() && indoor!=null){
                    //Can't pass only AirItem due to at ControlActivity Indoor always null
                    Intent intent = new Intent(context, ControlActivity.class);
                    intent.putExtra(Constant.AC_DATA, airItem);
                    intent.putExtra(Constant.INDOOR_DATA, indoor);
                    intent.putExtra(Constant.GROUP_ID, groupId);
                    intent.putExtra(Constant.TIME_ON, indoor.onTime);
                    intent.putExtra(Constant.TIME_OFF, indoor.offTime);
                    startActivity(intent);
                }

            }
        });
        airItemAdapter.setAirItemLongClickListener(new AirItemAdapter.OnAirItemClickListener() {
            @Override
            public void onClick(View view, int position, AirItem airItem) {
                //Show settings dialog
                showSettingDialog(position, airItem);
            }
        });
        airRv.setAdapter(airItemAdapter);
        //Set space
        airRv.addItemDecoration(decoration);
        //Set RecyclerView smooth scrolling with NestScrollView
        ViewCompat.setNestedScrollingEnabled(airRv, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.w(TAG, "View is created...");

        //Read Label
        tryReadLabelDetail();

    }

    @Override
    public void onStart() {
        super.onStart();
        //Read device
        //tryReadDeviceDetail();
    }

    @Override
    public void onStop() {
        //Disconnect
        //disconnectMQTTServer();
        super.onStop();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w(TAG, "onActivityResult...["+requestCode+"] "+resultCode);

        switch (requestCode){
            case RENAME_CODE:
                //Update item
                if(resultCode==RESULT_OK && data!=null){
                    int p = data.getIntExtra(Constant.POSITION_ID, -1);
                    AirItem airItem = data.getParcelableExtra(Constant.AC_DATA);
                    if(airItem!=null && p>-1){
                        airItemAdapter.setItems(p, airItem);
                        airItemAdapter.notifyItemChanged(p);
                        //Publish to rename and save on device
                        publish(airItem.getDeviceId(), new String[]{Indoor.SET_NICKNAME,
                                airItem.getNickname()});
                    }
                }
                break;
            case CHANGE_LABEL_CODE:
                if (resultCode==RESULT_OK){
                    //Reload
                    tryReadLabelDetail();
                }
                break;
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //This function work before onCreate()
        //Thank: https://stackoverflow.com/questions/8215308/using-context-in-a-fragment
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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


    private void setNumAC(){
        int total = airItemAdapter.getSize();
        String txt1 = "A/C ("+total+"), ";
        int n = airItemAdapter.getOnline();
        String txt2 = n>0? n+" "+context.getString(R.string.online)
                : context.getString(R.string.offline);
        String txt = txt1 + txt2;
        numAC.setText(txt);
        if(total==0){ showContent(NO_AC); }
    }

    private void setAirItem(@NonNull String topic, @NonNull MqttMessage message){
        Log.w(TAG, "New Message ["+topic+"] "+message.toString());
        //Check topic
        String s[] = topic.split("/");
        int group = Function.getInt(s[0].trim());
        if (group!=groupId){
            return;
        }
        Log.w(TAG, "Update...");
        //Update device
        if (s.length==3){
            switch (s[2]){
                case Constant.STATE: //Update air item
                    int v[] = Function.getIntArray(message.toString());
                    if (v.length>0){
                        int device = Function.getInt(s[1]);
                        int p = airItemAdapter.getPositionOf(device);
                        if (p>-1){
                            //Found item
                            AirItem old = airItemAdapter.getAirItem(p);
                            String err = null; //MB Error
                            Indoor indoor = null; //is offline
                            //Message same request MB_INFO tag (Single Control)
                            if (v.length>=Indoor.DATA_SIZE(Indoor.MB_INFO)){
                                Time on = new Time(v[2], v[3]);
                                Time off = new Time(v[4], v[5]);
                                indoor = new Indoor(v[0], v[1], on, off, v[6], v[7]);
                            }else {
                                err = message.toString();
                                if (err.endsWith(",")){ err = err.substring(0, err.length()-1); }
                            }
                            old.setError(err);
                            old.setIndoor(indoor);
                            airItemAdapter.setItems(p, old);
                            Log.w(TAG, "AirItem ID "+device+" is updated "+old.isOnline());
                        }
                    }
                    break;
            }
        }

        //Update number of list
        if (s.length==2){
            String v[] = message.toString().split(",");
            if (v.length<1){ return; }
            int deviceId = Function.getInt(v[0]);
            switch (s[1]){
                case Constant.DEVICE_CHANGED:
                    if (v.length>=2){
                        int p = airItemAdapter.getPositionOf(deviceId);
                        if (p>-1){
                            AirItem old = airItemAdapter.getAirItem(p);
                            old.setNickname(v[1]);
                            airItemAdapter.setItems(p, old);
                        }
                    }
                    break;
                case Constant.DEVICE_ADDED:
                    if (v.length>=3){
                        Log.w(TAG, "Add item ID: "+deviceId);
                        //Add item
                        airItemAdapter.addAirItems(new AirItem(deviceId, v[1], v[2], null));
                        //Subscribe
                        subscribeDevice(deviceId, true);
                    }
                    break;
                case Constant.DEVICE_DELETED:
                    Log.w(TAG, "Delete item ID: "+deviceId);
                    //Delete item on list
                    airItemAdapter.deleteItemID(deviceId);
                    //Unsubscribe
                    subscribeDevice(deviceId, false);
                    break;
            }
        }

        //Update total
        setNumAC();
    }

    private void tryReadLabelDetail(){
        if (Function.internetConnected(context)){
            HomeManager homeManager = new HomeManager(context);
            homeManager.readLabelDetail(groupId, new HomeManager.OnReadLabelCallback() {
                @Override
                public void onSuccess(List<LabelItem> items) {
                    Log.w(TAG, "OnSuccess...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(activity);
                    //Update label
                    labelItems.clear();
                    labelItems.add(new SheetItem(R.drawable.home_icon, "All", 0));
                    for (LabelItem label : items){
                        labelItems.add(new SheetItem(R.drawable.ic_tag, label.getText(), label.getId()));
                    }
                    labelItems.add(new SheetItem(R.drawable.ic_settings2, "Manage Label", -1));
                    //Label and load device list
                    setCurrentLabel(labelItems.get(0));
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showDBErrorDialog(activity, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(activity, errorListener);
        }
    }

    private void setCurrentLabel(SheetItem sheetItem){
        //Label
        cLabelId = sheetItem.getItemId();
        labelTxt.setText(sheetItem.getName());
        //Load devie list
        tryReadDeviceDetail();
    }

    private void tryReadDeviceDetail(){
        if (Function.internetConnected(context)){
            HomeManager homeManager = new HomeManager(context);
            homeManager.readFullDeviceDetail(groupId, cLabelId, new HomeManager.OnReadFullDeviceDetailListener() {
                @Override
                public void onSuccess(List<AirItem> airItems) {
                    Log.w(TAG, "OnSuccess...");
                    //Hide dialog
                    Function.dismissAppErrorDialog(activity);
                    //Clear
                    airItemAdapter.clearAll();
                    //Initial air item list
                    if (airItems!=null){
                        airItemAdapter.addAirItems(airItems);
                    }
                    if (!mqtt.isConnected()){
                        //Connect
                        connectMQTTServer();
                    }else {
                        //Subscribe group
                        subscribeGroup();
                        //Subscribe device
                        subscribeDevice(airItemAdapter.getItems());
                    }
                    //Update total
                    setNumAC();
                }

                @Override
                public void onFailed(String error) {
                    Log.w(TAG, "OnFailed..."+error);
                    Function.showDBErrorDialog(activity, error, errorListener);
                }
            });
        }else {
            Function.showNoInternetDialog(activity, errorListener);
        }
    }

    private AppErrorDialog.OnClickActionButtonListener errorListener
            = new AppErrorDialog.OnClickActionButtonListener() {
        @Override
        public void onClick(View view, int titleId, int buttonId) {
            tryReadLabelDetail();
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
                    //Subscribe group
                    subscribeGroup();
                    //Subscribe device
                    subscribeDevice(airItemAdapter.getItems());
                    //check item
                    if (airItemAdapter.getSize()<1){
                        showContent(NO_AC);
                    }else {
                        showContent(CONTENT);
                    }
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
        Log.w(TAG, "Try disconnect MQTT server...");
        disconnectFlag = true;
        countSubscribed = 0; //Clear
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

    private void subscribeDevice(@NonNull final List<AirItem> airItems){
        //Subscribe
        for (AirItem item : airItems){
            subscribeDevice(item.getDeviceId(), true);
        }
    }

    private void subscribeDevice(int deviceId, final boolean subscribe){
        //Subscribe device: <group-id>/<device-id>/<key>
        String topic = String.valueOf(groupId)+"/" + String.valueOf(deviceId) + "/" + Constant.STATE;
        setSubscribe(topic, subscribe);
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

    private void publish(int deviceId, String[] details){
        String topic = String.valueOf(groupId)+"/"+deviceId+"/"+Constant.REQUEST;
        String d = "";
        for (String v : details){
            d = d.concat(v);
            d = d.concat(",");
        }
        Log.w(TAG, "Publish: ["+topic+"] "+d);
        setPublish(topic, d);
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

    private float convertPixelToDP(int px){
        //Convert DP to Pixel
        //Thank: https://developer.android.com/training/multiscreen/screendensities#java
        float scale = getResources().getDisplayMetrics().density;
        float dp = (px-0.5f)/scale;
        Log.w(TAG, "Covert "+px+" px = "+dp+" DP by scale = "+scale);
        return dp;
    }

    private int[] getScreenDetail(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int h = displayMetrics.heightPixels;
        int w = displayMetrics.widthPixels;
        Log.w(TAG, "Screen W: "+w+", H: "+h);
        return new int[]{ w, h };
    }

    //minDP = Min width of layout
    private int getGirdColumn(){
        int display[] = getScreenDetail();
        float n = convertPixelToDP(display[0])/AIR_ITEM_WIDTH_MIN_DP;
        Log.w(TAG, "N: "+n);
        if (n<1.0f){ return 1; }
        //EX. n = 1.8 after cast int => 1, So check n.x > 0.5 if true return n+1
        float d = n - (int)n;
        if (d>0.5f){ n += 1.0f; }
        Log.w(TAG, "W: "+display[0]+", n: "+n);
        return (int)n;
    }

    private void showContent(int n){
        for (int i=0; i<sectionView.length; i++){
            sectionView[i].setVisibility(i==n? View.VISIBLE : View.GONE);
        }

    }

    public void pauseService(boolean pause){
        if (pause){
            if (mqtt.isConnected()) { disconnectMQTTServer(); }
        }else {
            if (!mqtt.isConnected()) { connectMQTTServer(); }
        }
    }

    private void showSettingDialog(final int p, final AirItem airItem){
        //Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(airItem.getNickname()+" | "+airItem.getActualName());
        //Add list
        final String[] settings = getResources().getStringArray(R.array.ac_setting_list);
        builder.setItems(settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Rename
                        Intent intent = new Intent(context, RenameACActivity.class);
                        intent.putExtra(Constant.POSITION_ID, p);
                        intent.putExtra(Constant.GROUP_ID, groupId);
                        intent.putExtra(Constant.AC_DATA, airItem);
                        startActivityForResult(intent, RENAME_CODE);
                        break;
                    case 1: // Delete
                        showConfirmDeleteDialog(airItem);
                        break;
                }
            }
        });
        // Create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showConfirmDeleteDialog(final AirItem airItem){
        String detail = context.getResources().getString(R.string.confirm_delete_ac);
        detail += " \""+airItem.getNickname()+"\"";
        Function.showAlertDialog(activity, R.string.delete_ac, detail,
                R.string.confirm, new MyAlertDialog.OnButtonClickListener() {
                    @Override
                    public void onClose() {

                    }

                    @Override
                    public void onAction(String data, String password) {
                        Function.dismissDialogFragment(activity, MyAlertDialog.TAG);
                        //Do
                        String sql = "DELETE FROM device_data WHERE device_id="+airItem.getDeviceId();
                        HomeManager manager = new HomeManager(context);
                        manager.insertUpdateAnyGroupTable(groupId, sql, new HomeManager.OnSingleStringCallback() {
                            @Override
                            public void onSuccess(String value) {
                                Log.w(TAG, "AC deleted...");
                                Function.showToast(context, R.string.deleted);
                                //Publish for switch mode from ST to AP
                                publish(airItem.getDeviceId(), new String[]{Indoor.SWITCH_MODE, "AP"});
                                //Update list
                                airItemAdapter.deleteItemID(airItem.getDeviceId());
                                //Update total
                                setNumAC();
                            }

                            @Override
                            public void onFailed(String error) {
                                Log.e(TAG, "Delete error: "+error);
                                Function.showToast(context, R.string.no_result);
                            }
                        });
                    }
                });

    }

    private void showLabelList(){
        //Layout
        View view = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_layout, null);
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(view);
        RecyclerView rv = view.findViewById(R.id.menu_rv);
        //Add to list
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(context));
        SheetItemAdapter adapter = new SheetItemAdapter(groupId, labelItems, new SheetItemAdapter.ItemListener() {
            @Override
            public void onItemClick(int id, int numberSelected, SheetItem itemSelected) {
                //Hide
                dialog.dismiss();
                Log.w(TAG, "Load label..."+id+", "+numberSelected+", "+itemSelected.getItemId());
                //Type
                if (itemSelected.getItemId()<0){
                    //Setting
                    Log.w(TAG, "Manage label");
                    Intent intent = new Intent(context, ManageLabelActivity.class);
                    intent.putExtra(Constant.GROUP_ID, groupId);
                    intent.putExtra(Constant.USER_ID, userId);
                    startActivityForResult(intent, CHANGE_LABEL_CODE);
                }else {
                    //Reload
                    setCurrentLabel(itemSelected);
                }
            }
        });
        rv.setAdapter(adapter);
        //Behavior and show
        BottomSheetBehavior.from((View)view.getParent());
        dialog.show();
    }

}
