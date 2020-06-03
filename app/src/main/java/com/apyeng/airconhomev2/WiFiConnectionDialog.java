package com.apyeng.airconhomev2;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

//Support android version < 10
public class WiFiConnectionDialog extends BottomSheetDialogFragment {

    //Layout
    private Switch swEnable;
    private TextView tvEnable, tvRefresh, tvNoWiFi;
    private View vLayWiFi;
    //Wi-Fi
    private MyWiFiManager myWiFiManager;
    private NetworkItemAdapter adapter;
    private ArrayList<NetworkItem> items;
    //Other
    private String ssidFilter;
    private WiFiCallback wiFiCallback = null;
    private Context context;
    private Activity activity;
    public static final int PERMISSION_CODE = 1644;
    public static final String TAG = "WiFiConnectionDialog";

    public WiFiConnectionDialog(){
        super();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.dialog_wifi_connection, container, false);

        //Layout
        swEnable = v.findViewById(R.id.sw_enable);
        tvEnable = v.findViewById(R.id.tv_wifi_enable);
        vLayWiFi = v.findViewById(R.id.lay_wifi);
        RecyclerView rvNetworks = v.findViewById(R.id.rv_wifi);
        tvNoWiFi = v.findViewById(R.id.tv_no_wifi);
        tvRefresh = v.findViewById(R.id.tv_refresh);
        TextView tvDone = v.findViewById(R.id.tv_done);

