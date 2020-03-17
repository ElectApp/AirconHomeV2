package com.apyeng.airconhomev2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WiFiScanner {

    private android.os.Handler handler;
    private boolean registed;
    private Context context;
    private WifiManager wifiManager;
    private static final int DEFAULT_INTERVAL = 12000; //Should more than 10 sec due to loop scanning of device = 10 sec
    private int interval;

    private int limitToLast; //Working with positive value only
    private int levelFilter; //Working with negative value only
    private List<ScanResult> lastResults;
    //Each signal level is calculated from WifiManager.calculateSignalLevel() by numLevels = 6
    //Cal. Distance rang (Free space): http://wifinigel.blogspot.com/2014/05/wifi-free-space-loss-calculator.html
    //Ex. Router (Asus DSL-N12U) output power 17 dBm @Receiver -55 dBm => distance 39 m
    public static final int EXCELLENT_LEVEL = -55;
    public static final int GOOD_LEVEL = -64;
    public static final int MEDIUM_LEVEL = -73;
    public static final int FAIR_LEVEL = -82;
    public static final int POOR_LEVEL = -91;
    public static final int NO_FILTER = 0;
    //LimitToLast constant
    public static final int UNLIMITED = 0;

    private OnScanResultListener resultListener;
    private OnResultUpdateListener updateListener;

    private String TAG = "WiFiScanner";

    public WiFiScanner(Context context){
        Log.w(TAG, "Create WiFiScanner ... ");
        this.context = context;
        wifiManager = (WifiManager)context
                .getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lastResults = new ArrayList<>(); //Create object
    }


    public void addOnScanResultListener(OnScanResultListener resultListener){
        this.resultListener = resultListener;
    }

    public void addOnResultUpdateListener(
            OnResultUpdateListener updateListener, int limitToLast, int levelFilter){
        this.updateListener = updateListener;
        this.limitToLast = limitToLast; //0 = unlimited
        this.levelFilter = levelFilter; //0 = no filter
    }

    public void setLoop(boolean loop){
        if(loop) {
            Log.w(TAG, "Set loop running by default interval...");
            this.interval = DEFAULT_INTERVAL;
            handler = new android.os.Handler();
        }
    }

    public void setLoop(boolean loop, int interval){
        if(loop) {
            Log.w(TAG, "Set loop running by "+interval+" ms");
            this.interval = interval;
            handler = new android.os.Handler();
        }
    }

    public void startScanWifi(){
        Log.w(TAG, "Start scan wifi");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);
        boolean success = wifiManager.startScan();
        if (!success){
            scanFailure();
        }
    }


    private void scanFailure(){
        Log.e(TAG, "Scan failure ... ");
        //Toast.makeText(context, "Scan wifi failure", Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            registed = true; //Set flag
            List<ScanResult> results = wifiManager.getScanResults();
            if (results!=null){
                Collections.sort(results, new LevelComparator());
                for (ScanResult r : results){
                    Log.w(TAG, "Result: "+r.SSID+" "+r.BSSID+" "+r.level+" "+r.frequency);
                }
                //Set on result callback
                if (resultListener!=null){
                    resultListener.onResult(results);
                }
                //Set result update call back
                if (updateListener!=null){
                    //Limit number and level of result for checking
                    List<ScanResult> limitResult;
                    //Each case
                    if (limitToLast>0 & levelFilter<0){ //Set limitToLast and levelFilter
                        Log.w(TAG, "Checking is limited at "+limitToLast+" items, level "+levelFilter+" dBm.");
                        limitResult = levelFilter(limitNumberOfResult(results, limitToLast), levelFilter);
                    }else if (limitToLast>0 & levelFilter==0){ //Set only limitToLast
                        Log.w(TAG, "Checking is limited at "+limitToLast+" items.");
                        limitResult = limitNumberOfResult(results, limitToLast);
                    }else if (limitToLast==0 & levelFilter<0){ //Set only levelFilter
                        Log.w(TAG, "Checking is limited at level "+levelFilter+" dBm.");
                        limitResult = levelFilter(results, levelFilter);
                    }else { //Not match case
                        Log.w(TAG, "Checking is unlimited!");
                        limitResult = results;
                    }

                    //Find new mac in old mac
                    List<ScanResult> addResult = new ArrayList<>();
                    for (ScanResult n : limitResult){
                        boolean f = false;
                        for (ScanResult o : lastResults){
                            if (n.BSSID.equals(o.BSSID)){ f = true; break; }
                        }
                        if (!f){
                            Log.w(TAG, "Add "+ n.SSID+" "+n.BSSID+" "+n.level);
                            addResult.add(n);
                        }
                    }

                    //Find old mac in new mac
                    List<ScanResult> removeResult = new ArrayList<>();
                    for (ScanResult o : lastResults){
                        boolean f = false;
                        for (ScanResult n : limitResult){
                            if (n.BSSID.equals(o.BSSID)){ f = true; break; }
                        }
                        if (!f){
                            Log.w(TAG, "Remove "+ o.SSID+" "+o.BSSID+" "+o.level);
                            removeResult.add(o);
                        }
                    }
                    //Set call back
                    if (addResult.size()>0){ updateListener.onAdd(addResult, limitResult); }
                    if (removeResult.size()>0){ updateListener.onRemove(removeResult, limitResult); }
                    //Copy to last results
                    lastResults = limitResult;
                }

            }

            //Set repeating
            if (handler!=null){
                handler.removeCallbacks(loopRun); //Clear previously loop
                handler.postDelayed(loopRun, interval); //Set post delay
            }


        }
    };

    public void stopScanWifi(){

        if (handler!=null){
            Log.w(TAG, "Stop loop running ... ");
            handler.removeCallbacks(loopRun); //Stop repeating
            handler = null; //Clear handler
        }

        if (registed){
            Log.w(TAG, "Stop scan wifi ... ");
            context.unregisterReceiver(wifiScanReceiver);
            registed = false; //Clear flag
        }

        //Clear last result
        if (lastResults!=null){ lastResults.clear(); }


    }

    public void setWifiEnable(boolean enable){
        if (wifiManager!=null){
            if (enable){
                Log.w(TAG, "Enable WiFi ...");
                if (!wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(true);
                }
            }else {
                Log.w(TAG, "Disable WiFi ...");
                if (wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                }
            }
        }
    }

    private Runnable loopRun = new Runnable() {
        @Override
        public void run() {
            //Scan again
            startScanWifi();
        }
    };

    private List<ScanResult> limitNumberOfResult(List<ScanResult> input, int limit){
        if (input.size()>=limit){
            List<ScanResult> r = new ArrayList<>();
            for (int i=0; i<limit; i++){
                r.add(input.get(i));
            }
            return r;
        }
        return input;
    }
    private List<ScanResult> levelFilter(List<ScanResult> input, int levelFilter){
        List<ScanResult> output = new ArrayList<>();
        for (ScanResult i : input){
            if (i.level>levelFilter){
                output.add(i);
            }
        }
        return output;
    }

    private class LevelComparator implements Comparator<ScanResult> {

        @Override
        public int compare(ScanResult o1, ScanResult o2) {
            int lv1 = o1.level; int lv2 = o2.level;
            return lv2-lv1; //Descending [Max to min]
            //return lv2-lv1; //Ascending [Min to max]
        }
    }


    interface OnScanResultListener{
        void onResult(List<ScanResult> results);
    }

    interface OnResultUpdateListener{
        void onAdd(List<ScanResult> add, List<ScanResult> current);
        void onRemove(List<ScanResult> remove, List<ScanResult> current);
    }


}
