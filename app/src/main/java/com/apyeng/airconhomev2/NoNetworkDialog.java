package com.apyeng.airconhomev2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

public class NoNetworkDialog extends DialogFragment {

    private Context context;
    private Activity activity;
    private boolean isNoInternet; //False = No AC connected, True = No Internet
    public static final String TAG = "NoNetworkDialog"; //Unique TAG


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        //Get data
        Bundle bundle = getArguments();
        isNoInternet = bundle.getBoolean(Constant.NO_NETWORK, true);

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.dialog_no_network, container, false);

        TextView title = view.findViewById(R.id.no_network_title);
        TextView detail = view.findViewById(R.id.no_network_detail);
        RoundButtonWidget action = view.findViewById(R.id.action_btn);
        RoundButtonWidget conBtn = view.findViewById(R.id.connect_btn);

        if (!isNoInternet){
            title.setText(R.string.no_ac_connected);
            detail.setText(R.string.connect_wifi_note);
            action.setText(R.string.connect_wifi);
        }else {
            title.setText(R.string.no_internet);
            detail.setText(R.string.no_internet_detail);
            action.setText(R.string.retry);
        }

        //Connect
        conBtn.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q){ //Settings.ACTION_WIFI_SETTINGS
                    //My Wi-Fi connectivity
                    WiFiConnectionDialog dialog = new WiFiConnectionDialog();
                    if (!isNoInternet){ dialog.setSsidFilter("AC"); }
                    dialog.setWiFiCallback(new WiFiConnectionDialog.WiFiCallback() {
                        @Override
                        public void onRequestPermission(@NonNull String[] permissions) {
                            ActivityCompat.requestPermissions(activity,
                                    permissions, WiFiConnectionDialog.PERMISSION_CODE);
                        }

                        @Override
                        public void onDone(NetworkItem item) {
                            if (item!=null){
                                Log.w(TAG, "Reload with..."+item.ssid);
                                //Recheck internet
                                if (Function.internetConnected(getDialog().getContext())){
                                    dismiss();
                                }
                            }
                        }
                    });
                    //Show DialogFragment inside DialogFragment
                    //Thank: https://stackoverflow.com/a/40342348
                    FragmentManager manager = ((FragmentActivity)context).getSupportFragmentManager();
                    dialog.show(manager, WiFiConnectionDialog.TAG);
                }else{
                    //Setting panel for Android 10
                    startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), 1335);
                }
            }
        });

        action.setOnWidgetClickListener(new RoundButtonWidget.OnWidgetClickListener() {
            @Override
            public void onClick(View view) {
                //Check action
                if (isNoInternet){
                    //Recheck internet
                    if (Function.internetConnected(getDialog().getContext())){
                        dismiss();
                    }
                }else {
                    //Connect AC
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
                }
            }
        });

        return view;

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
    public void onStart() {
        super.onStart();

        //Set size dialog
        Dialog dialog = getDialog();
        if(dialog!=null){
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window!=null){
                window.setLayout(width, height);
            }
            //Block cancel from Back button on navigator bar
            //dialog.setCancelable(false);
        }

    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        //Callback listener when dialog dismiss
        //Thank: https://stackoverflow.com/questions/23786033/dialogfragment-and-ondismiss

        final Activity activity = getActivity();
        if (activity instanceof DialogInterface.OnDismissListener){
            ((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
        }

    }



}
