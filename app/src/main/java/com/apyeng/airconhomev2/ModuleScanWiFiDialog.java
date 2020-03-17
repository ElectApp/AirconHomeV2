package com.apyeng.airconhomev2;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


public class ModuleScanWiFiDialog extends DialogFragment{

    private TextView tvTitle, tvNoWiFi;
    private RoundButtonWidget rbRefresh;
    private ClearableEditText eSSID;
    private FrameLayout flMain;
    private LinearLayout flCustom;
    private ArrayList<NetworkItem> items;
    private NetworkItemAdapter adapter;
    private NetworkItemAdapter.OnClickItemListener onClickItemListener;
    private Context context;
    private Handler handler;
    public static final String TAG = "ModuleScan";
    private static final long INTERVAL_SCAN = 10000;
    //Request SCAN Key
    private static final String SCAN_WIFI[] = { "13" }; //Key, limit -> It was sorted from the discovery sequence of WiFi module

    public ModuleScanWiFiDialog() {
        // Required empty public constructor
    }

    public static void show(FragmentActivity activity, NetworkItemAdapter.OnClickItemListener onClickItemListener){
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        ModuleScanWiFiDialog dialog = new ModuleScanWiFiDialog();
        dialog.show(transaction, TAG);
        dialog.onClickItemListener = onClickItemListener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.dialog_module_scan_wi_fi, container, false);

        tvTitle = v.findViewById(R.id.tv_title);
        RecyclerView rvNetwork = v.findViewById(R.id.rv_network_items);
        rbRefresh = v.findViewById(R.id.rb_refresh);
        tvNoWiFi = v.findViewById(R.id.tv_no_found);
        TextView tvCustom = v.findViewById(R.id.tv_custom);
        flMain = v.findViewById(R.id.lay_main);
        flCustom = v.findViewById(R.id.lay_custom);
        eSSID = v.findViewById(R.id.ssid_enter);

        //Refresh
        rbRefresh.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Re-scan
                startScanner(false);
            }
        });

        items = new ArrayList<>();
        /*
        int rssi[] = { -85, -80, -75, -70, -65, -60, -55, -50, -45 };
        for (int i=0; i<rssi.length; i++){
            items.add(new NetworkItem("SSID"+i, "RSSID"+i, rssi[i]));
        }*/
        adapter = new NetworkItemAdapter(context, items);
        adapter.setOnClickItemListener(new NetworkItemAdapter.OnClickItemListener() {
            @Override
            public void onClick(int position, NetworkItem item) {
                //Callback
                if (onClickItemListener!=null){
                    onClickItemListener.onClick(position, item);
                }
                //Close
                dismiss();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        rvNetwork.setLayoutManager(layoutManager);
        rvNetwork.setAdapter(adapter);

        tvCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomToolbar(true);
            }
        });

        v.findViewById(R.id.tv_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ssid = String.valueOf(eSSID.getText());
                if (onClickItemListener!=null && !ssid.isEmpty()){
                    onClickItemListener.onClick(0, new NetworkItem(ssid, "", -25));
                    dismiss();
                }
            }
        });

        v.findViewById(R.id.i_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomToolbar(false);
            }
        });

        showCustomToolbar(false);


        //Interval scanning
        handler = new Handler();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set animation here!
        //Thank: https://stackoverflow.com/questions/41869348/enter-and-exit-animations-not-working-in-dialog-fragment
        Dialog dialog = getDialog();
        if(dialog!=null){
            Window window = dialog.getWindow();
            if (window!=null){
                window.getAttributes().windowAnimations = R.style.UpDownAnimation;
            }
        }

    }

    @Override
    public void onResume() {
        // Store access variables for window and blank point
        Window window = getDialog().getWindow();
        Point size = new Point();
        if (window!=null){
            // Store dimensions of the screen in `size`
            Display display = window.getWindowManager().getDefaultDisplay();
            display.getSize(size);
            // Set the width of the dialog proportional to 95% of the screen width
            // Thank: https://guides.codepath.com/android/using-dialogfragment
            window.setLayout((int) (size.x * 0.90), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            //Set color TRANSPARENT to outside round layout of dialog
            //Thank: https://www.codingdemos.com/android-custom-dialog-animation/
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        super.onResume();

    }


    @Override
    public void onStart() {
        super.onStart();
        //Start scanner
        startScanner(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        //Stop scanner
        if (handler!=null){
            handler.removeCallbacks(runnable);
        }
    }

    private void setScanning(boolean scanning){
        if (scanning){
            tvTitle.setText(R.string.scanning_wifi);
            rbRefresh.setVisibility(View.GONE);
        }else {
            tvTitle.setText(R.string.select_wifi);
            rbRefresh.setVisibility(View.VISIBLE);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //Re-scan
            startScanner(true);
        }
    };

    private void startScanner(final boolean fromInterval){
        //Action
        if (!fromInterval){ setScanning(true); }
        new TCPMessage().run(SCAN_WIFI, false, new TCPMessage.OnTCPListener() {
            @Override
            public void onResult(Task task) {
                //Close scanning
                setScanning(false);
                //Set result
                Log.w(TAG, "Result: "+ Arrays.toString(task.response));
                items.clear();
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<NetworkItem>>(){}.getType();
                try {
                    //Set buffer
                    ArrayList<NetworkItem> buffer = gson.fromJson(task.response[0], type);
                    //Sort
                    Collections.sort(buffer, new RSSIComparator());
                    //Add to found list
                    items.addAll(buffer);
                }catch (Exception er){
                    er.printStackTrace();
                }
                //Update
                adapter.notifyDataSetChanged();
                //Set no found
                tvNoWiFi.setVisibility(items.size()>0? View.GONE : View.VISIBLE);
                //Interval Scanning
                if (handler!=null){ handler.postDelayed(runnable, INTERVAL_SCAN); }
            }
        });

    }

    private class RSSIComparator implements Comparator<NetworkItem>{
        @Override
        public int compare(NetworkItem t0, NetworkItem t1) {
            return t1.rssi - t0.rssi; //Max to min
        }
    }

    private void showCustomToolbar(boolean show){
        //Toggle toolbar
        if (show){
            flCustom.setVisibility(View.VISIBLE);
            flMain.setVisibility(View.GONE);
        }else {
            flCustom.setVisibility(View.GONE);
            flMain.setVisibility(View.VISIBLE);
        }
    }

}