        //Initial
        //Wi-Fi
        myWiFiManager = new MyWiFiManager(context, new MyWiFiManager.OnStatusChangedListener() {
            @Override
            public void onEnabling(boolean enabling) {
                Log.w(TAG, "Wi-Fi enabling..."+enabling);
                String t = getString(enabling? R.string.enabling:R.string.disabling)+"\u2026";
                tvEnable.setText(t);
            }

            @Override
            public void onEnabled(boolean enabled) {
                Log.w(TAG, "Wi-Fi enabled..."+enabled);
                swEnable.setChecked(enabled);
                tvEnable.setText(enabled? R.string.enabled:R.string.enable_wifi);
                //Show list
                showAvailableNetworks(enabled);
            }

            @Override
            public void onScanningStart() {
                tvRefresh.setEnabled(false);
                String t = getString(R.string.scanning)+"\u2026";
                tvRefresh.setText(t);
            }

            @Override
            public void onScanningEnd(List<NetworkItem> results, boolean updated) {
                //Enable
                tvRefresh.setText(R.string.scan);
                tvRefresh.setEnabled(true);
                //Update list
                Log.w(TAG, "AVAILABLE="+results.size());
                items.clear();
                items.addAll(results);
                adapter.notifyDataSetChanged();
                tvNoWiFi.setVisibility(results.size()>0? View.GONE:View.VISIBLE);
            }

            @Override
            public void onRequestAccessLocation() {
                if(wiFiCallback!=null){
                    wiFiCallback.onRequestPermission(MyWiFiManager.PERMISSION_LIST);
                }
            }

            @Override
            public void onRequestPassword(String ssid, boolean err) {
                if (err){ Function.showToast(context, "Wi-Fi password incorrect, please re-enter."); }
                showRequestPasswordDialog(ssid);
            }

            @Override
            public void onDisconnected() {
                //Clear status
                for(NetworkItem item : items){
                    if (item.status!=null){ item.status = null; }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onConnecting(String ssid, String status) {
                updateNetworkActive(ssid, status);
            }

            @Override
            public void onConnected(String ssid, int rssi) {
                //Update item
                updateNetworkConnected(ssid, rssi);
            }

            @Override
            public void onConnectFailed(String ssid, String err) {

            }
        });
        myWiFiManager.setSsidFilter(ssidFilter);

        //Wi-Fi Enable
        swEnable.setChecked(myWiFiManager.isWiFiEnabled());
        if (!myWiFiManager.isWiFiEnabled()){
            myWiFiManager.setWiFiEnable(true);
        }
        //Toggle Wi-Fi Enable
        swEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                myWiFiManager.setWiFiEnable(isChecked);
            }
        });

        //List
        items = new ArrayList<>();
        adapter = new NetworkItemAdapter(context, items);
        adapter.setOnClickItemListener(new NetworkItemAdapter.OnClickItemListener() {
            @Override
            public void onClick(int position, NetworkItem item) {
                Log.w(TAG, "Try to connect "+item.ssid+" ["+item.auth+"]");
                if (item.status!=null && item.status.equals(MyWiFiManager.WIFI_CONNECTED)){
                    Function.showToast(context, MyWiFiManager.WIFI_CONNECTED+" "+item.bssid);
                }else{
                    myWiFiManager.connect(item);
                }
            }
        });
        /*
        adapter.setOnHoldPressItemListener(new NetworkItemAdapter.OnHoldPressItemListener() {
            @Override
            public void onPress(int position, NetworkItem item) {
                Log.w(TAG, "Forget network..."+item.ssid);
                confirmToRemoveNetwork(item.ssid);
            }
        });
        */
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvNetworks.setLayoutManager(layoutManager);
        rvNetworks.setAdapter(adapter);
        showAvailableNetworks(swEnable.isChecked());

        //Scan Wi-Fi
        tvRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Scan
                myWiFiManager.scanAvailableNetwork();
            }
        });

        //Done
        tvDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Found connected item
                NetworkItem conItem = null;
                if (myWiFiManager.isWiFiEnabled()){
                    for (NetworkItem item : items){
                        if (item.status!=null && item.status.equals(MyWiFiManager.WIFI_CONNECTED)){
                            conItem = item; break;
                        }
                    }
                }
                //Callback
                if(wiFiCallback!=null){ wiFiCallback.onDone(conItem); }
                //Dismiss
                getDialog().dismiss();
            }
        });

        //Register
        myWiFiManager.registerBroadCasts();

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.activity = getActivity();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        myWiFiManager.stop();
    }

    private void showAvailableNetworks(boolean show){
        tvRefresh.setVisibility(show? View.VISIBLE:View.GONE);
        vLayWiFi.setVisibility(show? View.VISIBLE:View.GONE);
    }

    private void updateNetworkActive(String ssid, String status){
        if (ssid==null){ return; }
        NetworkItem mI;
        for(int i=0; i<items.size(); i++){
            if(ssid.equals(items.get(i).ssid)){
                //Remove
                mI = items.get(i);
                items.remove(i);
                //Insert
                mI.status = status;
                items.add(0, mI);
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }

    private void updateNetworkConnected(String ssid, int rssi){
        if (ssid==null){ return; }
        NetworkItem mI;
        for(int i=0; i<items.size(); i++){
            if(ssid.equals(items.get(i).ssid)){
                //Remove
                mI = items.get(i);
                items.remove(i);
                //Insert
                mI.status = MyWiFiManager.WIFI_CONNECTED;
                if (rssi<0){mI.rssi = rssi;}
                items.add(0, mI);
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }

    private void showRequestPasswordDialog(final String ssid){
        Function.showPasswordDialog(activity, "Wi-Fi password of \"" + ssid + "\"", R.string.connect,
                new MyAlertDialog.OnButtonClickListener() {
            @Override
            public void onClose() {

            }

            @Override
            public void onAction(String data, String password) {
                //Connect
                Log.w(TAG, "Connect with SSID="+ssid+", PASS="+password);
                myWiFiManager.connect(ssid, password);
                //Close
                Function.dismissDialogFragment(activity, MyAlertDialog.TAG);
            }
        });
    }

    /*
    private void confirmToRemoveNetwork(String ssid){
        Function.showAlertDialog(activity, R.string.forget_network,
                getString(R.string.forget_network_details) + " \"" + ssid + "\"",
                R.string.forget, new MyAlertDialog.OnButtonClickListener() {
                    @Override
                    public void onClose() {
                    }

                    @Override
                    public void onAction(View view) {
                        Function.dismissDialogFragment(activity, MyAlertDialog.TAG);
                        myWiFiManager.removeNetworkConnected();
                    }
                });
    }
    */

    public void onPermissionResult(boolean permitted){
        if (permitted){
            //Register again
            myWiFiManager.registerBroadCasts();
        }else {
            //Dismiss
            getDialog().dismiss();
        }
    }

    public void setSsidFilter(String ssidFilter) {
        this.ssidFilter = ssidFilter;
        if(myWiFiManager!=null){ myWiFiManager.setSsidFilter(ssidFilter); }
    }

    public void setWiFiCallback(WiFiCallback wiFiCallback) {
        this.wiFiCallback = wiFiCallback;
    }

    public interface WiFiCallback{
        void onRequestPermission(@NonNull String[] permissions);
        void onDone(NetworkItem item);
    }


}
