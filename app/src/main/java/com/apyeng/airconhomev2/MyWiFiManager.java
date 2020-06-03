package com.apyeng.airconhomev2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyWiFiManager {

    private Context context;
    private AnyBroadcastReceiver broadcastReceiver;
    private WifiManager wifiManager;
    private ConnectivityManager conManager;
    private OnStatusChangedListener onStatusChangedListener;
    //Permission
    public static final String []PERMISSION_LIST = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    //Auth Type
    public static final int AUTH_NONE = 0;
    public static final int AUTH_WPA = 1;
    public static final int AUTH_WPA2 = 2;
    //Status
    public static final String WIFI_CONNECTING = "Connecting...";
    public static final String WIFI_AUTHENTICATING = "Authenticating...";
    public static final String WIFI_INCORRECT_PASSWORD = "Incorrect password";
    public static final String WIFI_OBTAIN_IP = "Obtaining IP address...";
    public static final String WIFI_CONNECTED = "Connected";
    public static final String WIFI_CONNECT_FAILED = "Connect failed";
    public static final String WIFI_DISCONNECTING = "Disconnecting";
    public static final String WIFI_DISCONNECTED = "Disconnected";
    //Connect
    private String pendingSSID = null;
    private int pendingNetID = -1;
    //Interval scanning
    private boolean enableIntervalScan = false;
    private Handler loopScanHandler = null;
    private static final int INTERVAL_SCAN = 10000; //ms
    //Other
    private String ssidFilter = null;
    private static final String TAG = "MyWiFiManager";

    public MyWiFiManager(Context context, @NonNull OnStatusChangedListener _onStatusChangedListener){
        this.context = context;
        onStatusChangedListener = _onStatusChangedListener;

        //Broadcasts
        broadcastReceiver = new AnyBroadcastReceiver();
        broadcastReceiver.setOnCallback(new AnyBroadcastReceiver.OnCallback() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (context==null || intent==null){ return; }
                String action = intent.getAction();
                String extrasStr = getExtrasString(intent);
                if (action==null || extrasStr==null){ return; }
                Log.w(TAG, action);

                switch (action){
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        //Wi-Fi Enable/Disable
                        if (intent.hasExtra(WifiManager.EXTRA_WIFI_STATE)){
                            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                            Log.w(TAG, "WiFI-STATE="+state);
                            switch (state){
                                case 0:
                                    onStatusChangedListener.onEnabling(false); break;
                                case 1:
                                    onStatusChangedListener.onEnabled(false); break;
                                case 2:
                                    onStatusChangedListener.onEnabling(true); break;
                                case 3:
                                    onStatusChangedListener.onEnabled(true); break;
                            }
                        }
                        break;

                    case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                        if(intent.hasExtra(WifiManager.EXTRA_RESULTS_UPDATED)){
                            boolean updated = intent.getBooleanExtra(
                                    WifiManager.EXTRA_RESULTS_UPDATED, false);
                            Log.d(TAG, "LIST-UPDATED="+updated);
                            //Callback
                            List<ScanResult> results = wifiManager.getScanResults();
                            if(results!=null){
                                List<NetworkItem> netItems = new ArrayList<>();
                                NetworkItem item;
                                String s = getSSIDConnected();
                                for(ScanResult r : results){
                                    Log.w(TAG, r.toString());
                                    if (ssidFilter==null || r.SSID.startsWith(ssidFilter)){
                                        item = new NetworkItem(r.SSID, r.BSSID,
                                                r.capabilities.contains("WPA")? AUTH_WPA:AUTH_NONE, r.level);
                                        if (s!=null && s.equals(r.SSID)){
                                            item.status = WIFI_CONNECTED;
                                            netItems.add(0, item);
                                        }else{
                                            netItems.add(item);
                                        }
                                    }
                                }
                                onStatusChangedListener.onScanningEnd(netItems, updated);
                            }
                            //Scan again at interval time
                            if(loopScanHandler!=null && enableIntervalScan){
                                loopScanHandler.removeCallbacks(loopScanRunner);
                                loopScanHandler.postDelayed(loopScanRunner, INTERVAL_SCAN);
                            }
                        }
                        break;

                    case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                        if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)){
                            int code = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
                            Log.e(TAG, "CONNECT-ERROR="+code);
                            //Callback
                            if(pendingSSID!=null && code==1){
                                onStatusChangedListener.onRequestPassword(pendingSSID, true);
                                cancelConnecting();
                            }else if(code>1){
                                onStatusChangedListener.onConnectFailed(getSSIDConnected(), "Connect failed: "+code);
                                cancelConnecting();
                            }

                        }
                        break;

                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        if(intent.hasExtra(WifiManager.EXTRA_NETWORK_INFO)){
                            NetworkInfo info = (NetworkInfo)intent.getExtras()
                                    .get(WifiManager.EXTRA_NETWORK_INFO);
                            if(info!=null){
                                NetworkInfo.DetailedState state = info.getDetailedState();
                                Log.w(TAG, "NETWORK-STATE="+state);
                                switch (state){
                                    case DISCONNECTED:
                                        //Callback
                                        onStatusChangedListener.onDisconnected();
                                        //Reconnect
                                        doConnecting();
                                        break;
                                    case CONNECTING:
                                        onStatusChangedListener.onConnecting(pendingSSID!=null?
                                                pendingSSID : getSSIDConnected(), WIFI_CONNECTING);
                                        break;
                                    case AUTHENTICATING:
                                        onStatusChangedListener.onConnecting(pendingSSID!=null?
                                                pendingSSID : getSSIDConnected(), WIFI_AUTHENTICATING);
                                        break;
                                    case OBTAINING_IPADDR:
                                        onStatusChangedListener.onConnecting(pendingSSID!=null?
                                                pendingSSID : getSSIDConnected(), WIFI_OBTAIN_IP);
                                        break;
                                    case CONNECTED:
                                        onStatusChangedListener.onConnected(pendingSSID!=null?
                                                pendingSSID : getSSIDConnected(), getRSSIConnected());
                                        break;
                                }
                            }
                        }
                        break;

                    case WifiManager.RSSI_CHANGED_ACTION:
                        if (intent.hasExtra(WifiManager.EXTRA_NEW_RSSI)){
                            int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                            Log.w(TAG, "RSSI-UPDATED="+rssi);
                            onStatusChangedListener.onConnected(getSSIDConnected(), rssi);
                        }
                        break;
                }

            }
        });

        //Set Wi-Fi manager
        wifiManager = (WifiManager)context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        //Connection status
        conManager = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

    }

    public boolean isWiFiEnabled(){
        return wifiManager.isWifiEnabled();
    }

    public void setWiFiEnable(boolean enable){
        wifiManager.setWifiEnabled(enable);
    }

    public void registerBroadCasts(){
        //Check permission
        if(!hasPermission()){
            onStatusChangedListener.onRequestAccessLocation();
            return;
        }
        //Register BroadCasts
        IntentFilter intentFilter = new IntentFilter();
        //Wi-Fi Scanner
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //Wi-Fi Status
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //Register
        context.registerReceiver(broadcastReceiver, intentFilter);
        //Start scanner
        loopScanHandler = new Handler();
        scanAvailableNetwork();
    }


    public void scanAvailableNetwork(){
        //Start
        wifiManager.startScan();
        onStatusChangedListener.onScanningStart();
    }

    private Runnable loopScanRunner = new Runnable() {
        @Override
        public void run() {
            //Scan again
            scanAvailableNetwork();
        }
    };


    public void connect(@NonNull NetworkItem netItem){
        //Ok?
        if (netItem.ssid==null){ return; }
        Log.w(TAG, "Click to connect..."+netItem.ssid);
        //Save last
        pendingSSID = netItem.ssid;
        //Saved?
        int netId = -1;
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            //=================== Below Android 10 ===========//
            String ssid = addQuotationMark(netItem.ssid);
            for(WifiConfiguration con : wifiManager.getConfiguredNetworks()){
                //Log.d(TAG, "Saved: SSID="+con.SSID+", ID="+con.networkId);
                if(ssid.equals(con.SSID)){
                    Log.w(TAG, "Saved: SSID="+con.SSID+", ID="+con.networkId);
                    netId = con.networkId;
                    break;
                }
            }
        }
        //Action
        if(netId>-1){
            //Try connect
            connect(netId);
        }else{
            //Request password?
            if(netItem.auth>0){
                netItem.netID = 0;
                onStatusChangedListener.onRequestPassword(netItem.ssid, false);
            }else {
                //Try connect
                connect(netItem.ssid);
            }
        }
    }

    public void connect(String ssid, String password) {
        Log.w(TAG, "Try connect SSID="+ssid+", PASS="+password);
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            //================ below Android 10 ====================//
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotationMark(ssid);
            config.preSharedKey = addQuotationMark(password);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.status = WifiConfiguration.Status.ENABLED;
            connect(config);
        }else{
            //================ Android 10 up ====================//
            connectByAndroidQ(ssid, password, true);
        }
    }

    public void connect(String ssid) {
        Log.w(TAG, "Try connect SSID="+ssid);
        //Update
        pendingSSID = ssid;
        //Config
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){
            //================ below Android 10 ====================//
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotationMark(ssid);
            config.preSharedKey = null;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.status = WifiConfiguration.Status.ENABLED;
            connect(config);
        }else {
            //================ Android 10 up ====================//
            connectByAndroidQ(ssid, null, true);
        }
    }

    private void connect(WifiConfiguration config){
        int netId = wifiManager.addNetwork(config);
        connect(netId);
    }


    @RequiresApi(api = 29)
    public void connectByAndroidQ(String ssid, String password, boolean internet){
        if(internet){
            connectWithInternet(password==null?
                    getWifiNetworkSpecifier(ssid):getWifiNetworkSpecifier(ssid, password));
        }else{
            connectWithoutInternet(password==null?
                    getWifiNetworkSpecifier(ssid):getWifiNetworkSpecifier(ssid, password));
        }
    }

    @RequiresApi(api = 29)
    private void connectWithoutInternet(WifiNetworkSpecifier wifiSpec){
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(wifiSpec)
                .build();
        conManager.requestNetwork(req, netCallback);
    }

    @RequiresApi(api = 29)
    private void connectWithInternet(WifiNetworkSpecifier wifiSpec){
        NetworkRequest req = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiSpec)
                .build();
        conManager.requestNetwork(req, netCallback);
    }

    @RequiresApi(api = 29)
    private WifiNetworkSpecifier getWifiNetworkSpecifier(String ssid){
        return new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .build();
    }

    @RequiresApi(api = 29)
    private WifiNetworkSpecifier getWifiNetworkSpecifier(String ssid, String password){
        return new WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build();
    }

    private ConnectivityManager.NetworkCallback netCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.w(TAG, "onAvailable..."+network.toString());
            //testInternetAvailable();
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            Log.w(TAG, "onLosing...");
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.w(TAG, "onLost...");
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.w(TAG, "onUnavailable...");
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.w(TAG, "onCapabilitiesChanged...");
        }

        @Override
        public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            Log.w(TAG, "onLinkPropertiesChanged...");
        }

        @Override
        public void onBlockedStatusChanged(Network network, boolean blocked) {
            super.onBlockedStatusChanged(network, blocked);
            Log.w(TAG, "onBlockedStatusChanged..."+blocked);
        }
    };

    private void connect(int netID){
        //Update
        pendingNetID = netID;
        //Action
        if (getSSIDConnected()!=null){
            int cId = wifiManager.getConnectionInfo().getNetworkId();
            Log.w(TAG, "DisableID="+cId);
            wifiManager.disableNetwork(cId);    //Blocking reconnect
            wifiManager.disconnect();           //Disconnect
        }else{
            doConnecting();
        }
        Log.w(TAG, "Try connect ID="+netID);
    }

    public void cancelConnecting(){
        if(pendingNetID>0){
            Log.w(TAG, "Cancel connecting...SSID="+pendingSSID+", ID="+pendingNetID);
            wifiManager.removeNetwork(pendingNetID);
            pendingNetID = 0;
        }
    }

    //Can't get success due to E/WifiConfigManager: UID 11222 does not have permission to delete configuration
    public void removeNetworkConnected(){
        int id = getNetworkIdConnected();
        if (id>0){
            Log.w(TAG, "Remove network...ID="+id+" "+wifiManager.removeNetwork(id));
        }
    }

    private void doConnecting(){
        if (pendingSSID==null || pendingNetID<1){ return; }
        //Callback
        //onStatusChangedListener.onConnecting(pendingSSID, WIFI_CONNECTING);
        //Connect
        wifiManager.enableNetwork(pendingNetID, true);
        wifiManager.reconnect();
    }
    

    public String getSSIDConnected(){
        //This method is sensitive than the below method.
        String ssid = wifiManager.getConnectionInfo().getSSID();
        if (ssid!=null){
            ssid = ssid.replace("\"", "");
            if (ssid.isEmpty() || ssid.equals("<unknown ssid>")){
                ssid = null;
            }
        }

        Log.w(TAG, "SSID Connected="+ssid);
        return ssid;
    }

    public int getRSSIConnected(){
        return wifiManager.getConnectionInfo().getRssi();
    }

    public int getNetworkIdConnected(){
        return wifiManager.getConnectionInfo().getNetworkId();
    }

    public boolean isEnableIntervalScan() {
        return enableIntervalScan;
    }

    public void setEnableIntervalScan(boolean enableIntervalScan) {
        this.enableIntervalScan = enableIntervalScan;
    }

    public void stop(){
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            // TODO Skipping Exception which might be raising when the receiver is not yet
            // registered
            Log.e(TAG,
                    "Skipping Exception which might be raising when the receiver is not yet registered");
        }
        if (loopScanHandler!=null){
            loopScanHandler.removeCallbacks(loopScanRunner);
            loopScanHandler = null;
        }
    }

    private String getExtrasString(Intent pIntent) {
        String extrasString = "";
        Bundle extras = pIntent.getExtras();
        try {
            if (extras != null) {
                Set<String> keySet = extras.keySet();
                for (String key : keySet) {
                    try {
                        String extraValue = pIntent.getExtras().get(key).toString();
                        extrasString += key + ": " + extraValue + "\n";
                    } catch (Exception e) {
                        Log.e(TAG, "Exception 2 in getExtrasString(): " + e.toString());
                        extrasString += key + ": Exception:" + e.getMessage() + "\n";
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in getExtrasString(): " + e.toString());
            extrasString += "Exception:" + e.getMessage() + "\n";
        }
        Log.d(TAG, "extras=" + extrasString);
        return extrasString;
    }

    private boolean hasPermission(){
        for (String permission : PERMISSION_LIST) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String addQuotationMark(@NonNull String v){
        return "\""+v.trim()+"\"";
    }

    public void setSsidFilter(String ssidFilter) {
        this.ssidFilter = ssidFilter;
    }

    public interface OnStatusChangedListener{
        void onEnabling(boolean enabling);
        void onEnabled(boolean enabled);
        void onScanningStart();
        void onScanningEnd(List<NetworkItem> results, boolean updated);
        void onRequestAccessLocation();
        void onRequestPassword(String ssid, boolean err);
        void onDisconnected();
        void onConnecting(String ssid, String status);
        void onConnected(String ssid, int rssi);
        void onConnectFailed(String ssid, String err);
    }

}
